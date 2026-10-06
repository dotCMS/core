"""FR-006 / FR-008 / spec.md US2 acceptance scenario 1: `push --dry-run` describes every
prospective change but issues zero mutating requests, for both an asset entry and a
field entry, and never changes any entry's status in the manifest."""

import json

import migrate_custom_fields as mcf
from conftest import parse_last_json


def test_dry_run_issues_no_mutating_requests(seeded_push_instance, workdir, capsys):
    transport, _manifest, _state = seeded_push_instance

    mcf.push(workdir, dry_run=True, only=[])

    mutating = transport.requests_with_method("PUT", "POST", "DELETE")
    assert mutating == [], f"dry-run must never mutate the server, got: {mutating}"


def test_dry_run_describes_every_pending_entry(seeded_push_instance, workdir, capsys):
    transport, _manifest, _state = seeded_push_instance

    mcf.push(workdir, dry_run=True, only=[])

    result = parse_last_json(capsys.readouterr().out)
    described_keys = {entry["key"] for entry in result["entries"]}
    assert described_keys == {"asset-a", "Blog.author"}


def test_dry_run_leaves_manifest_status_untouched(seeded_push_instance, workdir):
    mcf.push(workdir, dry_run=True, only=[])

    manifest = json.loads((workdir / "manifest.json").read_text())
    for entry in manifest["entries"]:
        assert entry["status"] == "pending", "a preview must never change persisted state"
