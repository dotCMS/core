import os

# Must be set before changelog_publisher.client is imported (BASE_URL and PUBLISH_ACTION_ID
# are read at import time); the real values are repo variables, never hardcoded in the codebase.
os.environ.setdefault("DOTCMS_DEVSITE_URL", "https://corpsites.test")
os.environ.setdefault("DOTCMS_DEVSITE_RELEASENOTES_ACTION", "test-action-id")

import pytest

from changelog_publisher import publisher


@pytest.fixture(autouse=True)
def _no_index_poll_delay(monkeypatch):
    """Zero the post-create read-back delay so tests never sleep."""
    monkeypatch.setattr(publisher, "_INDEX_POLL_DELAY_SECONDS", 0)


@pytest.fixture(autouse=True)
def _no_prod_poll_delay(monkeypatch):
    """Zero the wait between prod confirmation checks so tests never sleep."""
    from changelog_publisher import client
    monkeypatch.setattr(client, "PROD_POLL_DELAY_SECONDS", 0)


@pytest.fixture(autouse=True)
def _prod_confirmation_off(monkeypatch):
    """The CLI builds no prod reader, so suites that predate prod confirmation keep their
    exact HTTP call counts. test_prod_confirm.py turns it back on."""
    from changelog_publisher import cli
    monkeypatch.setattr(cli, "prod_reader", lambda: None)
