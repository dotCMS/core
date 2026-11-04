"""The opt-in profile changes only dotCMS's JVM environment, never shared search services."""
import unittest
from pathlib import Path
from xml.etree import ElementTree as ET


class ProfileTest(unittest.TestCase):
    def test_profile_preserves_other_jvm_and_container_settings(self):
        root = ET.parse(Path(__file__).resolve().parents[3] / 'dotcms-postman/pom.xml').getroot()
        ns = {'m': 'http://maven.apache.org/POM/4.0.0'}
        profile = root.find("m:profiles/m:profile[m:id='test-tier1']", ns)
        self.assertIsNotNone(profile)
        self.assertIsNone(profile.find('m:activation', ns))
        plugin = profile.find('m:build/m:plugins/m:plugin', ns)
        self.assertEqual(plugin.findtext('m:artifactId', namespaces=ns), 'docker-maven-plugin')
        images = plugin.find('m:configuration/m:imagesMap', ns)
        self.assertEqual([item.tag.split('}')[-1] for item in images], ['dotcms'])
        env = images.find('m:dotcms/m:run/m:env', ns)
        self.assertEqual([item.tag.split('}')[-1] for item in env], ['JAVA_TOOL_OPTIONS'])
        self.assertEqual(env[0].text, '-XX:TieredStopAtLevel=1')


if __name__ == '__main__':
    unittest.main()
