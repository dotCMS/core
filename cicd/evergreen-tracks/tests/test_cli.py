"""Tests for the CLI layer (cli.py).

All registry I/O is patched out via unittest.mock so tests run fully offline.
"""
from __future__ import annotations

import datetime as dt
from unittest.mock import MagicMock, patch

import pytest

from evergreen_tracks.cli import build_parser, cmd_admin, cmd_promote, main
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
         "--action", "untaint", "--version", "26.06.02-01"]
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
         "--action", "untaint", "--version", "26.06.02-01"]
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
         "--action", "untaint", "--version", "26.06.02-01"]
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
         "--action", "untaint", "--version", "26.06.02-01"]
    )
    rc = cmd_admin(args)
    assert rc == 2


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
         "--action", "release-hold", "--track", "standard"]
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
         "--action", "release-hold", "--track", "standard"]
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
         "--action", "release-hold", "--track", "standard"]
    )
    rc = cmd_admin(args)
    assert rc == 2


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
