"""Tests for the CLI layer (cli.py).

All registry I/O is patched out via unittest.mock so tests run fully offline.
"""
from __future__ import annotations

import datetime as dt
from unittest.mock import MagicMock, patch

import pytest

from evergreen_tracks.cli import (
    _current_version,
    build_parser,
    cmd_admin,
    cmd_promote,
    main,
)
from evergreen_tracks.registry import Tag


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _make_tag(name: str, digest: str = "sha256:abcdef1234567890") -> Tag:
    return Tag(name=name, digest=digest)


def _tags_for_promote():
    """A minimal set of registry tags: two GA versions, no markers, no track tags."""
    return [
        _make_tag("26.06.02-01", "sha256:aaa"),
        _make_tag("26.06.16-01", "sha256:bbb"),
    ]


def _tags_for_hold():
    """Two GA versions where 26.06.02-01 already has a taint marker."""
    return [
        _make_tag("26.06.02-01", "sha256:aaa"),
        _make_tag("26.06.16-01", "sha256:bbb"),
        _make_tag("26.06.02-01_tainted", "sha256:aaa"),
    ]


# ---------------------------------------------------------------------------
# build_parser: --apply lives on the subcommands, not the top-level parser
# ---------------------------------------------------------------------------

def test_promote_apply_parses_correctly():
    """promote --apply must parse without error (blocking issue: --apply on subparser)."""
    args = build_parser().parse_args(["promote", "--repo", "dotcms/dotcms-test", "--apply"])
    assert args.apply is True


def test_promote_apply_defaults_to_false():
    args = build_parser().parse_args(["promote", "--repo", "dotcms/dotcms-test"])
    assert args.apply is False


def test_admin_apply_parses_correctly():
    args = build_parser().parse_args(
        ["admin", "--repo", "dotcms/dotcms-test", "--action", "taint",
         "--version", "26.06.02-01", "--apply"]
    )
    assert args.apply is True


def test_promote_apply_before_subcommand_is_unrecognized():
    """--apply before the subcommand is not valid (it lives on the subparser)."""
    with pytest.raises(SystemExit) as exc_info:
        build_parser().parse_args(["--apply", "promote", "--repo", "foo/bar"])
    assert exc_info.value.code == 2


def test_promote_threshold_defaults():
    args = build_parser().parse_args(["promote", "--repo", "dotcms/dotcms-test"])
    assert args.latest_days == 0
    assert args.standard_days == 14
    assert args.trailing_days == 28


def test_admin_force_default_false():
    args = build_parser().parse_args(
        ["admin", "--repo", "r", "--action", "taint", "--version", "26.06.02-01"]
    )
    assert args.force is False


# ---------------------------------------------------------------------------
# cmd_promote
# ---------------------------------------------------------------------------

@patch("evergreen_tracks.cli.point_tag")
@patch("evergreen_tracks.cli.list_tags")
def test_promote_dry_run_returns_zero(mock_list_tags, mock_point_tag):
    """Dry-run promote must return 0 and call point_tag with apply=False (dry-run)."""
    mock_list_tags.return_value = _tags_for_promote()
    args = build_parser().parse_args(
        ["promote", "--repo", "dotcms/dotcms-test",
         "--latest-days", "0", "--standard-days", "14", "--trailing-days", "28"]
    )
    # Fix today so the test is deterministic: use a date where 26.06.16-01 is ≥14 days old.
    with patch("evergreen_tracks.cli.dt") as mock_dt:
        mock_dt.date.today.return_value = dt.date(2026, 7, 15)
        rc = cmd_promote(args)
    assert rc == 0
    # In dry-run (apply=False), point_tag is called but with apply=False (no actual mutation).
    for call in mock_point_tag.call_args_list:
        assert call.kwargs.get("apply") is False or call[1].get("apply") is False


@patch("evergreen_tracks.cli.point_tag")
@patch("evergreen_tracks.cli.list_tags")
def test_promote_no_moves_when_no_releases(mock_list_tags, mock_point_tag):
    """When there are no GA releases, plan produces no moves and we return 0."""
    mock_list_tags.return_value = [_make_tag("latest", "sha256:aaa")]
    args = build_parser().parse_args(["promote", "--repo", "dotcms/dotcms-test"])
    rc = cmd_promote(args)
    assert rc == 0
    mock_point_tag.assert_not_called()


