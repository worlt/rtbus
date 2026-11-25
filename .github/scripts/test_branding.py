"""Offline checks for RTBus display names; no Android SDK or network required."""

import plistlib
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]


def android_string(relative_path, name):
    element = ET.parse(ROOT / relative_path).getroot().find(f"string[@name='{name}']")
    if element is None:
        raise AssertionError(f"Missing {name} in {relative_path}")
    return element.text


class BrandingTest(unittest.TestCase):
    def test_android_release_names(self):
        for locale in ("values", "values-en"):
            with self.subTest(locale=locale):
                self.assertEqual(
                    android_string(f"androidApp/src/main/res/{locale}/strings.xml", "app_name"),
                    "实时公交" if locale == "values" else "Real-Time Bus",
                )

    def test_android_debug_names(self):
        for locale in ("values", "values-en"):
            with self.subTest(locale=locale):
                self.assertEqual(
                    android_string(f"androidApp/src/debug/res/{locale}/strings.xml", "app_name"),
                    "实时公交.D" if locale == "values" else "RTBus.D",
                )

    def test_about_name_and_permission_copy(self):
        path = "feature/me/src/commonMain/composeResources/values/strings.xml"
        self.assertEqual(android_string(path, "me_app_name"), "实时公交")
        self.assertEqual(android_string(path, "me_about_app"), "关于实时公交")
        self.assertIn("实时公交", android_string(path, "me_about_install_permission"))

    def test_ios_display_names(self):
        for path in ("iosApp/Trackr/Info.plist", "iosApp/TrackrWidgets/Info.plist"):
            with self.subTest(path=path):
                with (ROOT / path).open("rb") as stream:
                    self.assertEqual(plistlib.load(stream)["CFBundleDisplayName"], "实时公交")

    def test_gradle_project_name(self):
        self.assertIn('rootProject.name = "RTBus"', (ROOT / "settings.gradle.kts").read_text(encoding="utf-8"))

    def test_upstream_credit_is_preserved(self):
        license_text = (ROOT / "LICENSE").read_text(encoding="utf-8")
        self.assertIn("MIT License", license_text)
        self.assertIn("Copyright (c) 2026 busonroad contributors", license_text)
        about = (ROOT / "feature/me/src/commonMain/kotlin/com/trackr/ui/screens/me/AboutScreen.kt").read_text(encoding="utf-8")
        self.assertIn('title = "上游项目"', about)
        self.assertIn('value = "5151561/Trackr"', about)
        self.assertIn("Copyright (c) 2026 busonroad contributors", about)


if __name__ == "__main__":
    unittest.main(verbosity=2)
