"""FR-010 / spec.md US3 acceptance scenario 2: a migrated file missing the
isNewEditModeEnabled() branch, or one that doesn't preserve the original legacy code
verbatim, is skipped and reported instead of published."""

import json

import migrate_custom_fields as mcf


def test_missing_edit_mode_marker_is_skipped(seeded_push_instance, workdir):
    _transport, _manifest, state = seeded_push_instance
    (workdir / "migrated" / "assets" / "asset-a__userID.vtl").write_text("<p>no branch here, oops</p>")

    mcf.push(workdir, dry_run=False, only=["asset-a"])

    assert state.publish_calls == 0
    manifest = json.loads((workdir / "manifest.json").read_text())
    entry = next(e for e in manifest["entries"] if e["key"] == "asset-a")
    assert entry["status"] == "skipped"
    assert "isNewEditModeEnabled" in entry["statusReason"]


def test_legacy_code_not_preserved_verbatim_is_skipped(seeded_push_instance, workdir):
    _transport, _manifest, state = seeded_push_instance
    (workdir / "migrated" / "fields" / "Blog.author.vtl").write_text(
        "#if( $structures.isNewEditModeEnabled() )\n<p>migrated</p>\n#else\n<p>NOT the original code</p>\n#end"
    )

    mcf.push(workdir, dry_run=False, only=["Blog.author"])

    assert state.field_update_calls == 0
    manifest = json.loads((workdir / "manifest.json").read_text())
    entry = next(e for e in manifest["entries"] if e["key"] == "Blog.author")
    assert entry["status"] == "skipped"
    assert "verbatim" in entry["statusReason"]


def test_legacy_code_hidden_in_if_branch_is_skipped(seeded_push_instance, workdir):
    """Regression for the reviewer-flagged gap: the original bytes are present in the
    file (inside a comment in the #if branch) but NOT in the #else branch — a
    substring-anywhere check would wrongly publish this; it must be skipped."""
    _transport, _manifest, state = seeded_push_instance
    original = (workdir / "original" / "fields" / "Blog.author.vtl").read_bytes()
    (workdir / "migrated" / "fields" / "Blog.author.vtl").write_bytes(
        b"#if( $structures.isNewEditModeEnabled() )\n"
        b"<!-- kept for reference: " + original + b" -->\n"
        b"<p>migrated</p>\n"
        b"#else\n<p>NOT the original code</p>\n#end"
    )

    mcf.push(workdir, dry_run=False, only=["Blog.author"])

    assert state.field_update_calls == 0
    manifest = json.loads((workdir / "manifest.json").read_text())
    entry = next(e for e in manifest["entries"] if e["key"] == "Blog.author")
    assert entry["status"] == "skipped"
    assert "verbatim" in entry["statusReason"]