@patch("evergreen_tracks.cli.point_tag")
@patch("evergreen_tracks.cli.list_tags")
def test_promote_apply_calls_point_tag(mock_list_tags, mock_point_tag):
    """With --apply and a pending move, point_tag must be called with apply=True."""
    mock_list_tags.return_value = _tags_for_promote()
    args = build_parser().parse_args(
        ["promote", "--repo", "dotcms/dotcms-test", "--apply",
         "--latest-days", "0", "--standard-days", "0", "--trailing-days", "0"]
    )
    with patch("evergreen_tracks.cli.dt") as mock_dt:
        mock_dt.date.today.return_value = dt.date(2026, 7, 15)
        rc = cmd_promote(args)
    assert rc == 0
    # point_tag should have been called for tracks that have a pending move
    assert mock_point_tag.call_count >= 1
    # All calls must use apply=True
    for call in mock_point_tag.call_args_list:
        assert call.kwargs.get("apply") is True or call[1].get("apply") is True


@patch("evergreen_tracks.cli.point_tag")
@patch("evergreen_tracks.cli.list_tags")
def test_promote_tracks_latest_never_moves_standard_or_trailing(mock_list_tags, mock_point_tag):
    """`--tracks latest` (the release pipeline's on-demand call) must move ONLY latest.

    standard/trailing move solely on a manual evergreen-tracks-promote dispatch;
    a release cut repointing them would trigger an off-window pod roll. Regression
    guard for issue #36520 concrete-scope item 3.
    """
    mock_list_tags.return_value = [
        _make_tag("26.06.02-01", "sha256:aaa"),
        _make_tag("26.06.16-01", "sha256:bbb"),
    ]
    # Zero thresholds so standard/trailing WOULD move if they weren't filtered out.
    args = build_parser().parse_args(
        ["promote", "--repo", "dotcms/dotcms-test", "--apply", "--tracks", "latest",
         "--latest-days", "0", "--standard-days", "0", "--trailing-days", "0"]
    )
    with patch("evergreen_tracks.cli.dt") as mock_dt:
        mock_dt.date.today.return_value = dt.date(2026, 7, 15)
        rc = cmd_promote(args)
    assert rc == 0
    moved = {c.args[1] for c in mock_point_tag.call_args_list}
    assert moved == {"latest"}, f"expected only 'latest' to move, got {moved}"


@patch("evergreen_tracks.cli.point_tag")
@patch("evergreen_tracks.cli.list_tags")
def test_promote_tracks_latest_ignores_held_standard(mock_list_tags, mock_point_tag):
    """`--tracks latest` must not touch a HELD standard/trailing tag either.

    cmd_promote has a second mutation path — the held-track reconciliation loop —
    scoped only by `held = held & wanted`. A release run (--tracks latest) that
    reconciled a drifted, held `standard` back to its hold marker would still roll
    pods on that track. This pins that scoping so the release stays latest-only
    even when a hold marker is present. Regression guard for issue #36520.
    """
    mock_list_tags.return_value = [
        _make_tag("26.06.02-01", "sha256:aaa"),
        _make_tag("26.06.16-01", "sha256:bbb"),
        # standard is held to the older release, but its floating tag has drifted.
        _make_tag("standard_hold", "sha256:aaa"),
        _make_tag("standard", "sha256:bbb"),
    ]
    args = build_parser().parse_args(
        ["promote", "--repo", "dotcms/dotcms-test", "--apply", "--tracks", "latest",
         "--latest-days", "0", "--standard-days", "0", "--trailing-days", "0"]
    )
    with patch("evergreen_tracks.cli.dt") as mock_dt:
        mock_dt.date.today.return_value = dt.date(2026, 7, 15)
        rc = cmd_promote(args)
    assert rc == 0
    moved = {c.args[1] for c in mock_point_tag.call_args_list}
    assert "standard" not in moved, f"held 'standard' must not move on --tracks latest, got {moved}"
    assert moved <= {"latest"}, f"expected at most 'latest' to move, got {moved}"


