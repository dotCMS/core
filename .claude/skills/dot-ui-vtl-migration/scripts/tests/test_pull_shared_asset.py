"""FR-003 / spec.md US1 acceptance scenario 1: a downloadable file referenced by several
content-type fields is fetched once, and every owning field is recorded against the same
manifest entry — not duplicated per field."""

import json

import migrate_custom_fields as mcf


def test_shared_asset_is_resolved_and_downloaded_exactly_once(full_instance, workdir):
    transport, log = full_instance

    mcf.pull(workdir)

    assert log.asset_resolve_calls == ["asset-shared-001"], "resolve must happen exactly once"
    assert log.asset_download_calls == ["asset-shared-001"], "download must happen exactly once"


def test_shared_asset_entry_lists_every_owning_field(full_instance, workdir):
    mcf.pull(workdir)

    manifest = json.loads((workdir / "manifest.json").read_text())
    entries_by_key = {entry["key"]: entry for entry in manifest["entries"]}
    shared_entry = entries_by_key["asset-shared-001"]

    assert sorted(shared_entry["usedBy"]) == ["Blog.author", "Event.organizer"]
    assert len([e for e in manifest["entries"] if e["kind"] == "asset"]) == 1, "no duplicate entry per field"


def test_shared_asset_file_is_written_once_under_original(full_instance, workdir):
    mcf.pull(workdir)

    manifest = json.loads((workdir / "manifest.json").read_text())
    entries_by_key = {entry["key"]: entry for entry in manifest["entries"]}
    shared_entry = entries_by_key["asset-shared-001"]

    downloaded = workdir / "original" / shared_entry["file"]
    assert downloaded.exists()
    assert "dojo.ready" in downloaded.read_text()
