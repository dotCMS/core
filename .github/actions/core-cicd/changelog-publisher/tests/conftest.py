import pytest

from changelog_publisher import publisher


@pytest.fixture(autouse=True)
def _no_index_poll_delay(monkeypatch):
    """Zero the post-create read-back delay so tests never sleep."""
    monkeypatch.setattr(publisher, "_INDEX_POLL_DELAY_SECONDS", 0)
