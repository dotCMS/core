"""Shared test isolation for the whole suite."""
from __future__ import annotations

import pytest

from evergreen_tracks.registry import _jwt


@pytest.fixture(autouse=True)
def _no_ambient_hub_creds(monkeypatch):
    """Neutralise DOCKER_USERNAME / DOCKER_TOKEN inherited from the shell.

    `registry.list_tags` reads them to decide whether to authenticate, so a developer or
    runner with Docker creds exported would send the anonymous-path tests through a real
    `hub_login()` POST that `responses` never registered — turning deterministic tests
    into environment-dependent ones. Tests that WANT auth set the vars themselves.

    In conftest rather than one test module so it covers every module, including ones
    not written yet: the trap is invisible until someone's shell happens to have creds.

    Also drops the memoised JWT. `_jwt` is deliberately cached — one Hub login per
    process rather than one per request — but that cache outlives a test, so without
    this a later test using the same credentials silently reuses an earlier test's
    token. Verified: two tests with identical creds and different tokens, and the
    second one saw the first one's JWT.
    """
    monkeypatch.delenv("DOCKER_USERNAME", raising=False)
    monkeypatch.delenv("DOCKER_TOKEN", raising=False)
    _jwt.cache_clear()
