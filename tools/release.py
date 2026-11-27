#!/usr/bin/env python3
"""Prepare RTBus validation releases locally. Never uploads, installs or changes channels."""
import argparse
import datetime
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import re
import shutil
import struct
import subprocess
import sys
import urllib.request
import uuid
import xml.etree.ElementTree as ET
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[1]
APPLICATION_ID = "com.github.u5151561.trackr.debug"
CERTIFICATE_SHA256 = "51efae50fd08caf43eca935ebb2110605008ba604d008775acebda42b3ffe9c6"
ABIS = ("arm64-v8a", "armeabi-v7a")
REPOSITORY = "worlt/rtbus"
BACKUP_DIR = Path.home() / ".rtbus/signing-backups" / ("validation-" + CERTIFICATE_SHA256[:16])
VERSION_RE = r"[0-9]+\.[0-9]+\.[0-9]+(?:-[0-9A-Za-z.]+)?"


def sha256(path):
    with Path(path).open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def write_json(path, value):
    Path(path).write_bytes((json.dumps(value, ensure_ascii=False, indent=2) + "\n").encode("utf-8"))


def read_version(path):
    text = Path(path).read_text(encoding="utf-8")
    code = re.search(r"\bversionCode\s*=\s*([0-9]+)", text)
    name = re.search(r'\bversionName\s*=\s*"([^"]+)"', text)
    if not code or int(code[1]) <= 0 or not name or not re.fullmatch(VERSION_RE, name[1]):
        raise ValueError("Invalid Android version metadata")
    return int(code[1]), name[1]


def read_notes(path):
    text = Path(path).read_text(encoding="utf-8-sig").strip()
    if not text:
        raise ValueError("Explicit nonempty public release notes are required")
    return text


def require_new_version(code, published_codes, rehearsal):
    if not published_codes or any(type(v) is not int or v <= 0 for v in published_codes):
        raise ValueError("Cannot determine the published stable/beta version floor")
    floor = max(published_codes)
    if code < floor or (code == floor and not rehearsal):
        raise ValueError(f"versionCode {code} must exceed every published channel ({floor}); use --rehearsal only for local verification")


def require_private_location(path):
    path = Path(path).expanduser().resolve()
    if path == Path(path.anchor) or path == Path.home().resolve():
        raise ValueError("Use a dedicated signing backup directory, not a filesystem root or home")
    if path.exists() and (not path.is_dir() or any(p.name not in {"validation.keystore", "backup.json"} or not p.is_file() or p.is_symlink() for p in path.iterdir())):
        raise ValueError("Backup directory contains unrelated files; nothing will be changed")
    if any((parent / ".git").exists() for parent in [path, *path.parents]):
        raise ValueError("Signing backup must be outside every Git worktree")
    return path


def check_backup(folder, source_hash):
    folder = require_private_location(folder)
    manifest = folder / "backup.json"
    key = folder / "validation.keystore"
    if not manifest.is_file() or not key.is_file():
        raise ValueError("Verified signing backup missing; run the backup command first")
    info = json.loads(manifest.read_text(encoding="utf-8"))
    if (info.get("certificate_sha256") != CERTIFICATE_SHA256 or info.get("keystore_sha256") != source_hash
            or sha256(key) != source_hash or info.get("restore_signing_verified") is not True):
        raise ValueError("Backup hash, certificate or restore verification is invalid")
    return key


def check_signature(text):
    signers = re.findall(r"^Signer #[0-9]+ certificate SHA-256 digest: ([0-9a-fA-F]{64})\s*$", text, re.M)
    if [s.lower() for s in signers] != [CERTIFICATE_SHA256]:
        raise ValueError("APK does not have the single pinned RTBus continuity certificate")
    return CERTIFICATE_SHA256


def check_build_metadata(meta, code, name):
    if meta.get("applicationId") != APPLICATION_ID or meta.get("variantName") != "validation":
        raise ValueError("Unexpected application identity or variant")
    elements = meta.get("elements", [])
    if len(elements) != len(ABIS):
        raise ValueError("Expected exactly two ABI outputs")
    found = {}
    for item in elements:
        filters = item.get("filters", [])
        filename = item.get("outputFile", "")
        if (len(filters) != 1 or filters[0].get("filterType") != "ABI"
                or filters[0].get("value") not in ABIS
                or (item.get("versionCode"), item.get("versionName")) != (code, name)
                or not re.fullmatch(r"[A-Za-z0-9_.-]+\.apk", filename)):
            raise ValueError("Invalid version, ABI or unsafe output filename")
        abi = filters[0]["value"]
        if abi in found:
            raise ValueError("Duplicate ABI output")
        found[abi] = filename
    return found


