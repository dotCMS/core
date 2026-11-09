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
