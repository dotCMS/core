"""pull must queue a "renderMode" entry for every custom field that needs its
`newRenderMode` field variable switched to "component": owners of a dA asset that is
being migrated (dependent on that asset), and fields whose code is already migrated but
still render in an iframe. Fields already in component mode, and inline legacy fields
(push flags those in the same PUT that updates their code), get no such entry."""

import json

import migrate_custom_fields as mcf
from conftest import parse_last_json


def render_mode_entries(manifest: dict) -> dict:
    return {e["key"]: e for e in manifest["entries"] if e["kind"] == "renderMode"}


def test_pull_queues_a_render_mode_entry_for_every_field_that_needs_the_flag(render_mode_pull_instance, workdir):
    mcf.pull(workdir)

    manifest = json.loads((workdir / "manifest.json").read_text())
    entries = render_mode_entries(manifest)
    assert set(entries) == {"Legacy.code", "Done.inlineDone", "Shared.a", "Shared.c"}
    assert entries["Legacy.code"]["requires"] == ["asset-legacy"], "only after its asset is published"
    assert entries["Done.inlineDone"]["requires"] == []
    assert entries["Shared.a"]["requires"] == [], "the shared asset's code is already migrated"
    assert entries["Shared.c"]["requires"] == [], "an explicit iframe must become component too"


def test_render_mode_entries_carry_the_field_ids_and_no_file(render_mode_pull_instance, workdir):
    mcf.pull(workdir)

    entry = render_mode_entries(json.loads((workdir / "manifest.json").read_text()))["Legacy.code"]
    assert (entry["typeId"], entry["fieldId"]) == ("ct-legacy", "f-legacy-code")
    assert entry["usedBy"] == ["Legacy.code"]
    assert entry["status"] == "pending"
    assert "file" not in entry, "nothing to migrate locally — only a flag to set"


def test_inline_legacy_fields_get_a_field_entry_not_a_render_mode_entry(render_mode_pull_instance, workdir):
    mcf.pull(workdir)

    manifest = json.loads((workdir / "manifest.json").read_text())
    kinds = [(e["key"], e["kind"]) for e in manifest["entries"] if e["key"] == "Done.legacyInline"]
    assert kinds == [("Done.legacyInline", "field")]


def test_render_mode_entries_come_after_the_assets_they_depend_on(render_mode_pull_instance, workdir):
    mcf.pull(workdir)

    keys = [e["key"] for e in json.loads((workdir / "manifest.json").read_text())["entries"]]
    assert keys.index("asset-legacy") < keys.index("Legacy.code")


def test_pull_counts_render_mode_entries(render_mode_pull_instance, workdir, capsys):
    mcf.pull(workdir)

    counts = parse_last_json(capsys.readouterr().out)["counts"]
    assert counts["renderMode"] == 4
