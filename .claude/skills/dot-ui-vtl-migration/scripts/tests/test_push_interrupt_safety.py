"""Edge Cases / research.md's incremental-persistence decision: `push` must write
manifest.json back to disk after EVERY entry's outcome, not only once at the end of the
batch, so a mid-batch crash still leaves an accurate record of what happened so far."""

import json

import pytest

import migrate_custom_fields as mcf


def test_manifest_reflects_partial_progress_after_a_mid_batch_crash(seeded_push_instance, workdir, monkeypatch):
    _transport, _manifest, _state = seeded_push_instance

    def exploding_push_field(*args, **kwargs):
        raise RuntimeError("simulated crash mid-batch")

    # raising=False: keeps this test robust to push_field being refactored away as its
    # own patchable module attribute in the future (e.g. inlined into push()) — it fails
    # as a clean "push never actually raised/crashed" assertion in that case, not as a
    # setup AttributeError.
    monkeypatch.setattr(mcf, "push_field", exploding_push_field, raising=False)

    # Manifest entries are processed in order: "asset-a" (asset) then "Blog.author"
    # (field) — the field entry is where the simulated crash happens.
    with pytest.raises(RuntimeError):
        mcf.push(workdir, dry_run=False, only=[])

    manifest = json.loads((workdir / "manifest.json").read_text())
    entries_by_key = {entry["key"]: entry for entry in manifest["entries"]}

    assert entries_by_key["asset-a"]["status"] == "published", (
        "the entry processed before the crash must have been persisted already, "
        "not lost because the batch never reached its end-of-run write"
    )
    assert entries_by_key["Blog.author"]["status"] == "pending", (
        "the entry that crashed never got a chance to update its own status, "
        "and must not have been marked as anything else by the crash"
    )
