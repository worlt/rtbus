"""Check actual Android dependency reports, merged manifests and APKs for retired SDKs."""

import argparse
from pathlib import Path
import re
import xml.etree.ElementTree as ET
from zipfile import ZipFile


VENDOR = re.compile(r"com[./](?:amap|autonavi|loc)(?:[./;]|$)|3dmap-location-search", re.I)
NATIVE_VENDOR = re.compile(r"amap|autonavi|apssdk", re.I)
DEX_VENDOR = re.compile(rb"Lcom/(?:amap|autonavi|loc)/")


def check_dependencies(path):
    text = Path(path).read_text(encoding="utf-8-sig")
    has_runtime_graph = re.search(r"(?m)^\w*RuntimeClasspath(?:\s|$)", text)
    if not has_runtime_graph or "BUILD SUCCESSFUL" not in text or "FAILED" in text or VENDOR.search(text):
        raise ValueError(f"Invalid dependency report or retired SDK dependency: {path}")


def check_manifest(path):
    root = ET.parse(path).getroot()
    if root.tag != "manifest" or root.find("application") is None:
        raise ValueError(f"Not an Android merged manifest: {path}")
    for element in root.iter():
        for value in element.attrib.values():
            if VENDOR.search(value) or "AMAP_ANDROID_KEY" in value:
                raise ValueError(f"Retired SDK manifest entry: {path}: {value}")


def check_apk(path):
    with ZipFile(path) as apk:
        names = apk.namelist()
        if "AndroidManifest.xml" not in names or "classes.dex" not in names:
            raise ValueError(f"Not an APK: {path}")
        if apk.testzip() is not None:
            raise ValueError(f"Corrupt APK: {path}")
        libs = sorted(name for name in names if name.startswith("lib/") and name.endswith(".so"))
        for name in names:
            if NATIVE_VENDOR.search(name):
                raise ValueError(f"Retired SDK APK entry: {path}: {name}")
            if re.fullmatch(r"classes\d*\.dex", name) and DEX_VENDOR.search(apk.read(name)):
                raise ValueError(f"Retired SDK DEX descriptor: {path}: {name}")
        return libs


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--dependencies", nargs="+", required=True, type=Path)
    parser.add_argument("--manifests", nargs="+", required=True, type=Path)
    parser.add_argument("--apks", nargs="+", required=True, type=Path)
    args = parser.parse_args()
    for path in args.dependencies:
        check_dependencies(path)
        print(f"Dependencies OK: {path}")
    for path in args.manifests:
        check_manifest(path)
        print(f"Manifest OK: {path}")
    for path in args.apks:
        libs = check_apk(path)
        print(f"APK OK: {path} ({len(libs)} native libraries)")
        for lib in libs:
            print(f"  {lib}")


if __name__ == "__main__":
    main()
