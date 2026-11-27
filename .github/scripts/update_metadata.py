#!/usr/bin/env python3
"""Generate RTBus release metadata locally, without reading private commit messages."""
import datetime
import json
import os
from pathlib import Path
import re

RELEASE_REPOSITORY = "worlt/rtbus-releases"


def main():
    tag = os.environ.get("TAG_NAME", "").strip().removeprefix("refs/tags/")
    if not re.fullmatch(r"v[0-9]+\.[0-9]+\.[0-9]+(?:-[0-9A-Za-z.]+)?", tag):
        raise SystemExit("TAG_NAME must be an explicit RTBus version tag, such as v0.1.16")
    notes = os.environ.get("RELEASE_BODY", "").strip()
    if not notes:
        raise SystemExit("RELEASE_BODY must contain explicit public release notes")
    gradle = Path("androidApp/build.gradle.kts").read_text(encoding="utf-8")
    code = re.search(r"versionCode\s*=\s*([0-9]+)", gradle)
    name = re.search(r'versionName\s*=\s*"([^"]+)"', gradle)
    if not code or not name or int(code.group(1)) <= 0:
        raise SystemExit("Cannot read a valid Android version from the source tree")
    version_name = name.group(1)
    if tag != "v" + version_name:
        raise SystemExit("TAG_NAME must match the versionName actually built")
    out_dir = Path(os.environ.get("OUTPUT_DIR", "build/rtbus-release-metadata"))
    is_beta = "-" in version_name
    filenames = ["update-beta.json"] if is_beta else ["update.json", "version.json"]
    previous = out_dir / filenames[0]
    old = json.loads(previous.read_text(encoding="utf-8")) if previous.exists() else {}
    base = f"https://github.com/{RELEASE_REPOSITORY}/releases/download/{tag}"
    urls = {abi: f"{base}/RTBus-{version_name}-{abi}.apk"
            for abi in ["arm64-v8a", "armeabi-v7a"]}
    update = {
        "versionCode": int(code.group(1)),
        "versionName": version_name,
        "minSupportedVersionCode": old.get("minSupportedVersionCode", 1),
        "changelog": notes,
        "downloadUrl": urls["arm64-v8a"],
        "downloadUrls": urls,
        "publishTime": datetime.date.today().isoformat(),
        "forceUpdate": old.get("forceUpdate", False),
    }
    out_dir.mkdir(parents=True, exist_ok=True)
    encoded = (json.dumps(update, ensure_ascii=False, indent=2) + "\n").encode("utf-8")
    for filename in filenames:
        (out_dir / filename).write_bytes(encoded)
    print(json.dumps({"repository": RELEASE_REPOSITORY,
                      "versionCode": update["versionCode"], "files": filenames}, ensure_ascii=False))


if __name__ == "__main__":
    main()