# ---------------------------------------------------------------------------
# _current_version — newest GA wins on a shared digest (FIX 1)
# ---------------------------------------------------------------------------

def test_current_version_picks_newest_on_shared_digest():
    """When two GA versions share the track tag's digest, the NEWEST must be returned."""
    from evergreen_tracks.calver import parse_release

    digests = {
        "latest": "sha256:shared",
        "26.06.02-01": "sha256:shared",  # older GA, same digest
        "26.06.16-01": "sha256:shared",  # newer GA, same digest
    }
    releases = [
        parse_release("26.06.02-01"),
        parse_release("26.06.16-01"),
    ]
    assert _current_version("latest", digests, releases) == "26.06.16-01"


def test_current_version_none_when_no_match():
    """No matching digest -> None."""
    from evergreen_tracks.calver import parse_release

    digests = {"latest": "sha256:zzz", "26.06.02-01": "sha256:aaa"}
    releases = [parse_release("26.06.02-01")]
    assert _current_version("latest", digests, releases) is None


# ---------------------------------------------------------------------------
# cmd_promote — held track reconciliation (FIX 2)
# ---------------------------------------------------------------------------

@patch("evergreen_tracks.cli.point_tag")
@patch("evergreen_tracks.cli.list_tags")
def test_promote_reconciles_held_track_to_hold_digest(mock_list_tags, mock_point_tag):
    """A held track whose floating tag diverges from its hold marker must be reconciled
    to the hold marker's digest (not promoted elsewhere)."""
    mock_list_tags.return_value = [
        _make_tag("26.06.02-01", "sha256:aaa"),
        _make_tag("26.06.16-01", "sha256:bbb"),
        # standard is held to the older release...
        _make_tag("standard_hold", "sha256:aaa"),
        # ...but the floating standard tag has drifted to the newer digest.
        _make_tag("standard", "sha256:bbb"),
    ]
    args = build_parser().parse_args(
        ["promote", "--repo", "dotcms/dotcms-test", "--apply",
         "--latest-days", "0", "--standard-days", "0", "--trailing-days", "0"]
    )
    with patch("evergreen_tracks.cli.dt") as mock_dt:
        mock_dt.date.today.return_value = dt.date(2026, 7, 15)
        rc = cmd_promote(args)
    assert rc == 0
    # The held "standard" track must be pointed at the hold marker digest (sha256:aaa),
    # and never at the newer digest (sha256:bbb) as a promotion.
    standard_calls = [c for c in mock_point_tag.call_args_list if c.args[1] == "standard"]
    assert standard_calls, "expected a point_tag call reconciling the held 'standard' track"
    for c in standard_calls:
        assert c.args[2] == "sha256:aaa"
        assert c.kwargs.get("apply") is True


@patch("evergreen_tracks.cli.point_tag")
@patch("evergreen_tracks.cli.list_tags")
def test_promote_held_track_logs_when_consistent(mock_list_tags, mock_point_tag, caplog):
    """A held track already matching its hold marker must log that it is held and
    skipped (no silent no-op), and emit no reconciling point_tag call."""
    mock_list_tags.return_value = [
        _make_tag("26.06.02-01", "sha256:aaa"),
        _make_tag("standard_hold", "sha256:aaa"),
        _make_tag("standard", "sha256:aaa"),  # already consistent with the hold
    ]
    args = build_parser().parse_args(
        ["promote", "--repo", "dotcms/dotcms-test", "--apply",
         "--latest-days", "0", "--standard-days", "0", "--trailing-days", "0"]
    )
    with caplog.at_level("INFO", logger="evergreen_tracks"):
        with patch("evergreen_tracks.cli.dt") as mock_dt:
            mock_dt.date.today.return_value = dt.date(2026, 7, 15)
            rc = cmd_promote(args)
    assert rc == 0
    assert any("held at standard_hold" in r.message for r in caplog.records)
    assert not [c for c in mock_point_tag.call_args_list if c.args[1] == "standard"]


# ---------------------------------------------------------------------------
# cmd_admin — taint
# ---------------------------------------------------------------------------

