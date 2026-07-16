"""Tests for the upsert/publish decision and the fired contentlet payload.

`responses` intercepts both the `_search` and the workflow-`fire` HTTP calls so the
create/update/skip decisions are exercised end-to-end without a live backend.
"""
from __future__ import annotations

import json
from pathlib import Path

import responses as responses_lib

from changelog_publisher import publisher
from changelog_publisher.client import BASE_URL, PUBLISH_ACTION_ID, CorpsitesClient

_FIXTURES = Path(__file__).parent / "fixtures"
_SEARCH_URL = f"{BASE_URL}/api/content/_search"
_FIRE_URL = f"{BASE_URL}/api/v1/workflow/actions/{PUBLISH_ACTION_ID}/fire"


def _fixture(name: str) -> dict:
    return json.loads((_FIXTURES / name).read_text())


def _add_search(fixture: str) -> None:
    responses_lib.add(responses_lib.POST, _SEARCH_URL, json=_fixture(fixture), status=200)


def _add_fire() -> None:
    responses_lib.add(responses_lib.PUT, _FIRE_URL, json={"entity": {}}, status=200)


def _fired_contentlet() -> dict:
    """The contentlet body sent to the fire endpoint (unwrapped from {"contentlet": ...})."""
    fire_calls = [c for c in responses_lib.calls if c.request.method == "PUT"]
    assert len(fire_calls) == 1, f"expected exactly one fire call, got {len(fire_calls)}"
    return json.loads(fire_calls[0].request.body)["contentlet"]


def _publish_create(client, **overrides):
    kwargs = dict(
        version="26.07.10-01",
        release_notes="### Fixes {#Fixes-26.07.10-01}\n- Something ([#1](https://x/pull/1))\n",
        docker_image="dotcms/dotcms:26.07.10-01_abc1234",
        released_date="2026-07-16",
        apply=True,
    )
    kwargs.update(overrides)
    return publisher.publish(client, **kwargs)


@responses_lib.activate
def test_create_path_fires_publish_with_expected_fields():
    """0-hit search -> create: the fired contentlet carries the confirmed field set."""
    _add_search("search_empty.json")
    _add_fire()
    client = CorpsitesClient(token="t")

    result = _publish_create(client)

    assert result.status == "created"
    c = _fired_contentlet()
    assert c["contentType"] == "Dotcmsbuilds"
    assert c["minor"] == "26.07.10-01"
    assert c["dockerImage"] == "dotcms/dotcms:26.07.10-01_abc1234"
    assert c["releasedDate"] == "2026-07-16"
    assert "### Fixes" in c["releaseNotes"]
    # Verified current-track stored values (T004, data-model.md).
    assert c["showInChangeLog"] is True
    assert c["lts"] == 3
    assert c["released"] is True
    assert c["download"] == 1


@responses_lib.activate
def test_create_path_sends_no_identifier():
    """Create must NOT carry an identifier (that is the update-in-place signal)."""
    _add_search("search_empty.json")
    _add_fire()
    client = CorpsitesClient(token="t")

    _publish_create(client)

    assert "identifier" not in _fired_contentlet()


@responses_lib.activate
def test_fire_payload_always_disables_wysiwyg_for_release_notes():
    """Regression guard (FR-005): disabledWYSIWYG must always list releaseNotes, or the
    markdown collapses into a single <p> on the site."""
    _add_search("search_empty.json")
    _add_fire()
    client = CorpsitesClient(token="t")

    _publish_create(client)

    assert _fired_contentlet()["disabledWYSIWYG"] == ["releaseNotes"]
