"""Tests for the CLI layer and version validation.

`responses.activate` with no registered endpoint means any HTTP attempt raises a
ConnectionError — that is how the "rejected before any network call" assertions are
enforced.
"""
from __future__ import annotations

import json
from pathlib import Path

import pytest
import responses as responses_lib

from changelog_publisher.cli import main
from changelog_publisher.client import BASE_URL, PUBLISH_ACTION_ID
from changelog_publisher.version import is_current_track

_FIXTURES = Path(__file__).parent / "fixtures"
_SEARCH_URL = f"{BASE_URL}/api/content/_search"
_FIRE_URL = f"{BASE_URL}/api/v1/workflow/actions/{PUBLISH_ACTION_ID}/fire"


def _fixture(name: str) -> dict:
    return json.loads((_FIXTURES / name).read_text())


def _notes_file(tmp_path: Path) -> str:
    f = tmp_path / "site-release-notes.md"
    f.write_text("### Fixes {#Fixes-26.07.10-01}\n- A fix ([#1](https://x/pull/1))\n")
    return str(f)


def _argv(tmp_path: Path, version: str, *extra: str) -> list[str]:
    return [
        "publish",
        "--version", version,
        "--notes-file", _notes_file(tmp_path),
        "--docker-image", "dotcms/dotcms:26.07.10-01_abc1234",
        "--released-date", "2026-07-16",
        *extra,
    ]


# ---------------------------------------------------------------------------
# version.py — current-track CalVer validation (T012, FR-007)
# ---------------------------------------------------------------------------

def test_is_current_track_accepts_calver():
    assert is_current_track("26.07.10-01") is True
    assert is_current_track("26.07.10-12") is True


def test_is_current_track_rejects_lts():
    assert is_current_track("26.07.10-01_lts_v3") is False
    assert is_current_track("26.07.10_lts_v3") is False


def test_is_current_track_rejects_cli():
    assert is_current_track("dotcms-cli-1.2.3") is False


def test_is_current_track_rejects_garbage():
    assert is_current_track("v26.07.10-01") is False  # no v-prefix on the minor field
    assert is_current_track("not-a-version") is False
    assert is_current_track("26.7.10-1") is False       # segments must be two digits


# ---------------------------------------------------------------------------
# CLI defense-in-depth: out-of-scope versions rejected before any network call
# ---------------------------------------------------------------------------

@responses_lib.activate
def test_cli_rejects_lts_version_before_network(tmp_path, monkeypatch):
    monkeypatch.setenv("DOTCMS_DEVSITE_TOKEN", "t")
    rc = main(_argv(tmp_path, "26.07.10-01_lts_v3", "--apply"))
    assert rc == 2
    assert len(responses_lib.calls) == 0  # no HTTP attempted


@responses_lib.activate
def test_cli_rejects_cli_release_before_network(tmp_path, monkeypatch):
    monkeypatch.setenv("DOTCMS_DEVSITE_TOKEN", "t")
    rc = main(_argv(tmp_path, "dotcms-cli-1.2.3", "--apply"))
    assert rc == 2
    assert len(responses_lib.calls) == 0


# ---------------------------------------------------------------------------
# Dry-run by default / --apply (T013)
# ---------------------------------------------------------------------------

@responses_lib.activate
def test_dry_run_default_issues_no_fire_call(tmp_path, monkeypatch, capsys):
    """Without --apply: a search may run, but the fire (PUT) call must not be issued,
    and the intended action is printed."""
    monkeypatch.setenv("DOTCMS_DEVSITE_TOKEN", "t")
    responses_lib.add(responses_lib.POST, _SEARCH_URL, json=_fixture("search_empty.json"), status=200)
    responses_lib.add(responses_lib.PUT, _FIRE_URL, json={"entity": {}}, status=200)

    rc = main(_argv(tmp_path, "26.07.10-01"))

    assert rc == 0
    put_calls = [c for c in responses_lib.calls if c.request.method == "PUT"]
    assert put_calls == []
    out = capsys.readouterr().out
    assert "26.07.10-01" in out


@responses_lib.activate
def test_apply_issues_fire_call(tmp_path, monkeypatch):
    """With --apply: the fire (PUT) call is issued."""
    monkeypatch.setenv("DOTCMS_DEVSITE_TOKEN", "t")
    responses_lib.add(responses_lib.POST, _SEARCH_URL, json=_fixture("search_empty.json"), status=200)
    responses_lib.add(responses_lib.PUT, _FIRE_URL, json={"entity": {}}, status=200)

    rc = main(_argv(tmp_path, "26.07.10-01", "--apply"))

    assert rc == 0
    put_calls = [c for c in responses_lib.calls if c.request.method == "PUT"]
    assert len(put_calls) == 1


# ===========================================================================
# User Story 3 — failure exit contract + skip-vs-failure stdout distinction
# ===========================================================================

