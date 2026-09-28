"""FR-007 / spec.md US2 acceptance scenario 2: `push --only KEY` restricts processing to
the matching entries, leaving every other manifest entry untouched."""

import json

import migrate_custom_fields as mcf
from conftest import parse_last_json


def test_only_by_key_considers_just_the_matching_entry(seeded_push_instance, workdir, capsys):
    mcf.push(workdir, dry_run=True, only=["asset-a"])

    result = parse_last_json(capsys.readouterr().out)
    considered_keys = {entry["key"] for entry in result["entries"]}
    assert considered_keys == {"asset-a"}


def test_only_by_identifier_also_matches_asset_entries(seeded_push_instance, workdir, capsys):
    mcf.push(workdir, dry_run=True, only=["ident-shared-001"])

    result = parse_last_json(capsys.readouterr().out)
    considered_keys = {entry["key"] for entry in result["entries"]}
    assert considered_keys == {"asset-a"}


def test_only_leaves_unmatched_entries_pending(seeded_push_instance, workdir):
    mcf.push(workdir, dry_run=True, only=["asset-a"])

    manifest = json.loads((workdir / "manifest.json").read_text())
    entries_by_key = {entry["key"]: entry for entry in manifest["entries"]}
    assert entries_by_key["Blog.author"]["status"] == "pending"