@patch("evergreen_tracks.cli.point_tag")
@patch("evergreen_tracks.cli.list_tags")
def test_admin_taint_dry_run_returns_zero(mock_list_tags, mock_point_tag):
    mock_list_tags.return_value = _tags_for_promote()
    args = build_parser().parse_args(
        ["admin", "--repo", "dotcms/dotcms-test",
         "--action", "taint", "--version", "26.06.02-01"]
    )
    rc = cmd_admin(args)
    assert rc == 0
    mock_point_tag.assert_called_once()
    _, kwargs = mock_point_tag.call_args
    assert kwargs["apply"] is False


@patch("evergreen_tracks.cli.point_tag")
@patch("evergreen_tracks.cli.list_tags")
def test_admin_taint_version_not_in_registry_returns_2(mock_list_tags, mock_point_tag):
    """Tainting a version that doesn't exist in the registry must return 2."""
    mock_list_tags.return_value = _tags_for_promote()
    args = build_parser().parse_args(
        ["admin", "--repo", "dotcms/dotcms-test",
         "--action", "taint", "--version", "26.01.01-01"]
    )
    rc = cmd_admin(args)
    assert rc == 2
    mock_point_tag.assert_not_called()


@patch("evergreen_tracks.cli.point_tag")
@patch("evergreen_tracks.cli.list_tags")
def test_admin_taint_invalid_version_returns_2(mock_list_tags, mock_point_tag):
    """Tainting a non-GA version string must return 2."""
    mock_list_tags.return_value = _tags_for_promote()
    args = build_parser().parse_args(
        ["admin", "--repo", "dotcms/dotcms-test",
         "--action", "taint", "--version", "not-a-version"]
    )
    rc = cmd_admin(args)
    assert rc == 2
    mock_point_tag.assert_not_called()


# ---------------------------------------------------------------------------
# cmd_admin — untaint (requires DOCKER env vars)
# ---------------------------------------------------------------------------

@patch("evergreen_tracks.cli.delete_tag")
@patch("evergreen_tracks.cli.hub_login", return_value="tok")
@patch("evergreen_tracks.cli.list_tags")
def test_admin_untaint_with_env_vars_returns_zero(mock_list_tags, mock_login, mock_delete, monkeypatch):
    monkeypatch.setenv("DOCKER_USERNAME", "user")
    monkeypatch.setenv("DOCKER_TOKEN", "secret")
    mock_list_tags.return_value = _tags_for_promote()
    args = build_parser().parse_args(
        ["admin", "--repo", "dotcms/dotcms-test",
         "--action", "untaint", "--version", "26.06.02-01", "--apply"]
    )
    rc = cmd_admin(args)
    assert rc == 0
    mock_login.assert_called_once_with("user", "secret")
    mock_delete.assert_called_once()


@patch("evergreen_tracks.cli.list_tags")
def test_admin_untaint_missing_docker_username_returns_2(mock_list_tags, monkeypatch):
    """Missing DOCKER_USERNAME must produce a clean error (not KeyError) and return 2."""
    monkeypatch.delenv("DOCKER_USERNAME", raising=False)
    monkeypatch.setenv("DOCKER_TOKEN", "secret")
    mock_list_tags.return_value = _tags_for_promote()
    args = build_parser().parse_args(
        ["admin", "--repo", "dotcms/dotcms-test",
         "--action", "untaint", "--version", "26.06.02-01", "--apply"]
    )
    rc = cmd_admin(args)
    assert rc == 2


@patch("evergreen_tracks.cli.list_tags")
def test_admin_untaint_missing_docker_token_returns_2(mock_list_tags, monkeypatch):
    """Missing DOCKER_TOKEN must produce a clean error (not KeyError) and return 2."""
    monkeypatch.setenv("DOCKER_USERNAME", "user")
    monkeypatch.delenv("DOCKER_TOKEN", raising=False)
    mock_list_tags.return_value = _tags_for_promote()
    args = build_parser().parse_args(
        ["admin", "--repo", "dotcms/dotcms-test",
         "--action", "untaint", "--version", "26.06.02-01", "--apply"]
    )
    rc = cmd_admin(args)
    assert rc == 2


