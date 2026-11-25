"""Source-boundary guards for the removed route planner; not runtime acceptance."""

from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[2]


class RouteFeatureBoundaryTest(unittest.TestCase):
    def test_route_feature_has_no_planning_or_map_sdk_sources(self):
        for path in (ROOT / "feature/route/src").rglob("*.kt"):
            with self.subTest(path=path.relative_to(ROOT).as_posix()):
                source = path.read_text(encoding="utf-8")
                self.assertNotIn("com.trackr.ui.screens.routeplan", source)
                self.assertNotIn("import com.amap.api.", source)
                self.assertNotIn("AmapSdkManager", source)

    def test_route_feature_has_no_planning_only_dependencies(self):
        build = (ROOT / "feature/route/build.gradle.kts").read_text(encoding="utf-8")
        self.assertNotIn("libs.amap", build)
        self.assertNotIn('project(":core:network")', build)

    def test_ios_has_no_native_route_map_bridge(self):
        paths = list((ROOT / "composeApp/src/iosMain").rglob("*.kt"))
        paths += list((ROOT / "iosApp/Trackr").rglob("*.swift"))
        paths += [ROOT / "iosApp/Trackr.xcodeproj/project.pbxproj"]
        for path in paths:
            with self.subTest(path=path.relative_to(ROOT).as_posix()):
                source = path.read_text(encoding="utf-8")
                for removed in ("IosRouteMapBridge", "isRouteMapSupported", "installIosMap", "NativeRouteMap"):
                    self.assertNotIn(removed, source)


if __name__ == "__main__":
    unittest.main(verbosity=2)