def check_asset_hashes(folder, hashes):
    folder = Path(folder)
    if {p.name for p in folder.iterdir()} != set(hashes):
        raise ValueError("Missing or unexpected public asset")
    for name, digest in hashes.items():
        if not re.fullmatch(r"[A-Za-z0-9_.-]+", name) or not (folder / name).is_file() or (folder / name).is_symlink():
            raise ValueError("Unsafe public asset")
        if sha256(folder / name) != digest:
            raise ValueError("Asset hash mismatch: " + name)


def run_logged(command, log, env=None):
    log = Path(log)
    with log.open("wb") as stream:
        result = subprocess.run([str(x) for x in command], cwd=ROOT, env=env, stdout=stream, stderr=subprocess.STDOUT)
    if result.returncode:
        raise RuntimeError(f"Command failed ({result.returncode}); see {log}")
    return log.read_text(encoding="utf-8", errors="replace")


def tools(args):
    java_home = Path(args.java_home or os.environ.get("JAVA_HOME", ""))
    sdk = Path(args.android_sdk or os.environ.get("ANDROID_HOME", os.environ.get("ANDROID_SDK_ROOT", "")))
    java = java_home / "bin" / ("java.exe" if os.name == "nt" else "java")
    bt = sdk / "build-tools" / args.build_tools
    result = {"java": java, "keytool": java.with_name("keytool.exe" if os.name == "nt" else "keytool"),
              "apksigner": bt / "lib/apksigner.jar", "aapt2": bt / ("aapt2.exe" if os.name == "nt" else "aapt2"),
              "zipalign": bt / ("zipalign.exe" if os.name == "nt" else "zipalign")}
    if any(not p.is_file() for p in result.values()):
        raise ValueError("JDK / Android Build Tools missing; pass --java-home and --android-sdk")
    version = subprocess.run([str(java), "-version"], capture_output=True, text=True, check=True)
    if not re.search(r'version "21(?:\.|\")', version.stdout + version.stderr):
        raise ValueError("Use the project's JDK 21 toolchain")
    env = os.environ.copy()
    env.update(JAVA_HOME=str(java_home.resolve()), ANDROID_HOME=str(sdk.resolve()), ANDROID_SDK_ROOT=str(sdk.resolve()))
    if args.gradle_user_home:
        env["GRADLE_USER_HOME"] = str(Path(args.gradle_user_home).resolve())
    # Existing Android debug-channel credentials only. Never print passwords or pass them in argv.
    env.setdefault("RTBUS_KEYSTORE_PASSWORD", "android")
    return result, env


def key_certificate(key, tool, env):
    if not Path(key).is_file():
        raise ValueError("Existing keystore missing; refusing to let a new key be generated")
    result = subprocess.run([str(tool["keytool"]), "-exportcert", "-alias", "androiddebugkey", "-keystore", str(key),
                             "-storepass:env", "RTBUS_KEYSTORE_PASSWORD"], env=env, capture_output=True)
    if result.returncode or hashlib.sha256(result.stdout).hexdigest() != CERTIFICATE_SHA256:
        raise ValueError("Keystore cannot supply the pinned RTBus certificate")


def protect(folder):
    if os.name != "nt":
        raise ValueError("This backup workflow currently requires Windows NTFS ACL support")
    environment = os.environ.copy()
    # A Python child of PowerShell 7 may inherit incompatible PowerShell 7 module paths.
    environment.pop("PSModulePath", None)
    result = subprocess.run(["powershell.exe", "-NoProfile", "-NonInteractive", "-File", str(ROOT / "tools/protect-signing-directory.ps1"),
                             "-Directory", str(folder)], env=environment, capture_output=True)
    if result.returncode:
        raise RuntimeError("Cannot protect backup directory: " + result.stderr.decode("utf-8", errors="replace").strip())


