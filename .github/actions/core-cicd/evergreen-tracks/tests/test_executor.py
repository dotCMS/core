"""Tests for the executor write layer.

Uses `responses` to intercept HTTP calls and `unittest.mock.patch` to intercept
subprocess calls, keeping the tests fully offline.
"""
from __future__ import annotations

import subprocess
from unittest.mock import MagicMock, patch

import pytest
import responses as responses_lib

from evergreen_tracks.executor import delete_tag, hub_login, point_tag


# ---------------------------------------------------------------------------
# point_tag
# ---------------------------------------------------------------------------

def test_point_tag_dry_run_does_not_call_subprocess():
    with patch("subprocess.run") as mock_run:
        point_tag("dotcms/dotcms-test", "latest", "sha256:abc123", apply=False)
        mock_run.assert_not_called()


def test_point_tag_apply_calls_imagetools_create():
    with patch("subprocess.run") as mock_run:
        mock_run.return_value = MagicMock(returncode=0)
        point_tag("dotcms/dotcms-test", "latest", "sha256:abc123", apply=True)
        mock_run.assert_called_once()
        cmd = mock_run.call_args[0][0]
        assert "docker" in cmd
        assert "imagetools" in cmd
        assert "create" in cmd
        assert "dotcms/dotcms-test:latest" in cmd
        assert "dotcms/dotcms-test@sha256:abc123" in cmd


def test_point_tag_apply_propagates_subprocess_error():
    with patch("subprocess.run", side_effect=subprocess.CalledProcessError(1, "docker")):
        with pytest.raises(subprocess.CalledProcessError):
            point_tag("dotcms/dotcms-test", "latest", "sha256:abc123", apply=True)


# ---------------------------------------------------------------------------
# hub_login
# ---------------------------------------------------------------------------

@responses_lib.activate
def test_hub_login_returns_token_on_success():
    responses_lib.add(
        responses_lib.POST,
        "https://hub.docker.com/v2/users/login",
        json={"token": "mytoken123"},
        status=200,
    )
    token = hub_login("myuser", "mypassword")
    assert token == "mytoken123"


@responses_lib.activate
def test_hub_login_raises_descriptive_error_when_token_missing():
    """If the Hub returns 200 with no 'token' key, hub_login should raise RuntimeError."""
    responses_lib.add(
        responses_lib.POST,
        "https://hub.docker.com/v2/users/login",
        json={"detail": "incorrect_authentication_credentials"},
        status=200,
    )
    with pytest.raises(RuntimeError, match="token"):
        hub_login("baduser", "badpassword")


@responses_lib.activate
def test_hub_login_raises_on_http_error():
    responses_lib.add(
        responses_lib.POST,
        "https://hub.docker.com/v2/users/login",
        json={"detail": "unauthorized"},
        status=401,
    )
    with pytest.raises(Exception):
        hub_login("myuser", "wrongpassword")


# ---------------------------------------------------------------------------
# delete_tag
# ---------------------------------------------------------------------------

@responses_lib.activate
def test_delete_tag_dry_run_makes_no_http_call():
    # No responses registered — if an HTTP call is made, responses will raise ConnectionError.
    delete_tag("dotcms/dotcms-test", "26.06.11-01_tainted", "mytoken", apply=False)
    # If we get here without an error, no HTTP call was made.


@responses_lib.activate
def test_delete_tag_apply_sends_delete_request():
    responses_lib.add(
        responses_lib.DELETE,
        "https://hub.docker.com/v2/repositories/dotcms/dotcms-test/tags/26.06.11-01_tainted/",
        status=204,
    )
    delete_tag("dotcms/dotcms-test", "26.06.11-01_tainted", "mytoken", apply=True)
    assert len(responses_lib.calls) == 1
    assert responses_lib.calls[0].request.headers["Authorization"] == "JWT mytoken"


@responses_lib.activate
def test_delete_tag_apply_treats_404_as_success():
    responses_lib.add(
        responses_lib.DELETE,
        "https://hub.docker.com/v2/repositories/dotcms/dotcms-test/tags/26.06.11-01_tainted/",
        status=404,
    )
    # Should NOT raise even though 404
    delete_tag("dotcms/dotcms-test", "26.06.11-01_tainted", "mytoken", apply=True)


@responses_lib.activate
def test_delete_tag_apply_raises_on_server_error():
    responses_lib.add(
        responses_lib.DELETE,
        "https://hub.docker.com/v2/repositories/dotcms/dotcms-test/tags/26.06.11-01_tainted/",
        status=500,
    )
    with pytest.raises(Exception):
        delete_tag("dotcms/dotcms-test", "26.06.11-01_tainted", "mytoken", apply=True)
