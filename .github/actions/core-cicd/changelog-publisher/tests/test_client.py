"""Tests for the dev-site HTTP client (`_search` and the `fire` action id).

Uses `responses` to intercept HTTP so the tests run fully offline, mirroring the
`evergreen-tracks` test idiom.
"""
from __future__ import annotations

import json
from pathlib import Path

import pytest
import responses as responses_lib

from changelog_publisher.client import BASE_URL, PUBLISH_ACTION_ID, CorpsitesClient

_FIXTURES = Path(__file__).parent / "fixtures"
_SEARCH_URL = f"{BASE_URL}/api/content/_search"


def _fixture(name: str) -> dict:
    return json.loads((_FIXTURES / name).read_text())


@responses_lib.activate
def test_search_zero_hits_returns_empty_list_and_sends_correct_query():
    """0-hit search (create path): the query is the exact dotraw match with limit 2,
    and results are read from entity.jsonObjectView.contentlets."""
    responses_lib.add(
        responses_lib.POST,
        _SEARCH_URL,
        json=_fixture("search_empty.json"),
        status=200,
    )
    client = CorpsitesClient(token="testtoken")
    hits = client._search("26.07.10-01")

    assert hits == []
    assert len(responses_lib.calls) == 1
    body = json.loads(responses_lib.calls[0].request.body)
    assert body["query"] == "+contentType:Dotcmsbuilds +Dotcmsbuilds.minor_dotraw:26.07.10-01"
    assert body["limit"] == 2


@responses_lib.activate
def test_search_one_hit_parses_identifier_and_mod_user_name():
    """1-hit search: identifier + modUserName are read from the contentlet for the
    upsert decision (used by US2)."""
    responses_lib.add(
        responses_lib.POST,
        _SEARCH_URL,
        json=_fixture("search_hit.json"),
        status=200,
    )
    client = CorpsitesClient(token="testtoken")
    hits = client._search("26.06.30-01")

    assert len(hits) == 1
    assert hits[0].identifier == "d6d34add-14d7-43df-8270-03cb104063f3"
    assert hits[0].mod_user_name == "Jamie Mauro"


@responses_lib.activate
def test_search_sends_bearer_token_header():
    """The token is sent as an Authorization: Bearer header (and only there)."""
    responses_lib.add(
        responses_lib.POST,
        _SEARCH_URL,
        json=_fixture("search_empty.json"),
        status=200,
    )
    client = CorpsitesClient(token="s3cr3t")
    client._search("26.07.10-01")

    assert responses_lib.calls[0].request.headers["Authorization"] == "Bearer s3cr3t"


@responses_lib.activate
def test_fire_uses_action_id_from_env():
    """The fire URL carries the DOTCMS_DEVSITE_RELEASENOTES_ACTION value, not a hardcoded id."""
    assert PUBLISH_ACTION_ID == "test-action-id"  # set by conftest before import
    url = f"{BASE_URL}/api/v1/workflow/actions/test-action-id/fire"
    responses_lib.add(responses_lib.PUT, url, json={}, status=200)
    client = CorpsitesClient(token="testtoken")
    client.fire({"contentType": "Dotcmsbuilds", "minor": "26.07.10-01"}, apply=True)

    assert client.action_id == "test-action-id"
    assert len(responses_lib.calls) == 1
    assert responses_lib.calls[0].request.url == url


@responses_lib.activate
def test_fire_action_id_param_overrides_env():
    """The constructor `action_id` wins over the environment value."""
    url = f"{BASE_URL}/api/v1/workflow/actions/override-action/fire"
    responses_lib.add(responses_lib.PUT, url, json={}, status=200)
    client = CorpsitesClient(token="testtoken", action_id="override-action")
    client.fire({"contentType": "Dotcmsbuilds", "minor": "26.07.10-01"}, apply=True)

    assert len(responses_lib.calls) == 1
    assert responses_lib.calls[0].request.url == url


def test_missing_action_id_raises(monkeypatch):
    """An unset or empty action (an unset repo var renders as "") fails at construction."""
    monkeypatch.setattr("changelog_publisher.client.PUBLISH_ACTION_ID", "")
    with pytest.raises(RuntimeError, match="DOTCMS_DEVSITE_RELEASENOTES_ACTION is not set"):
        CorpsitesClient(token="testtoken")
    with pytest.raises(RuntimeError, match="DOTCMS_DEVSITE_RELEASENOTES_ACTION is not set"):
        CorpsitesClient(token="testtoken", action_id="   ")
