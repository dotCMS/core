"""PR #37768 review question (adrianjm-dotCMS): a `#dotParse("/dA/<id>")` whose <id> is a
pinned inode rather than the identifier can never be safely republished — push's conflict
check compares against the identifier's *current* inode (always "changed"), and PUBLISH
always lands on the identifier's latest version anyway. pull must report it as out of
scope instead of queuing a migration that can never complete."""

import json

import migrate_custom_fields as mcf
from conftest import parse_last_json


def test_pinned_inode_reference_is_reported_not_queued(pinned_asset_instance, workdir, capsys):
    mcf.pull(workdir)

    manifest = json.loads((workdir / "manifest.json").read_text())
    assert manifest["entries"] == [], "a pinned-version reference must not become a pending entry"

    captured = capsys.readouterr()
    assert "inode-pinned-001" in captured.err
    assert "ident-real-001" in captured.err

    result = parse_last_json(captured.out)
    assert result["counts"]["pinnedVersions"] == 1
    assert result["counts"]["failed"] == 0, "out of scope is not a failure"


def test_pinned_inode_reference_is_never_downloaded(pinned_asset_instance, workdir):
    transport = pinned_asset_instance
    mcf.pull(workdir)

    assert all("/asset/pinned.vtl" not in r.url.path for r in transport.requests)
