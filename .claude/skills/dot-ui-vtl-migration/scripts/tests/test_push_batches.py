"""The skill migrates an instance in batches the customer picks: after `pull`, only the
selected files get migrated and pushed (`push --only <keys>`). Everything else must stay
pending for a later batch — never be closed out as skipped just because its migrated file
doesn't exist yet — and selecting an asset must also switch the fields that load it to the
component render mode in the same run."""

import json

import migrate_custom_fields as mcf
from conftest import parse_last_json


def statuses(workdir) -> dict:
    return {e["key"]: e["status"] for e in json.loads((workdir / "manifest.json").read_text())["entries"]}


def test_entry_without_a_migrated_file_stays_pending_for_a_later_batch(seeded_push_instance, workdir, capsys):
    _transport, _manifest, state = seeded_push_instance
    (workdir / "migrated" / "fields" / "Blog.author.vtl").unlink()  # not migrated in this batch

    mcf.push(workdir, dry_run=False, only=[])

    assert statuses(workdir) == {"asset-a": "published", "Blog.author": "pending"}
    assert parse_last_json(capsys.readouterr().out)["counts"]["waiting"] == 1


def test_a_later_batch_publishes_what_was_left_pending(seeded_push_instance, workdir):
    _transport, _manifest, state = seeded_push_instance
    author = workdir / "migrated" / "fields" / "Blog.author.vtl"
    migrated = author.read_text()
    author.unlink()
    mcf.push(workdir, dry_run=False, only=[])

    author.write_text(migrated)  # batch 2: the agent migrates it now
    mcf.push(workdir, dry_run=False, only=[])

    assert statuses(workdir)["Blog.author"] == "published"


def test_only_an_asset_also_switches_the_fields_that_load_it(seeded_render_mode_instance, workdir):
    _transport, _manifest, state = seeded_render_mode_instance

    mcf.push(workdir, dry_run=False, only=["asset-a"])

    assert statuses(workdir) == {
        "asset-a": "published",
        "Blog.teaser": "published",  # depends on asset-a: pulled into the same run
        "Blog.author": "pending",  # not selected
        "Page.done": "pending",  # not selected, no dependency on asset-a
    }


def test_only_an_asset_does_not_reprocess_a_dependent_that_already_finished(seeded_render_mode_instance, workdir):
    _transport, _manifest, state = seeded_render_mode_instance
    mcf.push(workdir, dry_run=False, only=["asset-a"])
    puts = len(state.v3_puts)

    mcf.push(workdir, dry_run=False, only=["asset-a"])  # the asset is retried explicitly...

    assert statuses(workdir)["Blog.teaser"] == "published"  # ...its finished dependent is left alone
    assert len(state.v3_puts) == puts
