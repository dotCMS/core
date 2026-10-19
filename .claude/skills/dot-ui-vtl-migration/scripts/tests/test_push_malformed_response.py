"""PR #37768 review finding: an unguarded `response.json()["entity"]["inode"]` after a
successful PUBLISH request means a non-JSON/malformed response body crashes the whole
batch — worse, this happens *after* the asset was already published server-side, so
the manifest never records that it actually succeeded."""

import json

import migrate_custom_fields as mcf
from conftest import parse_last_json


def test_malformed_publish_response_is_recorded_as_failed_not_crashed(seeded_push_instance, workdir, capsys):
    _transport, _manifest, state = seeded_push_instance
    state.publish_response_body = b"not valid json at all"

    mcf.push(workdir, dry_run=False, only=["asset-a"])

    result = parse_last_json(capsys.readouterr().out)
    assert result["counts"]["failed"] == 1
    assert result["counts"]["published"] == 0

    manifest = json.loads((workdir / "manifest.json").read_text())
    entry = next(e for e in manifest["entries"] if e["key"] == "asset-a")
    assert entry["status"] == "failed"


def test_malformed_publish_response_does_not_block_the_rest_of_the_batch(seeded_push_instance, workdir):
    _transport, _manifest, state = seeded_push_instance
    state.publish_response_body = b"not valid json at all"

    mcf.push(workdir, dry_run=False, only=[])

    manifest = json.loads((workdir / "manifest.json").read_text())
    entries_by_key = {e["key"]: e for e in manifest["entries"]}
    assert entries_by_key["asset-a"]["status"] == "failed"
    assert entries_by_key["Blog.author"]["status"] == "published", "the healthy entry must still go through"