@patch("evergreen_tracks.cli.list_tags")
def test_admin_untaint_missing_both_env_vars_returns_2(mock_list_tags, monkeypatch):
    """Missing both env vars must produce a clean error and return 2."""
    monkeypatch.delenv("DOCKER_USERNAME", raising=False)
    monkeypatch.delenv("DOCKER_TOKEN", raising=False)
    mock_list_tags.return_value = _tags_for_promote()
    args = build_parser().parse_args(
        ["admin", "--repo", "dotcms/dotcms-test",
         "--action", "untaint", "--version", "26.06.02-01", "--apply"]
    )
    rc = cmd_admin(args)
    assert rc == 2


@patch("evergreen_tracks.cli.delete_tag")
@patch("evergreen_tracks.cli.hub_login")
@patch("evergreen_tracks.cli.list_tags")
def test_admin_untaint_dry_run_needs_no_creds(mock_list_tags, mock_login, mock_delete, monkeypatch):
    """Dry-run (no --apply) must not require creds or hit Hub login."""
    monkeypatch.delenv("DOCKER_USERNAME", raising=False)
    monkeypatch.delenv("DOCKER_TOKEN", raising=False)
    mock_list_tags.return_value = _tags_for_promote()
    args = build_parser().parse_args(
        ["admin", "--repo", "dotcms/dotcms-test",
         "--action", "untaint", "--version", "26.06.02-01"]
    )
    rc = cmd_admin(args)
    assert rc == 0
    mock_login.assert_not_called()
    mock_delete.assert_called_once()  # called with apply=False -> logs the dry-run


# ---------------------------------------------------------------------------
# cmd_admin — hold
# ---------------------------------------------------------------------------

@patch("evergreen_tracks.cli.point_tag")
@patch("evergreen_tracks.cli.list_tags")
def test_admin_hold_unknown_track_returns_2(mock_list_tags, mock_point_tag):
    mock_list_tags.return_value = _tags_for_promote()
    args = build_parser().parse_args(
        ["admin", "--repo", "dotcms/dotcms-test",
         "--action", "hold", "--track", "bogus", "--version", "26.06.02-01"]
    )
    rc = cmd_admin(args)
    assert rc == 2
    mock_point_tag.assert_not_called()


@patch("evergreen_tracks.cli.point_tag")
@patch("evergreen_tracks.cli.list_tags")
def test_admin_hold_version_not_found_returns_2(mock_list_tags, mock_point_tag):
    mock_list_tags.return_value = _tags_for_promote()
    args = build_parser().parse_args(
        ["admin", "--repo", "dotcms/dotcms-test",
         "--action", "hold", "--track", "standard", "--version", "26.01.01-01"]
    )
    rc = cmd_admin(args)
    assert rc == 2
    mock_point_tag.assert_not_called()


@patch("evergreen_tracks.cli.point_tag")
@patch("evergreen_tracks.cli.list_tags")
def test_admin_hold_tainted_version_without_force_returns_2(mock_list_tags, mock_point_tag):
    """Holding a tainted version without --force must be rejected (return 2)."""
    mock_list_tags.return_value = _tags_for_hold()
    args = build_parser().parse_args(
        ["admin", "--repo", "dotcms/dotcms-test",
         "--action", "hold", "--track", "standard", "--version", "26.06.02-01"]
    )
    rc = cmd_admin(args)
    assert rc == 2
    mock_point_tag.assert_not_called()


@patch("evergreen_tracks.cli.point_tag")
@patch("evergreen_tracks.cli.list_tags")
def test_admin_hold_tainted_version_with_force_returns_zero(mock_list_tags, mock_point_tag):
    """Holding a tainted version with --force is allowed."""
    mock_list_tags.return_value = _tags_for_hold()
    args = build_parser().parse_args(
        ["admin", "--repo", "dotcms/dotcms-test",
         "--action", "hold", "--track", "standard", "--version", "26.06.02-01", "--force"]
    )
    rc = cmd_admin(args)
    assert rc == 0
    assert mock_point_tag.call_count == 2  # marker + track tag


