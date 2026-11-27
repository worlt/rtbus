import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

SCRIPT = Path(__file__).resolve().parents[2] / "tools/release.py"
spec = importlib.util.spec_from_file_location("rtbus_release", SCRIPT)
release = importlib.util.module_from_spec(spec)
spec.loader.exec_module(release)


class ReleaseToolingTest(unittest.TestCase):
    def test_release_preparation_uses_the_main_rtbus_repository(self):
        self.assertEqual("worlt/rtbus", release.REPOSITORY)

    def test_rejects_equal_or_older_than_either_channel(self):
        for code in (23, 24, 25):
            with self.subTest(code=code), self.assertRaises(ValueError):
                release.require_new_version(code, [24, 25], rehearsal=False)
        release.require_new_version(26, [24, 25], rehearsal=False)

    def test_rehearsal_allows_current_but_not_downgrade(self):
        release.require_new_version(24, [24, 24], rehearsal=True)
        with self.assertRaises(ValueError):
            release.require_new_version(23, [24], rehearsal=True)

    def metadata(self):
        return {"applicationId": release.APPLICATION_ID, "variantName": "validation", "elements": [
            {"versionCode": 24, "versionName": "0.1.16", "filters": [{"filterType": "ABI", "value": a}], "outputFile": a + ".apk"}
            for a in release.ABIS]}

    def test_correct_build_metadata_is_accepted(self):
        self.assertEqual(set(release.ABIS), set(release.check_build_metadata(self.metadata(), 24, "0.1.16")))

    def test_mixed_version_wrong_package_and_duplicate_abi_are_rejected(self):
        for mutation in ("version", "package", "duplicate", "traversal", "absolute", "variant", "filters"):
            m = self.metadata()
            if mutation == "version": m["elements"][0]["versionCode"] = 23
            if mutation == "package": m["applicationId"] = "com.other.app"
            if mutation == "duplicate": m["elements"][0]["filters"] = m["elements"][1]["filters"]
            if mutation == "traversal": m["elements"][0]["outputFile"] = "../secret.apk"
            if mutation == "absolute": m["elements"][0]["outputFile"] = "C:\\private.apk"
            if mutation == "variant": m["variantName"] = "debug"
            if mutation == "filters": m["elements"][0]["filters"].append({"filterType": "DENSITY", "value": "hdpi"})
            with self.subTest(mutation=mutation), self.assertRaises(ValueError):
                release.check_build_metadata(m, 24, "0.1.16")

    def test_signer_must_be_exactly_the_pinned_certificate(self):
        good = "Signer #1 certificate SHA-256 digest: " + release.CERTIFICATE_SHA256
        self.assertEqual(release.CERTIFICATE_SHA256, release.check_signature(good))
        for bad in ("", good.replace(release.CERTIFICATE_SHA256, "0" * 64), good + "\nSigner #2 certificate SHA-256 digest: " + "1" * 64):
            with self.assertRaises(ValueError): release.check_signature(bad)

    def test_backup_cannot_be_in_any_git_worktree(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory); (root / ".git").write_text("gitdir: elsewhere")
            with self.assertRaises(ValueError): release.require_private_location(root / "backup")

    def test_windows_acl_helper_does_not_inherit_powershell7_module_path(self):
        from unittest.mock import patch
        import subprocess
        with patch.dict(release.os.environ, {"PSModulePath": "PowerShell7-only"}), patch.object(release.os, "name", "nt"):
            with patch.object(release.subprocess, "run", return_value=subprocess.CompletedProcess([], 0, b"", b"")) as command:
                release.protect(Path("backup"))
                self.assertNotIn("PSModulePath", command.call_args.kwargs["env"])

    def test_backup_rejects_home_root_and_unrelated_nonempty_folder(self):
        with self.assertRaises(ValueError): release.require_private_location(Path.home())
        with self.assertRaises(ValueError): release.require_private_location(Path.home().anchor)
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory); (root / "unrelated.txt").write_text("preserve")
            with self.assertRaises(ValueError): release.require_private_location(root)
            self.assertEqual("preserve", (root / "unrelated.txt").read_text())

    def test_rehearsal_cannot_claim_publication_readiness(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "report.json").write_text(json.dumps({
                "certificate_sha256": release.CERTIFICATE_SHA256, "application_id": release.APPLICATION_ID,
                "version_code": 24, "version_name": "0.1.16", "rehearsal": True,
                "ready_for_publication": True, "source_dirty": False, "published_version_codes": [24, 24],
                "assets": {}}))
            with self.assertRaisesRegex(ValueError, "rehearsal|readiness"):
                release.verify(root)

    def test_missing_backup_is_not_silently_created(self):
        with tempfile.TemporaryDirectory() as directory:
            with self.assertRaises(ValueError): release.check_backup(Path(directory), "a" * 64)
            self.assertEqual([], list(Path(directory).iterdir()))

    def test_failed_command_aborts_and_leaves_log(self):
        import sys
        with tempfile.TemporaryDirectory() as directory:
            log = Path(directory) / "failure.log"
            with self.assertRaises(RuntimeError): release.run_logged([sys.executable, "-c", "raise SystemExit(9)"], log)
            self.assertTrue(log.exists())

    def test_missing_or_tampered_candidate_assets_are_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory); assets = root / "assets"; assets.mkdir()
            (assets / "x.apk").write_bytes(b"original")
            manifest = {"x.apk": release.sha256(assets / "x.apk")}
            release.check_asset_hashes(assets, manifest)
            (assets / "x.apk").write_bytes(b"tampered")
            with self.assertRaises(ValueError): release.check_asset_hashes(assets, manifest)
            (assets / "x.apk").unlink()
            with self.assertRaises(ValueError): release.check_asset_hashes(assets, manifest)

    def test_extra_file_is_not_treated_as_a_public_asset(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory); (root / "unexpected.keystore").write_bytes(b"not-a-real-key")
            with self.assertRaises(ValueError): release.check_asset_hashes(root, {})

    def test_notes_must_be_explicit_and_nonempty(self):
        with tempfile.TemporaryDirectory() as directory:
            notes = Path(directory) / "notes.md"; notes.write_text("  ")
            with self.assertRaises(ValueError): release.read_notes(notes)
            notes.write_text("RTBus public changes")
            self.assertEqual("RTBus public changes", release.read_notes(notes))

    def test_versions_are_read_from_actual_gradle_source(self):
        with tempfile.TemporaryDirectory() as directory:
            p = Path(directory) / "build.gradle.kts"
            p.write_text('versionCode = 24\nversionName = "0.1.16"\n')
            self.assertEqual((24, "0.1.16"), release.read_version(p))
            p.write_text('versionCode = 0\nversionName = "bad/../path"')
            with self.assertRaises(ValueError): release.read_version(p)


if __name__ == "__main__": unittest.main()
