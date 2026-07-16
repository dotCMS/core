"""Edge-case coverage from the spec (FR-013, Edge Cases): internal-maintenance-only
entries still publish, same-day hotfix suffixes each get their own row, and an older-line
patch carries today's date without touching any other row.
"""
from __future__ import annotations

import json

import responses as responses_lib

from changelog_publisher import publisher
from changelog_publisher.client import BASE_URL, PUBLISH_ACTION_ID, CorpsitesClient
from changelog_publisher.version import is_current_track

_SEARCH_URL = f"{BASE_URL}/api/content/_search"
_FIRE_URL = f"{BASE_URL}/api/v1/workflow/actions/{PUBLISH_ACTION_ID}/fire"

_EMPTY = {"entity": {"jsonObjectView": {"contentlets": []}}}


def _add_search_empty() -> None:
    responses_lib.add(responses_lib.POST, _SEARCH_URL, json=_EMPTY, status=200)


def _add_fire() -> None:
    responses_lib.add(responses_lib.PUT, _FIRE_URL, json={"entity": {}}, status=200)


def _publish(client, version, **overrides):
    kwargs = dict(
        release_notes="**dotCMS %s** notes\n" % version,
        docker_image=f"dotcms/dotcms:{version}_abc1234",
        released_date="2026-07-16",
        apply=True,
    )
    kwargs.update(overrides)
    return publisher.publish(client, version=version, **kwargs)


def _fire_bodies():
    return [
        json.loads(c.request.body)["contentlet"]
        for c in responses_lib.calls
        if c.request.method == "PUT"
    ]


def _search_queries():
    return [
        json.loads(c.request.body)["query"]
        for c in responses_lib.calls
        if c.request.method == "POST"
    ]


@responses_lib.activate
def test_internal_maintenance_only_still_publishes_an_entry():
    """A release with no customer-facing changes still gets an entry (site convention)."""
    _add_search_empty()
    _add_fire()
    client = CorpsitesClient(token="t")

    result = _publish(
        client, "26.07.10-01",
        release_notes="**dotCMS 26.07.10-01** This release contains internal maintenance only.\n",
    )

    assert result.status == "created"
    body = _fire_bodies()[0]
    assert "internal maintenance only" in body["releaseNotes"]
    assert body["disabledWYSIWYG"] == ["releaseNotes"]


@responses_lib.activate
def test_same_day_hotfix_suffixes_each_publish_their_own_row():
    """26.07.10-02 and 26.07.10-03 are distinct upsert keys -> each its own create."""
    assert is_current_track("26.07.10-02") and is_current_track("26.07.10-03")
    _add_search_empty(); _add_fire()   # -02
    _add_search_empty(); _add_fire()   # -03
    client = CorpsitesClient(token="t")

    _publish(client, "26.07.10-02")
    _publish(client, "26.07.10-03")

    minors = [b["minor"] for b in _fire_bodies()]
    assert minors == ["26.07.10-02", "26.07.10-03"]
    # Both are creates (no identifier) — never a shared/overwritten row.
    assert all("identifier" not in b for b in _fire_bodies())


@responses_lib.activate
def test_older_line_patch_carries_todays_date_and_touches_only_its_version():
    """A -04 patch of an older line gets its actual (today's) date and the tool searches /
    writes ONLY that version — it never reorders or touches other rows (FR-013)."""
    _add_search_empty()
    _add_fire()
    client = CorpsitesClient(token="t")

    result = _publish(client, "26.05.27-04", released_date="2026-07-16")

    assert result.status == "created"
    body = _fire_bodies()[0]
    assert body["minor"] == "26.05.27-04"
    assert body["releasedDate"] == "2026-07-16"  # today, not the original line's date
    # Exactly one search, scoped to this version only.
    assert _search_queries() == ["+contentType:Dotcmsbuilds +Dotcmsbuilds.minor_dotraw:26.05.27-04"]