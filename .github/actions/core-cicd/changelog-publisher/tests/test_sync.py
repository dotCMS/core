"""Tests for `changelog-publisher sync-site` (EvergreenState reconcile).

`responses` with nothing registered makes any HTTP attempt raise, which enforces the
"rejected before any network call" cases.
"""
from __future__ import annotations

import json
from pathlib import Path

import pytest
import responses as responses_lib

from changelog_publisher import sync
from changelog_publisher.cli import main
from changelog_publisher.client import BASE_URL, PUBLISH_ACTION_ID

_FIXTURES = Path(__file__).parent / "fixtures"
_SEARCH_URL = f"{BASE_URL}/api/content/_search"
_FIRE_URL = f"{BASE_URL}/api/v1/workflow/actions/{PUBLISH_ACTION_ID}/fire"
_TOKEN = "test-token-SECRET"

_HUB = {
    "latest": "26.09.28-02",
    "standard": "26.09.17-02",
    "trailing": "26.09.03-01",
    "tainted": ["26.08.28-01", "26.08.31-01"],
}


@pytest.fixture(autouse=True)
def _fast(monkeypatch):
    monkeypatch.setattr(sync, "_RETRY_DELAYS_SECONDS", (0, 0))
    monkeypatch.setattr(sync, "_READBACK_DELAY_SECONDS", 0)
    monkeypatch.setenv("DOTCMS_DEVSITE_RELEASENOTES_TOKEN", _TOKEN)


def _fixture(name: str) -> dict:
    return json.loads((_FIXTURES / name).read_text())


def _state_file(tmp_path: Path, hub=None, raw: str | None = None) -> str:
    f = tmp_path / "hub-state.json"
    f.write_text(raw if raw is not None else json.dumps(hub or _HUB))
    return str(f)


def _record(state) -> dict:
    body = _fixture("evergreen_state_hit.json")
    body["entity"]["jsonObjectView"]["contentlets"][0]["state"] = state
    return body


def _route_search(record, rows=None):
    """Route _search by query. `record` is a body or a list of bodies (successive reads,
    the last repeating); `rows` maps version -> bool (default True) for release rows."""
    rows = rows or {}
    seq = list(record) if isinstance(record, list) else [record]

    def cb(request):
        q = json.loads(request.body)["query"]
        if "EvergreenState" in q:
            body = seq.pop(0) if len(seq) > 1 else seq[0]
        else:
            version = q.split("minor_dotraw:")[1].split()[0]
            body = _fixture("search_hit.json" if rows.get(version, True) else "search_empty.json")
        return (200, {}, json.dumps(body))

    responses_lib.add_callback(responses_lib.POST, _SEARCH_URL, callback=cb,
                               content_type="application/json")


def _fires():
    return [c for c in responses_lib.calls if c.request.url == _FIRE_URL]


def _fire_body(i=0):
    return json.loads(_fires()[i].request.body)["contentlet"]


_OLD = json.loads(_fixture("evergreen_state_hit.json")["entity"]["jsonObjectView"]["contentlets"][0]["state"])
_NEW_RECORD = _record(json.dumps(_HUB))


@responses_lib.activate
def test_unchanged_makes_no_write(tmp_path, capsys):
    reordered = {**_HUB, "tainted": list(reversed(_HUB["tainted"]))}
    _route_search(_record(json.dumps(reordered)))
    assert main(["sync-site", "--state-file", _state_file(tmp_path), "--apply"]) == 0
    assert "::evergreen-sync::unchanged" in capsys.readouterr().out
    assert _fires() == []


@responses_lib.activate
def test_differs_fires_one_evergreenstate_write(tmp_path, capsys):
    _route_search([_fixture("evergreen_state_hit.json"), _NEW_RECORD])
    responses_lib.add(responses_lib.PUT, _FIRE_URL, json={}, status=200)
    assert main(["sync-site", "--state-file", _state_file(tmp_path), "--apply"]) == 0
    out = capsys.readouterr()
    assert "::evergreen-sync::updated fields=latest,standard,trailing,tainted" in out.out
    assert len(_fires()) == 1
    c = _fire_body()
    assert (c["contentType"], c["identifier"], c["title"]) == ("EvergreenState", "evg-1", "evergreen-state")
    assert json.loads(c["state"]) == _HUB
    assert all(json.loads(x.request.body)["contentlet"]["contentType"] != "Dotcmsbuilds" for x in _fires())
    assert "warn" not in out.err.lower()


