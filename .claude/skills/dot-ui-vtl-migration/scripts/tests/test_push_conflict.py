"""FR-009 / FR-011 / spec.md US3 acceptance scenarios 1 & 3: an entry that changed on
the server since `pull` is skipped and reported, not overwritten, and the rest of the
batch still proceeds independently."""

import json

import migrate_custom_fields as mcf
from conftest import parse_last_json


def test_asset_conflict_is_skipped_not_overwritten(seeded_push_instance, workdir):
    _transport, _manifest, state = seeded_push_instance
    state.asset_inode = "inode-CHANGED-by-someone-else"

    mcf.push(workdir, dry_run=False, only=[])

    assert state.publish_calls == 0, "must never publish over a concurrent server-side change"
    manifest = json.loads((workdir / "manifest.json").read_text())
    entry = next(e for e in manifest["entries"] if e["key"] == "asset-a")
    assert entry["status"] == "skipped"
    assert "changed on the server since pull" in entry["statusReason"]


def test_field_conflict_is_skipped_not_overwritten(seeded_push_instance, workdir):
    _transport, _manifest, state = seeded_push_instance
    state.field_values = "<script>SOMEONE ELSE EDITED THIS CONCURRENTLY</script>"

    mcf.push(workdir, dry_run=False, only=[])

    assert state.field_update_calls == 0, "must never publish over a concurrent server-side change"
    manifest = json.loads((workdir / "manifest.json").read_text())
    entry = next(e for e in manifest["entries"] if e["key"] == "Blog.author")
    assert entry["status"] == "skipped"
    assert "changed on the server since pull" in entry["statusReason"]


def test_one_conflicting_entry_does_not_block_the_rest_of_the_batch(seeded_push_instance, workdir, capsys):
    _transport, _manifest, state = seeded_push_instance
    state.asset_inode = "inode-CHANGED-by-someone-else"

    mcf.push(workdir, dry_run=False, only=[])

    manifest = json.loads((workdir / "manifest.json").read_text())
    entries_by_key = {entry["key"]: entry for entry in manifest["entries"]}
    assert entries_by_key["asset-a"]["status"] == "skipped"
    assert entries_by_key["Blog.author"]["status"] == "published", "the healthy entry must still go through"

    result = parse_last_json(capsys.readouterr().out)
    assert result["counts"] == {"published": 1, "skipped": 1, "failed": 0}


def test_asset_conflict_check_request_failure_is_failed_not_skipped(seeded_push_instance, workdir):
    """A transient 5xx on the conflict-check GET is not the same as 'this genuinely
    changed on the server' — it must be reported as a failure, not a skip, or the
    customer is wrongly told to just re-run pull."""
    _transport, _manifest, state = seeded_push_instance
    state.content_get_status = 500

    mcf.push(workdir, dry_run=False, only=["asset-a"])

    assert state.publish_calls == 0
    manifest = json.loads((workdir / "manifest.json").read_text())
    entry = next(e for e in manifest["entries"] if e["key"] == "asset-a")
    assert entry["status"] == "failed"
    assert "changed on the server" not in (entry["statusReason"] or "")


def test_field_conflict_check_request_failure_is_failed_not_skipped(seeded_push_instance, workdir):
    _transport, _manifest, state = seeded_push_instance
    state.field_get_status = 500

    mcf.push(workdir, dry_run=False, only=["Blog.author"])

    assert state.field_update_calls == 0
    manifest = json.loads((workdir / "manifest.json").read_text())
    entry = next(e for e in manifest["entries"] if e["key"] == "Blog.author")
    assert entry["status"] == "failed"
    assert "changed on the server" not in (entry["statusReason"] or "")
