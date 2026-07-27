import os

# Must be set before changelog_publisher.client is imported (BASE_URL is read at import
# time); the real value is a repo variable, never hardcoded in the codebase.
os.environ.setdefault("DOTCMS_DEVSITE_URL", "https://corpsites.test")

import pytest

from changelog_publisher import publisher


@pytest.fixture(autouse=True)
def _no_index_poll_delay(monkeypatch):
    """Zero the post-create read-back delay so tests never sleep."""
    monkeypatch.setattr(publisher, "_INDEX_POLL_DELAY_SECONDS", 0)