@responses_lib.activate
@pytest.mark.parametrize("state", [dict(_OLD), "not json", ""])
def test_object_and_garbage_record_state(tmp_path, state):
    _route_search(_record(state))
    responses_lib.add(responses_lib.PUT, _FIRE_URL, json={}, status=200)
    assert main(["sync-site", "--state-file", _state_file(tmp_path), "--apply"]) == 0
    assert json.loads(_fire_body()["state"]) == _HUB


@responses_lib.activate
def test_dry_run_prints_diff_and_does_not_fire(tmp_path, capsys):
    _route_search(_fixture("evergreen_state_hit.json"))
    assert main(["sync-site", "--state-file", _state_file(tmp_path)]) == 0
    out = capsys.readouterr().out
    assert "desired: {" in out
    assert "standard: 26.09.03-01 -> 26.09.17-02" in out
    assert "tainted: +26.08.31-01 -" in out
    assert "::evergreen-sync::would-update fields=latest,standard,trailing,tainted" in out
    assert _fires() == []


@responses_lib.activate
def test_missing_row_keeps_track_and_exits_3(tmp_path, capsys):
    _route_search([_fixture("evergreen_state_hit.json"), _NEW_RECORD], rows={_HUB["latest"]: False})
    responses_lib.add(responses_lib.PUT, _FIRE_URL, json={}, status=200)
    assert main(["sync-site", "--state-file", _state_file(tmp_path), "--apply"]) == 3
    assert "::evergreen-sync-missing-row::latest=26.09.28-02" in capsys.readouterr().out
    written = json.loads(_fire_body()["state"])
    assert written["latest"] == _OLD["latest"]
    assert written["standard"] == _HUB["standard"] and written["tainted"] == _HUB["tainted"]


@responses_lib.activate
def test_missing_row_without_previous_value_omits_key(tmp_path):
    old = {k: v for k, v in _OLD.items() if k != "latest"}
    _route_search(_record(json.dumps(old)), rows={_HUB["latest"]: False})
    responses_lib.add(responses_lib.PUT, _FIRE_URL, json={}, status=200)
    assert main(["sync-site", "--state-file", _state_file(tmp_path), "--apply"]) == 3
    assert "latest" not in json.loads(_fire_body()["state"])


@responses_lib.activate
def test_401_retries_three_times_and_hides_token(tmp_path, capsys):
    _route_search(_fixture("evergreen_state_hit.json"))
    responses_lib.add(responses_lib.PUT, _FIRE_URL, json={}, status=401)
    assert main(["sync-site", "--state-file", _state_file(tmp_path), "--apply"]) == 1
    out = capsys.readouterr()
    assert len(_fires()) == 3
    assert "::evergreen-sync-error::" in out.out
    assert _TOKEN not in out.out and _TOKEN not in out.err


@responses_lib.activate
def test_record_not_found(tmp_path, capsys):
    _route_search(_fixture("search_empty.json"))
    assert main(["sync-site", "--state-file", _state_file(tmp_path), "--apply"]) == 1
    out = capsys.readouterr().out
    assert "::evergreen-sync-error::" in out and "FR-044" in out
    n = len(responses_lib.calls)
    assert n == 3
    responses_lib.calls.reset()
    assert main(["sync-site", "--state-file", _state_file(tmp_path)]) == 1
    assert len(responses_lib.calls) == 1


@responses_lib.activate
def test_two_records_is_an_error(tmp_path):
    _route_search(_fixture("evergreen_state_two_hits.json"))
    assert main(["sync-site", "--state-file", _state_file(tmp_path)]) == 1


@responses_lib.activate
@pytest.mark.parametrize("raw", [
    "not json",
    json.dumps({k: v for k, v in _HUB.items() if k != "trailing"}),
    json.dumps({**_HUB, "latest": "26.09.28_lts_v1"}),
    json.dumps({**_HUB, "tainted": "26.08.28-01"}),
])
def test_invalid_state_file_rejected_before_http(tmp_path, raw):
    assert main(["sync-site", "--state-file", _state_file(tmp_path, raw=raw)]) == 2
    assert len(responses_lib.calls) == 0


@responses_lib.activate
def test_transient_500_then_success(tmp_path, capsys):
    old = _fixture("evergreen_state_hit.json")
    _route_search([old, old, _NEW_RECORD])  # attempt 1 read, attempt 2 read, read-back
    responses_lib.add(responses_lib.PUT, _FIRE_URL, json={}, status=500)
    responses_lib.add(responses_lib.PUT, _FIRE_URL, json={}, status=200)
    assert main(["sync-site", "--state-file", _state_file(tmp_path), "--apply"]) == 0
    assert "::evergreen-sync::updated" in capsys.readouterr().out
    assert len(_fires()) == 2
