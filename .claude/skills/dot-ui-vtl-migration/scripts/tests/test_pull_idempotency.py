"""FR-013 / spec.md US1 acceptance scenario 4: an already-migrated dA file or in-field
VTL is reported as already migrated, never downloaded/queued, and running `pull` again
does not produce a duplicate or new manifest entry for it."""

import json

import migrate_custom_fields as mcf


def test_already_migrated_asset_and_field_produce_no_manifest_entries(already_migrated_instance, workdir):
    mcf.pull(workdir)

    manifest = json.loads((workdir / "manifest.json").read_text())
    assert manifest["entries"] == [], "nothing here needs migrating"


def test_already_migrated_asset_is_not_downloaded_to_original(already_migrated_instance, workdir):
    mcf.pull(workdir)

    assert list((workdir / "original" / "assets").glob("*")) == [], "no asset file should be written"


def test_running_pull_twice_stays_stable(already_migrated_instance, workdir):
    mcf.pull(workdir)
    first = json.loads((workdir / "manifest.json").read_text())

    mcf.pull(workdir)
    second = json.loads((workdir / "manifest.json").read_text())

    assert first["entries"] == second["entries"] == []