def backup(args):
    tool, env = tools(args)
    source = Path(args.keystore).expanduser().resolve()
    key_certificate(source, tool, env)
    original_hash = sha256(source)
    folder = require_private_location(args.backup_dir)
    protect(folder)
    key = folder / "validation.keystore"
    if key.exists() and sha256(key) != original_hash:
        raise ValueError("Different backup already exists; it will not be overwritten")
    if not key.exists():
        with key.open("xb") as dest, source.open("rb") as src:
            shutil.copyfileobj(src, dest)
    if sha256(key) != original_hash:
        raise ValueError("Copied keystore hash mismatch")
    key_certificate(key, tool, env)
    work = folder / ("restore-check-" + uuid.uuid4().hex)
    work.mkdir()
    try:
        reference = Path(args.reference_apk).resolve()
        check_signature(run_logged([tool["java"], "-jar", tool["apksigner"], "verify", "--print-certs", reference], work / "reference.log"))
        signed = work / "restore-check.apk"
        run_logged([tool["java"], "-jar", tool["apksigner"], "sign", "--ks", key, "--ks-key-alias", "androiddebugkey",
                    "--ks-pass", "env:RTBUS_KEYSTORE_PASSWORD", "--key-pass", "env:RTBUS_KEYSTORE_PASSWORD",
                    "--out", signed, reference], work / "sign.log", env)
        check_signature(run_logged([tool["java"], "-jar", tool["apksigner"], "verify", "--print-certs", signed], work / "verify.log"))
        if sha256(source) != original_hash:
            raise ValueError("Original keystore changed during backup")
        report = {"certificate_sha256": CERTIFICATE_SHA256, "keystore_sha256": original_hash,
                  "restore_signing_verified": True, "verified_at": datetime.datetime.now().astimezone().isoformat(),
                  "protection": "Windows ACL: current user and SYSTEM", "off_device_backup_verified": False}
        write_json(folder / "backup.json", report)
    finally:
        # Only unlink files in this exact newly created restore-check directory.
        if work.resolve().parent != folder.resolve():
            raise ValueError("Unsafe restore-check cleanup path")
        for file in work.iterdir():
            if file.is_file() and not file.is_symlink(): file.unlink()
        work.rmdir()
    check_backup(folder, original_hash)
    protect(folder)
    print(json.dumps({"backup_directory": str(folder), "certificate_sha256": CERTIFICATE_SHA256,
                      "restore_signing_verified": True, "off_device_backup_verified": False}, indent=2))


def fetch_channels():
    result = {}
    for name in ("update.json", "update-beta.json"):
        url = "https://raw.githubusercontent.com/" + REPOSITORY + "/main/" + name
        with urllib.request.urlopen(url, timeout=40) as response:
            result[name] = json.load(response)
    return result


def audit_apks(tool, output, code, name):
    spec = importlib.util.spec_from_file_location("native_guard", ROOT / ".github/scripts/check_native_sdk_artifacts.py")
    guard = importlib.util.module_from_spec(spec); spec.loader.exec_module(guard)
    logs = output / "logs"
    guard.check_dependencies(logs / "dependencies.log")
    manifest = ROOT / "androidApp/build/intermediates/merged_manifest/validation/processValidationMainManifest/AndroidManifest.xml"
    guard.check_manifest(manifest)
    app = ET.parse(manifest).getroot().find("application")
    android = "{http://schemas.android.com/apk/res/android}"
    if app.get(android + "debuggable", "false") != "false" or app.find("profileable") is None or app.find("profileable").get(android + "shell") != "true":
        raise ValueError("Validation debug/profile flags changed")
    apk_dir = ROOT / "androidApp/build/outputs/apk/validation"
    meta = json.loads((apk_dir / "output-metadata.json").read_text(encoding="utf-8"))
    files = check_build_metadata(meta, code, name)
    if (ROOT / "androidApp/build/outputs/mapping/validation/mapping.txt").stat().st_size <= 0:
        raise ValueError("R8 mapping missing")
    for abi, filename in files.items():
        apk = apk_dir / filename
        if apk.is_symlink(): raise ValueError("Symlinked APK output")
        libs = guard.check_apk(apk)
        if not libs: raise ValueError("Expected native ABI libraries missing")
        check_signature(run_logged([tool["java"], "-jar", tool["apksigner"], "verify", "--verbose", "--print-certs", apk], logs / (abi + "-signature.log")))
        run_logged([tool["zipalign"], "-c", "-P", "16", "-v", "4", apk], logs / (abi + "-zipalign.log"))
        badging = run_logged([tool["aapt2"], "dump", "badging", apk], logs / (abi + "-badging.log"))
        package = re.search(r"package: name='([^']+)' versionCode='([^']+)' versionName='([^']+)'", badging)
        if not package or package.groups() != (APPLICATION_ID, str(code), name) or "application-debuggable" in badging:
            raise ValueError("Actual APK package/version/debug flag differs from metadata")
        if any(permission not in badging for permission in ("android.permission.VIBRATE", "android.permission.ACCESS_NETWORK_STATE")):
            raise ValueError("Required reminder/network permissions missing")
        if re.findall(r"native-code: '([^']+)'", badging) != [abi]:
            raise ValueError("Actual APK ABI differs from metadata")
        with ZipFile(apk) as archive:
            if not any(n.startswith("assets/dexopt/") for n in archive.namelist()): raise ValueError("Baseline Profile missing")
            for lib in libs:
                if not lib.startswith("lib/" + abi + "/"): raise ValueError("Mixed ABI libraries")
                data = archive.read(lib)
                if data[:4] != b"\x7fELF": raise ValueError("Invalid ELF")
                endian = "<" if data[5] == 1 else ">"
                if data[4] == 2:
                    offset = struct.unpack_from(endian + "Q", data, 32)[0]
                    size, count = struct.unpack_from(endian + "HH", data, 54)
                    for i in range(count):
                        pos = offset + i * size
                        if struct.unpack_from(endian + "I", data, pos)[0] != 1: continue
                        fileoff, address = struct.unpack_from(endian + "QQ", data, pos + 8)
                        alignment = struct.unpack_from(endian + "Q", data, pos + 48)[0]
                        if alignment < 16384 or fileoff % 16384 != address % 16384: raise ValueError("ELF 16 KB alignment failed")
        shutil.copyfile(apk, output / "assets" / f"RTBus-{name}-{abi}.apk")