@patch("evergreen_tracks.cli.point_tag")
@patch("evergreen_tracks.cli.list_tags")
def test_admin_hold_clean_version_returns_zero(mock_list_tags, mock_point_tag):
    """Normal hold on a clean, existing version must call point_tag twice and return 0."""
    mock_list_tags.return_value = _tags_for_promote()
    args = build_parser().parse_args(
        ["admin", "--repo", "dotcms/dotcms-test",
         "--action", "hold", "--track", "standard", "--version", "26.06.02-01"]
    )
    rc = cmd_admin(args)
    assert rc == 0
    assert mock_point_tag.call_count == 2  # hold marker + track tag itself


# ---------------------------------------------------------------------------
# cmd_admin — release-hold (requires DOCKER env vars)
# ---------------------------------------------------------------------------

@patch("evergreen_tracks.cli.delete_tag")
@patch("evergreen_tracks.cli.hub_login", return_value="tok")
@patch("evergreen_tracks.cli.list_tags")
def test_admin_release_hold_with_env_vars_returns_zero(mock_list_tags, mock_login, mock_delete, monkeypatch):
    monkeypatch.setenv("DOCKER_USERNAME", "user")
    monkeypatch.setenv("DOCKER_TOKEN", "secret")
    mock_list_tags.return_value = _tags_for_promote()
    args = build_parser().parse_args(
        ["admin", "--repo", "dotcms/dotcms-test",
         "--action", "release-hold", "--track", "standard", "--apply"]
    )
    rc = cmd_admin(args)
    assert rc == 0
    mock_login.assert_called_once_with("user", "secret")
    mock_delete.assert_called_once()


@patch("evergreen_tracks.cli.list_tags")
def test_admin_release_hold_missing_docker_token_returns_2(mock_list_tags, monkeypatch):
    """Missing DOCKER_TOKEN for release-hold must return 2, not crash with KeyError."""
    monkeypatch.setenv("DOCKER_USERNAME", "user")
    monkeypatch.delenv("DOCKER_TOKEN", raising=False)
    mock_list_tags.return_value = _tags_for_promote()
    args = build_parser().parse_args(
        ["admin", "--repo", "dotcms/dotcms-test",
         "--action", "release-hold", "--track", "standard", "--apply"]
    )
    rc = cmd_admin(args)
    assert rc == 2


@patch("evergreen_tracks.cli.list_tags")
def test_admin_release_hold_missing_docker_username_returns_2(mock_list_tags, monkeypatch):
    monkeypatch.delenv("DOCKER_USERNAME", raising=False)
    monkeypatch.setenv("DOCKER_TOKEN", "secret")
    mock_list_tags.return_value = _tags_for_promote()
    args = build_parser().parse_args(
        ["admin", "--repo", "dotcms/dotcms-test",
         "--action", "release-hold", "--track", "standard", "--apply"]
    )
    rc = cmd_admin(args)
    assert rc == 2


@patch("evergreen_tracks.cli.delete_tag")
@patch("evergreen_tracks.cli.hub_login")
@patch("evergreen_tracks.cli.list_tags")
def test_admin_release_hold_dry_run_needs_no_creds(mock_list_tags, mock_login, mock_delete, monkeypatch):
    """release-hold dry-run (no --apply) must not require creds or hit Hub login."""
    monkeypatch.delenv("DOCKER_USERNAME", raising=False)
    monkeypatch.delenv("DOCKER_TOKEN", raising=False)
    mock_list_tags.return_value = _tags_for_promote()
    args = build_parser().parse_args(
        ["admin", "--repo", "dotcms/dotcms-test",
         "--action", "release-hold", "--track", "standard"]
    )
    rc = cmd_admin(args)
    assert rc == 0
    mock_login.assert_not_called()
    mock_delete.assert_called_once()


@patch("evergreen_tracks.cli.list_tags")
def test_admin_release_hold_unknown_track_returns_2(mock_list_tags, monkeypatch):
    monkeypatch.setenv("DOCKER_USERNAME", "user")
    monkeypatch.setenv("DOCKER_TOKEN", "secret")
    mock_list_tags.return_value = _tags_for_promote()
    args = build_parser().parse_args(
        ["admin", "--repo", "dotcms/dotcms-test",
         "--action", "release-hold", "--track", "bogus"]
    )
    rc = cmd_admin(args)
    assert rc == 2


