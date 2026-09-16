from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[4]
report = ET.parse(root / 'dotCMS/target/surefire-reports/TEST-com.dotcms.storage.BinaryS3StorageTest.xml')
classpath = next(p.attrib['value'] for p in report.findall('./properties/property') if p.attrib['name'] == 'java.class.path')
if '/junit-platform-launcher/' not in classpath:
    engine = next(Path(p) for p in classpath.split(':') if '/junit-platform-engine/' in p and p.endswith('.jar'))
    version = engine.parent.name
    launcher = engine.parent.parent.parent / 'junit-platform-launcher' / version / f'junit-platform-launcher-{version}.jar'
    if not launcher.is_file():
        raise SystemExit(f'Run the core tests first; matching JUnit launcher is missing: {launcher}')
    classpath += ':' + str(launcher)
classpath = '/mnt/nfs/runner:' + classpath.replace(str(root), '/workspace').replace(str(Path.home() / '.m2/repository'), '/m2')
output = root / 'dotCMS/target/s3-nfs'
output.mkdir(exist_ok=True)
(output / 'java.args').write_text('-Xmx768m\n-XX:+EnableDynamicAgentLoading\n-Djava.io.tmpdir=/mnt/nfs/junit\n--add-opens=java.base/java.lang=ALL-UNNAMED\n--add-opens=java.base/java.util=ALL-UNNAMED\n-classpath\n"' + classpath + '"\n')
(output / 'javac.args').write_text('-proc:none\n-classpath\n"' + classpath + '"\n-d\n/mnt/nfs/runner\n')
