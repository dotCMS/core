"""Tests for the corpsites HTTP client (`_search`).

Uses `responses` to intercept HTTP so the tests run fully offline, mirroring the
`evergreen-tracks` test idiom.
"""
from __future__ import annotations

import json
from pathlib import Path

import responses as responses_lib

from changelog_publisher.client import BASE_URL, CorpsitesClient

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