# ---------------------------------------------------------------------------
# cmd_promote — --tracks subset (release pipeline moves only `latest`)
# ---------------------------------------------------------------------------

def test_promote_tracks_defaults_empty():
    args = build_parser().parse_args(["promote", "--repo", "dotcms/dotcms-test"])
    assert args.tracks == ""


@patch("evergreen_tracks.cli.point_tag")
@patch("evergreen_tracks.cli.list_tags")
def test_promote_tracks_latest_moves_only_latest(mock_list_tags, mock_point_tag):
    """--tracks latest must move ONLY the latest tag, even when standard/trailing
    are also eligible (thresholds 0). This is the release-pipeline invocation."""
    mock_list_tags.return_value = _tags_for_promote()
    args = build_parser().parse_args(
        ["promote", "--repo", "dotcms/dotcms-test", "--apply", "--tracks", "latest",
         "--latest-days", "0", "--standard-days", "0", "--trailing-days", "0"]
    )
    with patch("evergreen_tracks.cli.dt") as mock_dt:
        mock_dt.date.today.return_value = dt.date(2026, 7, 15)
        rc = cmd_promote(args)
    assert rc == 0
    moved = {call.args[1] for call in mock_point_tag.call_args_list}
    assert moved == {"latest"}


@patch("evergreen_tracks.cli.point_tag")
@patch("evergreen_tracks.cli.list_tags")
def test_promote_tracks_unknown_returns_2(mock_list_tags, mock_point_tag):
    mock_list_tags.return_value = _tags_for_promote()
    args = build_parser().parse_args(
        ["promote", "--repo", "dotcms/dotcms-test", "--tracks", "bogus"]
    )
    rc = cmd_promote(args)
    assert rc == 2
    mock_point_tag.assert_not_called()


# ---------------------------------------------------------------------------
# Narrow (filtered) registry reads — see cli._promote_state
# ---------------------------------------------------------------------------

def _fake_registry(tags):
    """Stand-in for registry.list_tags that honours Hub's `name=` SUBSTRING filter.

    An empty filter returns everything, which is exactly what the real full listing
    does — so `"" in calls` is the assertion for "fell back to the 79-page scan".
    """
    calls: list[str] = []

    def fake(repo, *, name_filter=""):
        calls.append(name_filter)
        return [t for t in tags if name_filter in t.name]

    return fake, calls


def _registry_tags(standard_digest="sha256:r0705", trailing_digest="sha256:r0602"):
    """Four GA releases plus two floating track tags pointing into that history."""
    return [
        Tag("26.06.02-01", "sha256:r0602"),
        Tag("26.07.05-01", "sha256:r0705"),
        Tag("26.07.28-01", "sha256:r0728"),
        Tag("26.08.10-01", "sha256:r0810"),
        Tag("standard", standard_digest),
        Tag("trailing", trailing_digest),
    ]


@patch("evergreen_tracks.cli.point_tag")
@patch("evergreen_tracks.cli.list_tags")
def test_promote_uses_filtered_reads_not_the_full_listing(mock_list_tags, mock_point_tag):
    """The daily promote must plan from a handful of filtered calls, never the full scan.

    This is the whole point of the narrow path: the full listing is 79 calls against a
    180-req/60s per-IP Hub budget.
    """
    fake, calls = _fake_registry(_registry_tags())
    mock_list_tags.side_effect = fake
    args = build_parser().parse_args(
        ["promote", "--repo", "dotcms/dotcms-test", "--tracks", "standard,trailing",
         "--standard-days", "14", "--trailing-days", "28"]
    )
    with patch("evergreen_tracks.cli.dt") as mock_dt:
        mock_dt.date.today.return_value = dt.date(2026, 8, 11)
        rc = cmd_promote(args)

    assert rc == 0
    assert "" not in calls, f"fell back to the unfiltered full listing: {calls}"
    assert len(calls) <= 6, f"expected a handful of filtered calls, got {calls}"
    # Newest release ≥14d is 26.07.28-01; newest ≥28d is 26.07.05-01.
    moved = {c.args[1]: c.args[2] for c in mock_point_tag.call_args_list}
    assert moved == {"standard": "sha256:r0728", "trailing": "sha256:r0705"}