# A distinctive token so a leak into stdout/stderr/logs is unambiguous.
_SECRET_TOKEN = "SUPERSECRET-devsite-TOKEN-abc123"


@responses_lib.activate
def test_auth_error_exits_nonzero_without_leaking_token(tmp_path, monkeypatch, capsys, caplog):
    """A 401 on search -> non-zero exit; the bearer token never appears in
    stdout/stderr/logs, and no skip marker is emitted (FR-008, Constitution III)."""
    monkeypatch.setenv("DOTCMS_DEVSITE_TOKEN", _SECRET_TOKEN)
    responses_lib.add(responses_lib.POST, _SEARCH_URL, json={"message": "invalid token"}, status=401)

    with caplog.at_level("DEBUG"):
        rc = main(_argv(tmp_path, "26.07.10-01", "--apply"))

    assert rc != 0
    out, err = capsys.readouterr()
    assert _SECRET_TOKEN not in out
    assert _SECRET_TOKEN not in err
    assert _SECRET_TOKEN not in caplog.text
    assert "::changelog-skip::" not in out


@responses_lib.activate
def test_payload_rejected_exits_nonzero_without_skip_marker(tmp_path, monkeypatch, capsys):
    """A 4xx on the fire (payload rejected) -> non-zero exit, no skip marker."""
    monkeypatch.setenv("DOTCMS_DEVSITE_TOKEN", _SECRET_TOKEN)
    responses_lib.add(responses_lib.POST, _SEARCH_URL, json=_fixture("search_empty.json"), status=200)
    responses_lib.add(responses_lib.PUT, _FIRE_URL, json={"message": "bad payload"}, status=400)

    rc = main(_argv(tmp_path, "26.07.10-01", "--apply"))

    assert rc != 0
    out, _ = capsys.readouterr()
    assert _SECRET_TOKEN not in out
    assert "::changelog-skip::" not in out


@responses_lib.activate
def test_ambiguous_match_exits_nonzero(tmp_path, monkeypatch, capsys):
    """>1 hit -> non-zero exit (never guess), no skip marker."""
    monkeypatch.setenv("DOTCMS_DEVSITE_TOKEN", _SECRET_TOKEN)
    responses_lib.add(responses_lib.POST, _SEARCH_URL, json=_fixture("search_two_hits.json"), status=200)

    rc = main(_argv(tmp_path, "26.07.10-01", "--apply", "--service-account", "user-devbot-0000"))

    assert rc != 0
    out, _ = capsys.readouterr()
    assert "::changelog-skip::" not in out


@responses_lib.activate
def test_protective_skip_exits_zero_with_marker(tmp_path, monkeypatch, capsys):
    """A human-edit protective skip exits 0 and emits the skip marker naming the version,
    so the workflow can branch to skip (not failure) Slack wording (T005 contract)."""
    monkeypatch.setenv("DOTCMS_DEVSITE_TOKEN", _SECRET_TOKEN)
    responses_lib.add(responses_lib.POST, _SEARCH_URL, json=_fixture("search_hit.json"), status=200)

    rc = main(_argv(tmp_path, "26.07.10-01", "--apply", "--service-account", "user-devbot-0000"))

    assert rc == 0
    out, _ = capsys.readouterr()
    assert "::changelog-skip::26.07.10-01" in out


@responses_lib.activate
def test_missing_backend_url_exits_one_with_clean_error(tmp_path, monkeypatch, capsys, caplog):
    """A missing/empty DOTCMS_DEVSITE_URL -> rc 1 with a clean one-line error (the URL is a
    repo variable, never hardcoded — an unset variable must fail loudly, not fall back)."""
    monkeypatch.setenv("DOTCMS_DEVSITE_TOKEN", "t")
    monkeypatch.setattr("changelog_publisher.client.BASE_URL", "")

    with caplog.at_level("ERROR"):
        rc = main(_argv(tmp_path, "26.07.10-01", "--apply"))

    assert rc == 1
    out, err = capsys.readouterr()
    assert "Traceback" not in out and "Traceback" not in err
    assert any("DOTCMS_DEVSITE_URL" in r.message for r in caplog.records)
    assert len(responses_lib.calls) == 0  # failed before any network call


def test_missing_token_exits_one_with_clean_error(tmp_path, monkeypatch, capsys, caplog):
    """A missing DOTCMS_DEVSITE_TOKEN -> rc 1 with a clean one-line error, not a traceback."""
    monkeypatch.delenv("DOTCMS_DEVSITE_TOKEN", raising=False)

    with caplog.at_level("ERROR"):
        rc = main(_argv(tmp_path, "26.07.10-01", "--apply"))

    assert rc == 1
    out, err = capsys.readouterr()
    assert "Traceback" not in out and "Traceback" not in err
    assert any("DOTCMS_DEVSITE_TOKEN" in r.message for r in caplog.records)
    assert len(responses_lib.calls) == 0  # failed before any network call
