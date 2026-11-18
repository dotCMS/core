"""A migrated custom field only renders natively in the new editor once its
`newRenderMode` field variable is "component" (the admin UI's "Recommended"
implementation). push must set it — via the same v3 field PUT the admin UI uses — on
every field whose code it migrates, keep every other field variable untouched, and never
flag a field whose dA asset hasn't actually been published."""

import json

import migrate_custom_fields as mcf
from conftest import parse_last_json


def variables_by_key(variables: list[dict]) -> dict:
    return {v["key"]: v for v in variables}


# ─── helpers ─────────────────────────────────────────────────────────────


def test_with_component_render_mode_adds_the_variable_and_keeps_the_others():
    field = {"id": "f1", "fieldVariables": [{"id": "v1", "key": "hideLabel", "value": "true", "fieldId": "f1"}]}

    updated = mcf.with_component_render_mode(field)

    by_key = variables_by_key(updated["fieldVariables"])
    assert by_key["newRenderMode"]["value"] == "component"
    assert by_key["newRenderMode"]["fieldId"] == "f1"
    assert by_key["hideLabel"] == {"id": "v1", "key": "hideLabel", "value": "true", "fieldId": "f1"}
    assert field["fieldVariables"] == [{"id": "v1", "key": "hideLabel", "value": "true", "fieldId": "f1"}], "input untouched"


def test_with_component_render_mode_replaces_iframe_and_keeps_the_variable_id():
    field = {"id": "f1", "fieldVariables": [{"id": "v9", "key": "newRenderMode", "value": "iframe", "fieldId": "f1"}]}

    updated = mcf.with_component_render_mode(field)

    assert [(v["id"], v["value"]) for v in updated["fieldVariables"]] == [("v9", "component")]


def test_has_component_render_mode():
    assert mcf.has_component_render_mode({"fieldVariables": [{"key": "newRenderMode", "value": "component"}]})
    assert not mcf.has_component_render_mode({"fieldVariables": [{"key": "newRenderMode", "value": "iframe"}]})
    assert not mcf.has_component_render_mode({"fieldVariables": []})
    assert not mcf.has_component_render_mode({})


# ─── inline field entries: code + flag in one v3 PUT ────────────────────────────────


def test_inline_field_push_sets_values_and_render_mode_in_one_v3_put(seeded_push_instance, workdir):
    _transport, _manifest, state = seeded_push_instance
    migrated = (workdir / "migrated" / "fields" / "Blog.author.vtl").read_text()

    mcf.push(workdir, dry_run=False, only=["Blog.author"])

    assert [field_id for field_id, _ in state.v3_puts] == ["field-blog-author"]
    sent = state.v3_puts[0][1]["field"]
    assert sent["values"] == migrated
    by_key = variables_by_key(sent["fieldVariables"])
    assert by_key["newRenderMode"]["value"] == "component"
    assert by_key["hideLabel"]["id"] == "var-hidelabel-field-blog-author", "other variables are sent back unchanged"


# ─── render-mode-only entries ─────────────────────────────────────────────


def test_render_mode_entry_is_flagged_once_its_asset_is_published(seeded_render_mode_instance, workdir):
    _transport, _manifest, state = seeded_render_mode_instance

    mcf.push(workdir, dry_run=False, only=[])

    manifest = json.loads((workdir / "manifest.json").read_text())
    statuses = {e["key"]: e["status"] for e in manifest["entries"]}
    assert statuses == {"asset-a": "published", "Blog.author": "published", "Blog.teaser": "published", "Page.done": "published"}

    teaser = variables_by_key(state.fields["field-blog-teaser"]["fieldVariables"])
    assert teaser["newRenderMode"]["value"] == "component"
    assert teaser["hideLabel"]["id"] == "var-hidelabel-field-blog-teaser"
    assert state.fields["field-blog-teaser"]["values"] == '#dotParse("/dA/asset-a")', "values untouched"
    assert variables_by_key(state.fields["field-page-done"]["fieldVariables"])["newRenderMode"]["value"] == "component"


def test_render_mode_entry_waits_while_its_asset_is_not_published(seeded_render_mode_instance, workdir, capsys):
    _transport, _manifest, state = seeded_render_mode_instance
    state.asset_inode = "inode-CHANGED-by-someone-else"  # asset-a will be skipped

    mcf.push(workdir, dry_run=False, only=[])

    manifest = json.loads((workdir / "manifest.json").read_text())
    entries = {e["key"]: e for e in manifest["entries"]}
    assert entries["asset-a"]["status"] == "skipped"
    assert entries["Blog.teaser"]["status"] == "pending", "must stay retryable, not become terminal"
    assert "field-blog-teaser" not in [field_id for field_id, _ in state.v3_puts], "legacy code must not be flagged"
    assert entries["Page.done"]["status"] == "published", "an entry without dependencies still goes through"

    result = parse_last_json(capsys.readouterr().out)
    assert result["counts"]["waiting"] == 1


def test_render_mode_waiting_entry_goes_through_on_a_later_push(seeded_render_mode_instance, workdir):
    _transport, _manifest, state = seeded_render_mode_instance
    state.asset_inode = "inode-CHANGED-by-someone-else"
    mcf.push(workdir, dry_run=False, only=[])

    # The customer re-pulls/resolves the conflict and publishes asset-a explicitly.
    state.asset_inode = "inode-shared-001"
    mcf.push(workdir, dry_run=False, only=["asset-a"])
    mcf.push(workdir, dry_run=False, only=[])

    manifest = json.loads((workdir / "manifest.json").read_text())
    assert {e["key"]: e["status"] for e in manifest["entries"]}["Blog.teaser"] == "published"


def test_dry_run_flags_nothing_but_previews_every_render_mode_entry(seeded_render_mode_instance, workdir, capsys):
    transport, _manifest, state = seeded_render_mode_instance

    mcf.push(workdir, dry_run=True, only=[])

    assert transport.requests_with_method("PUT", "POST", "DELETE") == []
    outcomes = {e["key"]: e["outcome"] for e in parse_last_json(capsys.readouterr().out)["entries"]}
    assert outcomes["Blog.teaser"].startswith("dry-run:"), "its asset would be published in this same run"
    assert outcomes["Page.done"].startswith("dry-run:")


def test_render_mode_entry_already_component_on_the_server_is_skipped_without_a_put(seeded_render_mode_instance, workdir):
    _transport, _manifest, state = seeded_render_mode_instance
    state.fields["field-page-done"]["fieldVariables"] = [
        {"id": "v-existing", "key": "newRenderMode", "value": "component", "fieldId": "field-page-done"}
    ]

    mcf.push(workdir, dry_run=False, only=["Page.done"])

    assert state.v3_puts == []
    manifest = json.loads((workdir / "manifest.json").read_text())
    assert {e["key"]: e["status"] for e in manifest["entries"]}["Page.done"] == "skipped"


def test_rerunning_push_never_reflags(seeded_render_mode_instance, workdir):
    _transport, _manifest, state = seeded_render_mode_instance

    mcf.push(workdir, dry_run=False, only=[])
    puts_after_first_run = len(state.v3_puts)
    mcf.push(workdir, dry_run=False, only=[])

    assert len(state.v3_puts) == puts_after_first_run


def test_render_mode_put_failure_is_recorded_as_failed(seeded_render_mode_instance, workdir):
    _transport, _manifest, state = seeded_render_mode_instance
    state.field_update_status = 500

    mcf.push(workdir, dry_run=False, only=["Page.done"])

    manifest = json.loads((workdir / "manifest.json").read_text())
    assert {e["key"]: e["status"] for e in manifest["entries"]}["Page.done"] == "failed"