@patch("evergreen_tracks.cli.point_tag")
@patch("evergreen_tracks.cli.list_tags")
def test_promote_walks_back_months_for_an_eligible_release(mock_list_tags, mock_point_tag):
    """The threshold cutoff can sit in an earlier month than today's, so walk 1 must
    keep fetching months until a candidate actually clears the bar."""
    fake, calls = _fake_registry(_registry_tags())
    mock_list_tags.side_effect = fake
    args = build_parser().parse_args(
        ["promote", "--repo", "dotcms/dotcms-test", "--tracks", "trailing",
         "--trailing-days", "28"]
    )
    with patch("evergreen_tracks.cli.dt") as mock_dt:
        mock_dt.date.today.return_value = dt.date(2026, 8, 11)
        cmd_promote(args)

    # 26.08 holds nothing ≥28 days old, so it must have walked back to 26.07.
    assert "26.08" in calls and "26.07" in calls
    assert "" not in calls


@patch("evergreen_tracks.cli.point_tag")
@patch("evergreen_tracks.cli.list_tags")
def test_promote_narrow_reads_still_refuse_to_move_a_track_backwards(
    mock_list_tags, mock_point_tag
):
    """Forward-only must survive the narrowed pool.

    `standard` sits on 26.08.10-01, newer than anything 14 days old. If the filtered
    reads failed to name the current version, `plan` would skip its forward-only guard
    and demote the track to 26.07.28-01. Nothing may move here.
    """
    fake, calls = _fake_registry(_registry_tags(standard_digest="sha256:r0810"))
    mock_list_tags.side_effect = fake
    args = build_parser().parse_args(
        ["promote", "--repo", "dotcms/dotcms-test", "--tracks", "standard",
         "--standard-days", "14"]
    )
    with patch("evergreen_tracks.cli.dt") as mock_dt:
        mock_dt.date.today.return_value = dt.date(2026, 8, 11)
        rc = cmd_promote(args)

    assert rc == 0
    assert "" not in calls
    mock_point_tag.assert_not_called()


@patch("evergreen_tracks.cli.point_tag")
@patch("evergreen_tracks.cli.list_tags")
def test_promote_falls_back_to_full_listing_when_the_window_is_empty(
    mock_list_tags, mock_point_tag
):
    """Nothing promotable in the walked months must fall back to the full listing.

    "No moves" is indistinguishable from a broken read path, and a quiet stall is how
    tracks would silently stop advancing — so the expensive answer is worth having here.
    """
    fake, calls = _fake_registry(_registry_tags())
    mock_list_tags.side_effect = fake
    args = build_parser().parse_args(
        ["promote", "--repo", "dotcms/dotcms-test", "--tracks", "standard",
         "--standard-days", "14"]
    )
    # Far enough ahead that the 6-month walk window holds no release at all.
    with patch("evergreen_tracks.cli.dt") as mock_dt:
        mock_dt.date.today.return_value = dt.date(2027, 6, 11)
        rc = cmd_promote(args)

    assert rc == 0
    assert "" in calls, f"expected the terminal full-listing fallback, got {calls}"


@patch("evergreen_tracks.cli.point_tag")
@patch("evergreen_tracks.cli.list_tags")
def test_promote_scoped_to_latest_never_reads_other_tracks(mock_list_tags, mock_point_tag):
    """--tracks latest (the release pipeline's path) must not spend calls on the
    tracks it was told to leave alone."""
    fake, calls = _fake_registry(_registry_tags())
    mock_list_tags.side_effect = fake
    args = build_parser().parse_args(
        ["promote", "--repo", "dotcms/dotcms-test", "--tracks", "latest", "--latest-days", "0"]
    )
    with patch("evergreen_tracks.cli.dt") as mock_dt:
        mock_dt.date.today.return_value = dt.date(2026, 8, 11)
        cmd_promote(args)

    assert "standard" not in calls and "trailing" not in calls, calls
