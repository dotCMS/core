"""Shared test isolation for the whole suite."""
from __future__ import annotations

import pytest


@pytest.fixture(autouse=True)
def _no_ambient_hub_creds(monkeypatch):
    """Neutralise DOCKER_USERNAME / DOCKER_TOKEN inherited from the shell.

    `registry.list_tags` reads them to decide whether to authenticate, so a developer or
    runner with Docker creds exported would send the anonymous-path tests through a real
    `hub_login()` POST that `responses` never registered — turning deterministic tests
    into environment-dependent ones. Tests that WANT auth set the vars themselves.

    In conftest rather than one test module so it covers every module, including ones
    not written yet: the trap is invisible until someone's shell happens to have creds.
    """
    monkeypatch.delenv("DOCKER_USERNAME", raising=False)
    monkeypatch.delenv("DOCKER_TOKEN", raising=False)
