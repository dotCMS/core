"""Tests for the upsert/publish decision and the fired contentlet payload.

`responses` intercepts both the `_search` and the workflow-`fire` HTTP calls so the
create/update/skip decisions are exercised end-to-end without a live backend.
"""
from __future__ import annotations

import json
from pathlib import Path

import pytest
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


# The service-account identity is the immutable user id (modUser), not the mutable
# display name (modUserName) — per lead directive, a rename must not silently disable
# FR-011 protection. data-model.md documents the display-name fallback.
SERVICE_ACCOUNT = "user-devbot-0000"


def _publish(client, **overrides):
    kwargs = dict(
        version="26.07.10-01",
        release_notes="### Fixes {#Fixes-26.07.10-01}\n- Something ([#1](https://x/pull/1))\n",
        docker_image="dotcms/dotcms:26.07.10-01_abc1234",
        released_date="2026-07-16",
        service_account=SERVICE_ACCOUNT,
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

    result = _publish(client)

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

    _publish(client)

    assert "identifier" not in _fired_contentlet()


@responses_lib.activate
def test_fire_payload_always_disables_wysiwyg_for_release_notes():
    """Regression guard (FR-005): disabledWYSIWYG must always list releaseNotes, or the
    markdown collapses into a single <p> on the site."""
    _add_search("search_empty.json")
    _add_fire()
    client = CorpsitesClient(token="t")

    _publish(client)

    assert _fired_contentlet()["disabledWYSIWYG"] == ["releaseNotes"]


# ===========================================================================
# User Story 2 — update-in-place, idempotency, human-edit protection, >1 guard
# ===========================================================================

def _all_fire_bodies() -> list[dict]:
    return [
        json.loads(c.request.body)["contentlet"]
        for c in responses_lib.calls
        if c.request.method == "PUT"
    ]


@responses_lib.activate
def test_update_in_place_fires_with_found_identifier():
    """1 hit owned by the service account -> update in place: the fire carries the found
    identifier (no second row) (FR-003, SC-003)."""
    _add_search("search_hit_automation.json")
    _add_fire()
    client = CorpsitesClient(token="t")

    result = _publish(client)

    assert result.status == "updated"
    c = _fired_contentlet()
    assert c["identifier"] == "aaaa1111-bbbb-2222-cccc-333344445555"


@responses_lib.activate
def test_idempotent_rerun_takes_update_path_with_latest_notes():
    """Two runs (miss then hit) converge to one row: run 1 creates, run 2 updates in place
    carrying the latest notes — never a second create (FR-004)."""
    # responses consumes registered responses in order for the same URL.
    _add_search("search_empty.json")   # run 1: no existing row
    _add_fire()
    _add_search("search_hit_automation.json")  # run 2: the row now exists (automation-owned)
    _add_fire()
    client = CorpsitesClient(token="t")

    first = _publish(client)
    second = _publish(client, release_notes="### Fixes {#Fixes-26.07.10-01}\n- Corrected ([#2](x))\n")

    assert first.status == "created"
    assert second.status == "updated"
    bodies = _all_fire_bodies()
    assert len(bodies) == 2
    assert "identifier" not in bodies[0]                      # create
    assert bodies[1]["identifier"] == "aaaa1111-bbbb-2222-cccc-333344445555"  # update in place
    assert "Corrected" in bodies[1]["releaseNotes"]           # latest notes win


@responses_lib.activate
def test_human_edited_entry_is_skipped_without_force():
    """1 hit last modified by someone other than the service account -> skip, no fire,
    skip reason names the human modifier (FR-011, SC-006)."""
    _add_search("search_hit.json")  # modUser = Jamie Mauro's id, != service account
    _add_fire()
    client = CorpsitesClient(token="t")

    result = _publish(client)

    assert result.status == "skipped"
    assert _all_fire_bodies() == []          # nothing fired
    assert "Jamie Mauro" in (result.reason or "")


@responses_lib.activate
def test_force_overrides_human_edit_protection():
    """--force updates a human-edited entry in place (explicit operator override, FR-011)."""
    _add_search("search_hit.json")
    _add_fire()
    client = CorpsitesClient(token="t")

    result = _publish(client, force=True)

    assert result.status == "updated"
    assert _fired_contentlet()["identifier"] == "d6d34add-14d7-43df-8270-03cb104063f3"


@responses_lib.activate
def test_more_than_one_hit_raises_ambiguous():
    """>1 hit -> error (never guess which to update); feeds the US3 failure path (D4)."""
    _add_search("search_two_hits.json")
    _add_fire()
    client = CorpsitesClient(token="t")

    with pytest.raises(publisher.AmbiguousMatchError):
        _publish(client)

    assert _all_fire_bodies() == []
