"""Offline regression tests for Maven publication; no AWS credentials or network needed.

Run: python3 -m unittest discover -s .github/scripts/publish-to-s3/tests -v
"""

import base64
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
import xml.etree.ElementTree as ET


SCRIPT = Path(__file__).resolve().parents[1] / "publish.sh"
VERSION = "26.09.28-02"
TIKA = "com/dotcms/core/plugins/com.dotcms.tika-api"
FLAT = "com/dotcms/dotcms-core"
BUNDLES = "com/dotcms/plugins/dotcms-core-bundles"
PARENT = "com/dotcms/core/plugins/dotcms-core-plugins-parent"
PREFIX = "s3://test-bucket/libs-release/"

# Snapshot every cp's source bytes before the publisher deletes its temp dirs.
# The stub deliberately never imports an AWS SDK or makes network requests.
AWS_STUB = r'''
import base64
import fnmatch
import json
import os
from pathlib import Path
import sys

args = sys.argv[1:]
operation = args[args.index("s3") + 1:]
entry = {"args": args, "files": {}}
if operation[0] == "ls":
    if os.environ.get("STUB_FAIL_LIST") == "true":
        sys.exit(1)
    print("                           PRE 26.09.14-01/")
    print("                           PRE 26.09.28-02-java25/")
    print("2026-09-14 12:00:00 300 maven-metadata.xml")
elif operation[0] == "cp":
    source, dest = Path(operation[1]), operation[2]
    exclusions = [operation[i + 1] for i, arg in enumerate(operation) if arg == "--exclude"]
    if source.is_dir():
        for path in sorted(source.rglob("*")):
            if not path.is_file():
                continue
            rel = path.relative_to(source).as_posix()
            if not any(fnmatch.fnmatch(rel, pattern) for pattern in exclusions):
                entry["files"][dest.rstrip("/") + "/" + rel] = base64.b64encode(path.read_bytes()).decode()
    else:
        entry["files"][dest] = base64.b64encode(source.read_bytes()).decode()
else:
    raise RuntimeError("Unexpected AWS operation: " + repr(operation))
with open(os.environ["STUB_LOG"], "a") as log:
    log.write(json.dumps(entry) + "\n")
'''


class PublishTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="maven publisher tests ")
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.repo = self.root / "repository"
        self.bin = self.root / "bin"
        self.bin.mkdir()
        stub = self.bin / "aws"
        stub.write_text(f"#!{sys.executable}\n" + AWS_STUB)
        stub.chmod(0o755)
        self.log = self.root / "aws.jsonl"
        self.env = {key: value for key, value in os.environ.items()
                    if not key.startswith(("MAVEN_", "AWS_", "GITHUB_", "STUB_"))}
        self.env.update({
            "PATH": str(self.bin) + os.pathsep + os.environ["PATH"],
            "MAVEN_BUNNY_RW_USERNAME": "test-bucket",
            "MAVEN_BUNNY_RW_PASSWORD": "offline-test-only",
            "STUB_LOG": str(self.log),
        })
        self.install(FLAT)
        self.install(TIKA)
        self.install(BUNDLES, extension="zip")
        self.install(PARENT, extension=None)

    def install(self, artifact_path, version=VERSION, extension="jar"):
        directory = self.repo / artifact_path / version
        directory.mkdir(parents=True, exist_ok=True)
        artifact = artifact_path.rsplit("/", 1)[1]
        group = artifact_path.rsplit("/", 1)[0].replace("/", ".")
        (directory / f"{artifact}-{version}.pom").write_text(
            f"<project><groupId>{group}</groupId><artifactId>{artifact}</artifactId>"
            f"<version>{version}</version></project>"
        )
        if extension:
            (directory / f"{artifact}-{version}.{extension}").write_bytes(b"offline artifact fixture")
        return directory

    def publish(self, *options, version=VERSION, expected_code=0):
        result = subprocess.run(
            ["bash", str(SCRIPT), "maven", "--version", version,
             "--repo-dir", str(self.repo), *options],
            env=self.env, capture_output=True, text=True, timeout=30,
        )
        self.assertEqual(result.returncode, expected_code, result.stdout + result.stderr)
        self.calls = [json.loads(line) for line in self.log.read_text().splitlines()] if self.log.exists() else []
        self.uploads = {key: base64.b64decode(value) for call in self.calls
                        for key, value in call["files"].items()}
        return result

    def key(self, artifact_path, suffix, version=VERSION):
        artifact = artifact_path.rsplit("/", 1)[1]
        return f"{PREFIX}{artifact_path}/{version}/{artifact}-{version}.{suffix}"

    def test_flat_group_upload_is_preserved(self):
        self.publish()
        self.assertIn(self.key(FLAT, "jar"), self.uploads)
        self.assertIn(self.key(FLAT, "pom"), self.uploads)

    def test_nested_groups_upload_jars_and_poms_at_original_paths(self):
        self.publish()
        for artifact_path, suffixes in ((TIKA, ("jar", "pom")),
                                        (BUNDLES, ("zip", "pom")), (PARENT, ("pom",))):
            for suffix in suffixes:
                with self.subTest(artifact_path=artifact_path, suffix=suffix):
                    self.assertIn(self.key(artifact_path, suffix), self.uploads)

    def test_nested_metadata_has_correct_group_and_preserves_versions(self):
        self.publish()
        for artifact_path in (TIKA, BUNDLES, PARENT):
            key = f"{PREFIX}{artifact_path}/maven-metadata.xml"
            with self.subTest(artifact_path=artifact_path):
                self.assertIn(key, self.uploads)
                metadata = ET.fromstring(self.uploads[key])
                group, artifact = artifact_path.rsplit("/", 1)
                self.assertEqual(metadata.findtext("groupId"), group.replace("/", "."))
                self.assertEqual(metadata.findtext("artifactId"), artifact)
                self.assertEqual(metadata.findtext("versioning/latest"), VERSION)
                self.assertEqual(metadata.findtext("versioning/release"), VERSION)
                self.assertEqual([entry.text for entry in metadata.findall("versioning/versions/version")],
                                 ["26.09.14-01", VERSION, VERSION + "-java25"])
                self.assertNotIn("12:00:00", self.uploads[key].decode())

    def test_nested_artifact_and_metadata_checksums_match_uploaded_bytes(self):
        self.publish()
        keys = [self.key(TIKA, "jar"), self.key(TIKA, "pom"),
                self.key(BUNDLES, "zip"), self.key(PARENT, "pom")]
        keys.extend(f"{PREFIX}{path}/maven-metadata.xml" for path in (TIKA, BUNDLES, PARENT))
        for key in keys:
            self.assertIn(key, self.uploads)
            for suffix, digest in (("sha1", hashlib.sha1), ("md5", hashlib.md5)):
                self.assertEqual(self.uploads[key + "." + suffix].decode().strip(),
                                 digest(self.uploads[key]).hexdigest())

    def test_modules_filter_finds_nested_artifact_and_excludes_others(self):
        self.publish("--modules", "com.dotcms.tika-api")
        self.assertIn(self.key(TIKA, "jar"), self.uploads)
        self.assertNotIn(self.key(FLAT, "jar"), self.uploads)
        self.assertNotIn(self.key(BUNDLES, "zip"), self.uploads)
        self.assertNotIn(self.key(PARENT, "pom"), self.uploads)

    def test_same_artifact_id_in_different_groups_preserves_both_paths(self):
        other = "com/dotcms/other/plugins/com.dotcms.tika-api"
        self.install(other)
        self.publish("--modules", "com.dotcms.tika-api")
        self.assertIn(self.key(TIKA, "jar"), self.uploads)
        self.assertIn(self.key(other, "jar"), self.uploads)
        for artifact_path in (TIKA, other):
            key = f"{PREFIX}{artifact_path}/maven-metadata.xml"
            metadata = ET.fromstring(self.uploads[key])
            self.assertEqual(metadata.findtext("groupId"), artifact_path.rsplit("/", 1)[0].replace("/", "."))

    def test_other_versions_and_non_dotcms_groups_are_not_uploaded(self):
        old = self.install(TIKA, "26.09.14-01")
        self.install("com/example/example-plugin")
        self.publish()
        self.assertIn(self.key(TIKA, "jar"), self.uploads)
        self.assertFalse(any("com/example" in key for key in self.uploads))
        self.assertFalse(any("/26.09.14-01/" in key for key in self.uploads))
        self.assertTrue(old.is_dir())

    def test_flat_module_filter_still_works(self):
        self.publish("--modules", "dotcms-core")
        self.assertIn(self.key(FLAT, "jar"), self.uploads)
        self.assertNotIn(self.key(TIKA, "jar"), self.uploads)

    def test_missing_requested_module_is_an_error(self):
        self.publish("--modules", "missing-module", expected_code=1)
        self.assertEqual(self.calls, [])

    def test_local_repository_markers_and_custom_extensions_are_excluded(self):
        directory = self.repo / TIKA / VERSION
        excluded = ["_remote.repositories", "resolver-status.properties", "maven-metadata-local.xml",
                    "download.lastUpdated", "artifact.excludeext", "artifact.custom"]
        for name in excluded:
            (directory / name).write_text("local-only fixture")
        self.publish("--exclude-ext", "repositories,excludeext,custom")
        self.assertIn(self.key(TIKA, "jar"), self.uploads)
        for name in excluded:
            self.assertNotIn(f"{PREFIX}{TIKA}/{VERSION}/{name}", self.uploads)

    def test_snapshot_refusal_never_calls_aws(self):
        self.install(TIKA, "1.0.0-SNAPSHOT")
        self.publish(version="1.0.0-SNAPSHOT")
        self.assertEqual(self.calls, [])

    def test_dry_run_marks_every_upload_including_nested_artifacts(self):
        self.publish("--dry-run")
        self.assertIn(self.key(TIKA, "jar"), self.uploads)
        for call in self.calls:
            if "cp" in call["args"]:
                self.assertIn("--dryrun", call["args"])

    def test_listing_failure_does_not_overwrite_metadata(self):
        self.env["STUB_FAIL_LIST"] = "true"
        self.publish()
        self.assertIn(self.key(TIKA, "jar"), self.uploads)
        self.assertFalse(any("maven-metadata.xml" in key for key in self.uploads))


if __name__ == "__main__":
    unittest.main()