def verify(folder):
    folder = Path(folder).resolve()
    report = json.loads((folder / "report.json").read_text(encoding="utf-8"))
    if report.get("certificate_sha256") != CERTIFICATE_SHA256 or report.get("application_id") != APPLICATION_ID:
        raise ValueError("Candidate identity differs from the continuity policy")
    code, name = report["version_code"], report["version_name"]
    if type(code) is not int or code <= 0 or not isinstance(name, str) or not re.fullmatch(VERSION_RE, name):
        raise ValueError("Invalid candidate version")
    rehearsal, ready = report.get("rehearsal"), report.get("ready_for_publication")
    if type(rehearsal) is not bool or type(ready) is not bool or ready == rehearsal:
        raise ValueError("Candidate rehearsal/readiness flags conflict")
    if report.get("source_dirty") and not rehearsal:
        raise ValueError("Dirty source cannot claim publication readiness")
    require_new_version(code, report.get("published_version_codes", []), rehearsal)
    expected = {f"RTBus-{name}-{abi}.apk" for abi in ABIS} | {"SHA256SUMS.txt", "LICENSE"}
    if set(report["assets"]) != expected: raise ValueError("Unexpected public asset manifest")
    check_asset_hashes(folder / "assets", report["assets"])
    sums = "".join(f"{report['assets'][f'RTBus-{name}-{abi}.apk']}  RTBus-{name}-{abi}.apk\n" for abi in ABIS)
    if (folder / "assets/SHA256SUMS.txt").read_text(encoding="ascii") != sums: raise ValueError("Checksum file mismatch")
    channel = "update-beta.json" if "-" in name else "update.json"
    data = json.loads((folder / "metadata" / channel).read_text(encoding="utf-8"))
    urls = {abi: f"https://github.com/{REPOSITORY}/releases/download/v{name}/RTBus-{name}-{abi}.apk" for abi in ABIS}
    if data.get("versionCode") != code or data.get("versionName") != name or data.get("downloadUrls") != urls or data.get("downloadUrl") != urls[ABIS[0]]:
        raise ValueError("Generated update metadata differs from audited artifacts")
    if "-" not in name and (folder / "metadata/version.json").read_bytes() != (folder / "metadata/update.json").read_bytes():
        raise ValueError("Stable metadata copies differ")
    print(json.dumps({"candidate": str(folder), "verified": True, "rehearsal": report["rehearsal"],
                      "ready_for_publication": report["ready_for_publication"], "version_code": code}, indent=2))
    return report


