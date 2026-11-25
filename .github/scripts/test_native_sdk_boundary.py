"""Source and artifact guard regressions, not iOS or device runtime acceptance."""

import os
from pathlib import Path
import re
from tempfile import TemporaryDirectory
import unittest
import xml.etree.ElementTree as ET
from zipfile import ZipFile

from check_native_sdk_artifacts import check_apk, check_dependencies, check_manifest


ROOT = Path(os.environ.get("RTBUS_BOUNDARY_ROOT", Path(__file__).resolve().parents[2]))


def source(path):
    return (ROOT / path).read_text(encoding="utf-8")


class NativeSdkBoundaryTest(unittest.TestCase):
    def test_android_build_has_no_native_sdk_dependency_key_injection_or_mirror(self):
        text = "\n".join(source(path) for path in (
            "androidApp/build.gradle.kts", "settings.gradle.kts", "gradle/libs.versions.toml",
            "androidApp/proguard-rules.pro",
        ))
        for retired in ("libs.amap", "com.amap", "com.autonavi", "com.loc.", "amap3dBundle",
                        "AMAP_ANDROID_KEY", "amapKeyStatus", "maven.aliyun.com"):
            self.assertNotIn(retired, text)

    def test_android_sources_no_longer_import_native_sdk(self):
        for directory in ("androidApp", "composeApp", "core", "feature"):
            for path in (ROOT / directory).rglob("*.kt"):
                if "build" not in path.relative_to(ROOT).parts:
                    self.assertNotRegex(path.read_text(encoding="utf-8"),
                                        r"(?m)^import com\.(?:amap|autonavi|loc)\.", str(path))

    def test_manifest_keeps_location_tracking_widgets_and_updates_without_vendor_components(self):
        path = ROOT / "androidApp/src/main/AndroidManifest.xml"
        check_manifest(path)
        android = "{http://schemas.android.com/apk/res/android}"
        root = ET.parse(path).getroot()
        names = {node.get(android + "name") for node in root.iter()}
        self.assertTrue({"android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION",
                         "android.permission.FOREGROUND_SERVICE_LOCATION", "android.permission.POST_NOTIFICATIONS",
                         "android.permission.REQUEST_INSTALL_PACKAGES", ".service.BusTrackingService",
                         ".widget.BusStopWidgetProvider", ".receiver.BootCompletedReceiver",
                         "androidx.core.content.FileProvider"}.issubset(names))

    def test_network_policy_keeps_only_retained_city_index_cleartext_exception(self):
        root = ET.fromstring(source("androidApp/src/main/res/xml/network_security_config.xml"))
        self.assertEqual("false", root.find("base-config").get("cleartextTrafficPermitted"))
        self.assertEqual(["222.76.217.238"], [node.text for node in root.iter("domain")])

    def test_ios_has_no_cocoapods_or_vendor_link_configuration(self):
        self.assertFalse((ROOT / "iosApp/Podfile").exists())
        self.assertFalse((ROOT / "iosApp/Podfile.lock").exists())
        paths = [ROOT / "iosApp" / name for name in (
            "Trackr.xcodeproj/project.pbxproj", "Configuration/Debug.xcconfig", "Configuration/Release.xcconfig",
            "Configuration/Shared.xcconfig", "Configuration/Local.xcconfig.example",
        )]
        for directory in ("Trackr", "TrackrWidgets"):
            paths += [p for p in (ROOT / "iosApp" / directory).rglob("*") if p.suffix in (".plist", ".swift")]
        for path in paths:
            with self.subTest(path=path.relative_to(ROOT)):
                self.assertNotRegex(path.read_text(encoding="utf-8"), r"(?i)Pods|CocoaPods|AMap|autonavi")
        text = source("iosApp/Configuration/Shared.xcconfig")
        self.assertNotIn("OTHER_LDFLAGS[sdk=iphonesimulator", text)

    def test_ios_project_keeps_shared_framework_widgets_and_resolvable_object_references(self):
        text = source("iosApp/Trackr.xcodeproj/project.pbxproj")
        definitions = re.findall(r"(?m)^\s*([A-F0-9]{24}) (?:/\*.*?\*/ )?= \{", text)
        self.assertEqual(len(definitions), len(set(definitions)))
        references = set(re.findall(r"\b[A-F0-9]{24}\b", text))
        self.assertEqual(set(), references - set(definitions))
        for kept in ("embedAndSignAppleFrameworkForXcode", "Embed App Extensions", "TrackrWidgets.swift in Sources",
                     "TrackrTests", "TrackrUITests", "ComposeApp,", "$(inherited)"):
            self.assertIn(kept, text)


class ArtifactGuardTest(unittest.TestCase):
    def setUp(self):
        self.temp = TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)

    def test_dependency_guard_rejects_vendor_and_incomplete_reports(self):
        path = self.root / "dependencies.log"
        for bad in ("", "BUILD FAILED", "BUILD SUCCESSFUL",
                    "debugRuntimeClasspath\nBUILD SUCCESSFUL\ncom.amap.api:3dmap-location-search:1.0",
                    "releaseRuntimeClasspath\nBUILD SUCCESSFUL\nsome:dependency FAILED"):
            path.write_text(bad, encoding="utf-8")
            with self.assertRaises(ValueError):
                check_dependencies(path)
        path.write_text("debugRuntimeClasspath - Runtime dependencies\nandroidx:sqlite:1.0\nBUILD SUCCESSFUL", encoding="utf-8")
        check_dependencies(path)

    def test_manifest_guard_rejects_vendor_service_and_metadata(self):
        path = self.root / "AndroidManifest.xml"
        for entry in ('<service name="com.amap.api.location.APSService"/>',
                      '<meta-data name="com.amap.api.v2.apikey" value="old"/>'):
            path.write_text(f"<manifest><application>{entry}</application></manifest>", encoding="utf-8")
            with self.assertRaises(ValueError):
                check_manifest(path)
        path.write_text('<manifest><application><service name="com.trackr.service.BusTrackingService"/></application></manifest>',
                        encoding="utf-8")
        check_manifest(path)

    def test_apk_guard_rejects_vendor_libraries_and_secondary_dex(self):
        path = self.root / "fixture.apk"
        for name, content in (("lib/arm64-v8a/libAMapSDK_MAP_v11_3_100.so", b"elf"),
                              ("lib/armeabi-v7a/libapssdk.so", b"elf"),
                              ("classes2.dex", b"dex Lcom/autonavi/map/Map;")):
            with ZipFile(path, "w") as apk:
                apk.writestr("AndroidManifest.xml", b"manifest")
                apk.writestr("classes.dex", b"dex")
                apk.writestr(name, content)
            with self.assertRaises(ValueError):
                check_apk(path)
        with ZipFile(path, "w") as apk:
            apk.writestr("AndroidManifest.xml", b"manifest")
            apk.writestr("classes.dex", b"dex Lcom/trackr/service/BusTrackingService;")
            apk.writestr("lib/arm64-v8a/libsqliteJni.so", b"elf")
        self.assertEqual(["lib/arm64-v8a/libsqliteJni.so"], check_apk(path))


if __name__ == "__main__":
    unittest.main(verbosity=2)
