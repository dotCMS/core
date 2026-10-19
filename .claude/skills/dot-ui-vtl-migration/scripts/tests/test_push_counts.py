"""The final CLI Result's `counts` for `push` must break down published/skipped/failed
(and dryRun, for a preview run) — not just an opaque "considered" total — matching
data-model.md's CLI Result contract."""

import migrate_custom_fields as mcf
from conftest import parse_last_json


def test_clean_run_counts_every_entry_as_published(seeded_push_instance, workdir, capsys):
    mcf.push(workdir, dry_run=False, only=[])

    result = parse_last_json(capsys.readouterr().out)
    assert result["counts"] == {"published": 2, "skipped": 0, "failed": 0}


def test_a_failed_publish_is_counted_as_failed_not_just_considered(seeded_push_instance, workdir, capsys):
    _transport, _manifest, state = seeded_push_instance
    state.publish_status = 500

    mcf.push(workdir, dry_run=False, only=[])

    result = parse_last_json(capsys.readouterr().out)
    assert result["counts"] == {"published": 1, "skipped": 0, "failed": 1}


def test_dry_run_counts_are_reported_under_dry_run_not_published_or_skipped(seeded_push_instance, workdir, capsys):
    mcf.push(workdir, dry_run=True, only=[])

    result = parse_last_json(capsys.readouterr().out)
    assert result["counts"] == {"published": 0, "skipped": 0, "failed": 0, "dryRun": 2}
