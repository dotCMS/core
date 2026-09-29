"""FR-002: pull must classify every custom field into exactly one of 5 categories.

Uses the `full_instance` fixture (conftest.py): Blog.author + Event.organizer share one
downloadable dA file; Banner.widget has in-field legacy VTL; Page has one core-file field,
one non-/dA #dotParse field, one clean inline field, and one already-migrated field.
"""

import json

import migrate_custom_fields as mcf


def test_pull_classifies_every_custom_field_into_one_of_five_categories(full_instance, workdir):
    mcf.pull(workdir)

    manifest = json.loads((workdir / "manifest.json").read_text())
    entries_by_key = {entry["key"]: entry for entry in manifest["entries"]}

    # Category (a): downloadable dA file -> one manifest entry, shared by both fields.
    assert "asset-shared-001" in entries_by_key
    assert entries_by_key["asset-shared-001"]["kind"] == "asset"

    # Category (b): in-field legacy VTL -> its own manifest entry.
    assert "Banner.widget" in entries_by_key
    assert entries_by_key["Banner.widget"]["kind"] == "field"

    # Categories (c) core file, (d) non-/dA #dotParse, and (e) clean inline VTL are
    # report-only: never downloaded, never queued.
    for report_only_key in ("Page.coreWidget", "Page.otherPath", "Page.cleanInline"):
        assert report_only_key not in entries_by_key, f"{report_only_key} must not produce a manifest entry"

    migration_entries = [e["key"] for e in manifest["entries"] if e["kind"] != "renderMode"]
    assert sorted(migration_entries) == ["Banner.widget", "asset-shared-001"], "no other code to migrate"

    # Already-migrated inline code needs no migration, only the component render-mode flag;
    # the shared asset's two owners get the flag once that asset is published.
    render_mode = {e["key"]: e["requires"] for e in manifest["entries"] if e["kind"] == "renderMode"}
    assert render_mode == {
        "Blog.author": ["asset-shared-001"],
        "Event.organizer": ["asset-shared-001"],
        "Page.alreadyMigrated": [],
    }


def test_pull_writes_a_pending_status_for_every_new_entry(full_instance, workdir):
    mcf.pull(workdir)

    manifest = json.loads((workdir / "manifest.json").read_text())
    for entry in manifest["entries"]:
        assert entry["status"] == "pending"
        assert entry["statusReason"] is None
