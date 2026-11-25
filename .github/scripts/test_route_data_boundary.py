"""Source guards for retired planning data; not runtime or iOS acceptance."""

from pathlib import Path
import re
import unittest


ROOT = Path(__file__).resolve().parents[2]


class RouteDataBoundaryTest(unittest.TestCase):
    def test_shared_data_has_no_planning_types_or_fakes(self):
        retired = re.compile(
            r"\b(?:RoutePlanRepository|DefaultRoutePlanRepository|AmapSchemeAlignmentService|"
            r"AmapRouteGateway|WebAmapRouteGateway|FakeAmapRouteGateway|"
            r"AmapTipItem|AmapTransitSchemeResult|MAX_VISIBLE_SCHEMES)\b"
        )
        violations = []
        for module in ("data", "model", "testing"):
            for path in (ROOT / "core" / module / "src").rglob("*.kt"):
                if retired.search(path.read_text(encoding="utf-8")):
                    violations.append(path.relative_to(ROOT).as_posix())
        self.assertEqual([], sorted(violations))

    def test_network_has_no_amap_planning_client_cache_or_endpoint(self):
        retired = ("remote.amap", "AmapTransitApiClient", "AmapLruCache", "restapi.amap.com")
        violations = []
        for path in (ROOT / "core/network/src").rglob("*.kt"):
            source = path.read_text(encoding="utf-8")
            if any(token in source for token in retired):
                violations.append(path.relative_to(ROOT).as_posix())
        self.assertEqual([], sorted(violations))

    def test_platform_wiring_has_no_web_planning_bindings(self):
        paths = [ROOT / "androidApp/src/main/java/com/trackr/di/AppModule.kt"]
        paths += list((ROOT / "composeApp/src").rglob("*.kt"))
        retired = ("AmapTransitApiClient", "AmapRouteGateway", "WebAmapRouteGateway", "RoutePlanRepository")
        violations = []
        for path in paths:
            source = path.read_text(encoding="utf-8")
            if any(token in source for token in retired):
                violations.append(path.relative_to(ROOT).as_posix())
        self.assertEqual([], sorted(violations))


if __name__ == "__main__":
    unittest.main(verbosity=2)
