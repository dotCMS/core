"""PR #37768 review finding: re-running `push` against terminal manifest entries must
never reprocess them — a published asset would otherwise be republished identically
(its inode was updated to the new value on the prior run, so the conflict check
passes again), and a published field would be wrongly re-reported as "skipped:
changed on the server" (since the server now holds the migrated code)."""

import json

import migrate_custom_fields as mcf
from conftest import parse_last_json


def test_running_push_twice_does_not_reprocess_terminal_entries(seeded_push_instance, workdir, capsys):
    _transport, _manifest, state = seeded_push_instance

    mcf.push(workdir, dry_run=False, only=[])
    assert state.publish_calls == 1
    assert state.field_update_calls == 1

    capsys.readouterr()  # discard first run's output
    mcf.push(workdir, dry_run=False, only=[])

    assert state.publish_calls == 1, "a terminal asset entry must not be republished"
    assert state.field_update_calls == 1, "a terminal field entry must not be re-updated"

    result = parse_last_json(capsys.readouterr().out)
    assert result["counts"] == {"published": 0, "skipped": 0, "failed": 0}
    assert result["entries"] == []

    manifest = json.loads((workdir / "manifest.json").read_text())
    assert {e["key"]: e["status"] for e in manifest["entries"]} == {
        "asset-a": "published",
        "Blog.author": "published",
    }


def test_only_still_forces_reprocessing_a_terminal_entry(seeded_push_instance, workdir):
    _transport, _manifest, state = seeded_push_instance

    mcf.push(workdir, dry_run=False, only=[])
    assert state.publish_calls == 1

    # An explicit --only is treated as "the customer wants this one retried."
    mcf.push(workdir, dry_run=False, only=["asset-a"])

    assert state.publish_calls == 2, "--only must bypass the terminal-status guard"