def prepare(args):
    notes = read_notes(args.notes_file)
    code, name = read_version(ROOT / "androidApp/build.gradle.kts")
    tool, env = tools(args)
    key = Path(args.keystore).expanduser().resolve()
    key_certificate(key, tool, env)
    check_backup(args.backup_dir, sha256(key))
    protect(Path(args.backup_dir).expanduser().resolve())
    channels = fetch_channels()
    require_new_version(code, [v.get("versionCode") for v in channels.values()], args.rehearsal)
    dirty = bool(subprocess.check_output(["git", "status", "--porcelain"], cwd=ROOT, text=True).strip())
    if dirty and not args.rehearsal: raise ValueError("Commit the source before preparing a publishable candidate")
    commit = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip()
    label = datetime.datetime.now().strftime("%Y%m%d-%H%M%S") + "-" + uuid.uuid4().hex[:8]
    output = ROOT / "build/rtbus-release" / name / label
    output.mkdir(parents=True, exist_ok=False)
    for part in ("logs", "assets", "metadata"): (output / part).mkdir()
    logs = output / "logs"
    print("Preparing local candidate: " + str(output), flush=True)
    run_logged([sys.executable, "-X", "utf8", "-m", "unittest", "discover", "-s", ".github/scripts", "-p", "test_*.py"], logs / "static.log", env)
    gradle = [tool["java"], "-cp", ROOT / "gradle/wrapper/gradle-wrapper.jar", "org.gradle.wrapper.GradleWrapperMain", "--console=plain", "--max-workers=4"]
    if args.offline: gradle.append("--offline")
    signing = run_logged(gradle + [":androidApp:signingReport"], logs / "signing.log", env)
    match = re.search(r"(?ms)^Variant: validation\s*\n(.*?)(?=^Variant:|\Z)", signing)
    if not match: raise ValueError("Cannot locate the validation signing configuration")
    digest = re.search(r"(?m)^SHA-256: ([0-9A-Fa-f:]+)", match[1])
    store = re.search(r"(?m)^Store: (.+)", match[1])
    if not digest or digest[1].replace(":", "").lower() != CERTIFICATE_SHA256 or not store or Path(store[1].strip()).resolve() != key:
        raise ValueError("Gradle validation signing config differs from the backed-up keystore")
    run_logged(gradle + ["test", ":androidApp:assembleValidation"], logs / "build.log", env)
    run_logged(gradle + [":androidApp:dependencies", "--configuration", "validationRuntimeClasspath"], logs / "dependencies.log", env)
    audit_apks(tool, output, code, name)
    shutil.copyfile(ROOT / "LICENSE", output / "assets/LICENSE")
    sums = "".join(f"{sha256(output / 'assets' / f'RTBus-{name}-{abi}.apk')}  RTBus-{name}-{abi}.apk\n" for abi in ABIS)
    (output / "assets/SHA256SUMS.txt").write_bytes(sums.encode("ascii"))
    channel = "update-beta.json" if "-" in name else "update.json"
    write_json(output / "metadata" / channel, channels[channel])
    env.update(TAG_NAME="v" + name, RELEASE_BODY=notes, OUTPUT_DIR=str(output / "metadata"))
    run_logged([sys.executable, "-X", "utf8", ROOT / ".github/scripts/update_metadata.py"], logs / "metadata.log", env)
    counts = {k: 0 for k in ("tests", "failures", "errors", "skipped")}
    for file in ROOT.glob("**/build/test-results/**/TEST-*.xml"):
        if (ROOT / "build") in file.parents: continue
        suite = ET.parse(file).getroot()
        for k in counts: counts[k] += int(suite.get(k, 0))
    if not counts["tests"] or any(counts[k] for k in ("failures", "errors", "skipped")): raise ValueError("Host test results are missing or not entirely green")
    if sha256(key) != sha256(check_backup(args.backup_dir, sha256(key))): raise ValueError("Signing backup drifted")
    report = {"application_id": APPLICATION_ID, "certificate_sha256": CERTIFICATE_SHA256, "version_code": code,
              "version_name": name, "source_commit": commit, "source_dirty": dirty, "rehearsal": args.rehearsal,
              "ready_for_publication": not args.rehearsal, "published_version_codes": [v["versionCode"] for v in channels.values()],
              "host_tests": counts, "assets": {p.name: sha256(p) for p in sorted((output / "assets").iterdir())}}
    write_json(output / "report.json", report)
    verify(output)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="command", required=True)
    for name in ("backup", "prepare"):
        p = commands.add_parser(name)
        p.add_argument("--java-home"); p.add_argument("--android-sdk"); p.add_argument("--gradle-user-home")
        p.add_argument("--build-tools", default="36.0.0")
        p.add_argument("--keystore", default=str(Path.home() / ".android/debug.keystore"))
        p.add_argument("--backup-dir", default=str(BACKUP_DIR))
        if name == "backup": p.add_argument("--reference-apk", required=True)
        else:
            p.add_argument("--notes-file", required=True); p.add_argument("--offline", action="store_true")
            p.add_argument("--rehearsal", action="store_true", help="Local verification only; never a publishable candidate")
    commands.add_parser("verify").add_argument("candidate")
    args = parser.parse_args()
    try:
        if args.command == "backup": backup(args)
        elif args.command == "prepare": prepare(args)
        else: verify(args.candidate)
    except (ValueError, RuntimeError, OSError, subprocess.CalledProcessError) as error:
        parser.exit(1, "Stopped: " + str(error) + "\n")


if __name__ == "__main__": main()
