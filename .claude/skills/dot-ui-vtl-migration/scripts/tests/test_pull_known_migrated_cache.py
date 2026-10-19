"""FR-013 / PR #37768 review finding: a re-pull after a successful push must recognize the
published asset as already migrated WITHOUT re-downloading its binary — using only the
cheap metadata GET plus the inode recorded in the previous manifest — and must fall back
to a real download whenever the server's inode no longer matches that record."""

import json

import migrate_custom_fields as mcf
from conftest import WIDGET_DOWNLOAD_PATH, write_inline_migration


def pull_then_push(workdir, state):
    mcf.pull(workdir)
    manifest = json.loads((workdir / "manifest.json").read_text())
    entry = next(e for e in manifest["entries"] if e["key"] == "asset-w1")
    write_inline_migration(workdir, entry)
    mcf.push(workdir, dry_run=False, only=[])
    assert state.publish_calls == 1
    assert state.download_calls == 1


def test_repull_after_push_skips_the_binary_download(reversionable_asset_instance, workdir):
    transport, state = reversionable_asset_instance
    pull_then_push(workdir, state)

    snapshot = len(transport.requests)
    mcf.pull(workdir)
    new_paths = [r.url.path for r in transport.requests[snapshot:]]

    assert WIDGET_DOWNLOAD_PATH not in new_paths, "the binary must not be re-downloaded"
    assert "/api/v1/content/asset-w1" in new_paths, "the cheap metadata GET still happens"

    manifest = json.loads((workdir / "manifest.json").read_text())
    assert manifest["entries"] == []
    assert manifest["knownMigrated"] == {"asset-w1": state.migrated_inode}


def test_repull_redownloads_when_the_server_inode_changed(reversionable_asset_instance, workdir):
    transport, state = reversionable_asset_instance
    pull_then_push(workdir, state)

    state.inode = "inode-w1-v1"  # someone reverted the asset outside this tool
    mcf.pull(workdir)

    assert state.download_calls == 2, "a stale record must never be trusted blindly"
    manifest = json.loads((workdir / "manifest.json").read_text())
    assert [e["key"] for e in manifest["entries"]] == ["asset-w1"]
    assert manifest["entries"][0]["status"] == "pending"
    assert manifest["knownMigrated"] == {}


def test_corrupt_previous_manifest_just_disables_the_skip(reversionable_asset_instance, workdir):
    _transport, state = reversionable_asset_instance
    pull_then_push(workdir, state)

    (workdir / "manifest.json").write_text("{corrupt")
    mcf.pull(workdir)

    assert state.download_calls == 2, "an unreadable previous manifest means: re-check everything"
    manifest = json.loads((workdir / "manifest.json").read_text())
    assert manifest["entries"] == []  # still recognized as migrated — via the real marker check
    assert manifest["knownMigrated"] == {"asset-w1": state.migrated_inode}
