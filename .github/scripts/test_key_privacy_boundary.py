"""Offline retirement boundaries; runtime tests cover settings and restored navigation."""
import os
from pathlib import Path
import re
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(os.environ.get("RTBUS_BOUNDARY_ROOT", Path(__file__).resolve().parents[2]))

class KeyPrivacyBoundaryTest(unittest.TestCase):
    def test_no_active_key_repository_storage_or_privacy_pipeline(self):
        retired = re.compile(r'\b(?:AmapKeyRepository|AmapKeyDefaults|DefaultAmapKeyRepository|AmapKeyStore|DataStoreAmapKeyStore|SharedPrefsAmapKeyStore|LegacyAmapKeyMigration|AmapKeyKeys|AmapKeyStorage|AppSigningInfo)\b')
        for folder in ('androidApp', 'composeApp', 'core', 'feature'):
            for path in (ROOT / folder).rglob('*.kt'):
                if not any(part in ('commonMain', 'androidMain', 'iosMain', 'main') for part in path.parts) or 'build' in path.relative_to(ROOT).parts:
                    continue
                self.assertNotRegex(path.read_text(encoding='utf-8'), retired, str(path.relative_to(ROOT)))

        ios = (ROOT / 'composeApp/src/iosMain/kotlin/com/trackr/app/di/IosAppRuntime.kt').read_text(encoding='utf-8')
        self.assertNotRegex(ios, r'\bsingle\s*\{\s*(?:keys|defaults)\s*\}', 'Retired anonymous iOS Key bindings')
        for relative in ('androidApp/src/main/java/com/trackr/data/local/StorageBackend.kt',
                         'composeApp/src/iosMain/kotlin/com/trackr/app/di/IosAppRuntime.kt'):
            self.assertNotRegex((ROOT / relative).read_text(encoding='utf-8'),
                                r'TrackrDataStoreFiles\.AMAP_KEYS|trackr_amap_keys', relative)

    def test_me_has_no_key_entry_or_resources(self):
        for path in (ROOT / 'feature/me/src/commonMain').rglob('*'):
            if path.suffix in ('.kt', '.xml'):
                self.assertNotRegex(path.read_text(encoding='utf-8'), r'AmapKey|amapKey|me_amap_|me_group_map', str(path.relative_to(ROOT)))

    def test_no_build_time_amap_key_configuration(self):
        for folder in ('androidApp', 'composeApp', 'iosApp', 'build-logic'):
            for path in (ROOT / folder).rglob('*'):
                if (path.suffix in ('.kts', '.swift', '.xcconfig') or path.name.endswith('.xcconfig.example')) and 'build' not in path.relative_to(ROOT).parts and path.name != "Local.xcconfig":
                    self.assertNotRegex(path.read_text(encoding='utf-8'), r'AMAP_(?:WEB|IOS|ANDROID)_KEY', str(path.relative_to(ROOT)))

    def test_historical_sensitive_files_stay_excluded_from_all_backup_modes(self):
        files = ROOT / 'androidApp/src/main/res/xml'
        legacy = ET.parse(files / 'backup_rules.xml').getroot()
        extraction = ET.parse(files / 'data_extraction_rules.xml').getroot()
        sections = [legacy, extraction.find('cloud-backup'), extraction.find('device-transfer')]
        for section in sections:
            rules = {(e.tag, e.get('domain'), e.get('path')) for e in section}
            self.assertIn(('exclude', 'file', 'datastore/trackr_amap_keys.preferences_pb'), rules)
            self.assertIn(('exclude', 'sharedpref', 'trackr_amap_keys.xml'), rules)
            self.assertFalse(any(e.tag == 'include' for e in section))
            for domain, path in (('file', 'datastore/trackr_settings.preferences_pb'), ('database', 'trackr.db'), ('sharedpref', 'trackr_prefs.xml')):
                self.assertFalse(any(e.tag == 'exclude' and e.get('domain') == domain and (e.get('path') == '.' or path == e.get('path') or path.startswith(e.get('path').rstrip('/') + '/')) for e in section))

    def test_old_key_navigation_identifier_remains_serializable(self):
        nav = ROOT / 'core/navigation/src/commonMain/kotlin/com/trackr/core/navigation'
        self.assertRegex((nav / 'ScreenKey.kt').read_text(encoding='utf-8'), r'@Serializable\s+data object AmapKey')
        self.assertIn('subclass(ScreenKey.AmapKey.serializer())', (nav / 'TrackrSavedStateConfiguration.kt').read_text(encoding='utf-8'))

if __name__ == '__main__':
    unittest.main()
