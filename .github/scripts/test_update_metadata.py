"""Offline release-metadata boundary tests; never call GitHub."""
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / ".github/scripts/update_metadata.py"


class UpdateMetadataTest(unittest.TestCase):
    def generate(self, tag="v0.1.16", body="RTBus 更新说明", code=24, name="0.1.16"):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "androidApp").mkdir()
            (root / "androidApp/build.gradle.kts").write_text(
                f'versionCode = {code}\nversionName = "{name}"\n', encoding="utf-8")
            env = os.environ.copy()
            env.update(TAG_NAME=tag, RELEASE_BODY=body, OUTPUT_DIR=str(root / "metadata"),
                       GITHUB_REPOSITORY="someone/unrelated-source", PYTHONIOENCODING="utf-8")
            result = subprocess.run([sys.executable, str(SCRIPT)], cwd=root, env=env,
                                    capture_output=True, text=True, encoding="utf-8")
            files = {p.name: json.loads(p.read_text(encoding="utf-8"))
                     for p in (root / "metadata").glob("*.json")}
            return result, files

    def test_stable_metadata_points_only_to_rtbus_download_repository(self):
        result, files = self.generate()
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual({"update.json", "version.json"}, set(files))
        update = files["update.json"]
        self.assertEqual(24, update["versionCode"])
        self.assertEqual("0.1.16", update["versionName"])
        base = "https://github.com/worlt/rtbus-releases/releases/download/v0.1.16/"
        self.assertEqual(base + "RTBus-0.1.16-arm64-v8a.apk", update["downloadUrl"])
        self.assertEqual({abi: base + f"RTBus-0.1.16-{abi}.apk"
                          for abi in ["arm64-v8a", "armeabi-v7a"]}, update["downloadUrls"])
        self.assertFalse(update["forceUpdate"])
        self.assertEqual(files["update.json"], files["version.json"])

    def test_beta_metadata_does_not_overwrite_stable_channel(self):
        result, files = self.generate(tag="v0.1.17-beta.1", code=25, name="0.1.17-beta.1")
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual({"update-beta.json"}, set(files))

    def test_missing_tag_is_rejected_without_metadata(self):
        result, files = self.generate(tag="")
        self.assertNotEqual(0, result.returncode)
        self.assertEqual({}, files)

    def test_mismatched_tag_is_rejected_without_metadata(self):
        result, files = self.generate(tag="v9.9.9")
        self.assertNotEqual(0, result.returncode)
        self.assertEqual({}, files)

    def test_missing_public_notes_do_not_leak_private_git_messages(self):
        result, files = self.generate(body="")
        self.assertNotEqual(0, result.returncode)
        self.assertEqual({}, files)


if __name__ == "__main__":
    unittest.main(verbosity=2)
