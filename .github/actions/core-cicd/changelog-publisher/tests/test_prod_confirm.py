"""Tests for confirming that writes reach prod.

Writes go to auth and the workflow action pushes them to prod asynchronously, so a 200
from auth says nothing about prod. After each write the tool polls prod for the inode it
just wrote (a push keeps the inode), and the daily sync compares prod as well as auth so
a push that failed is re-fired on the next run.

Auth and prod are mocked on separate base URLs. The conftest fixture that turns prod
confirmation off for the other suites is undone here with `_prod_enabled`.
"""
from __future__ import annotations

import json
from pathlib import Path

import pytest
import responses as responses_lib

from changelog_publisher import cli, client as client_mod, publisher, sync
from changelog_publisher.cli import main
from changelog_publisher.client import BASE_URL, PUBLISH_ACTION_ID, CorpsitesClient

_FIXTURES = Path(__file__).parent / "fixtures"
_PROD_URL = "https://prod.test"
_AUTH_SEARCH = f"{BASE_URL}/api/content/_search"
_PROD_SEARCH = f"{_PROD_URL}/api/content/_search"
_FIRE_URL = f"{BASE_URL}/api/v1/workflow/actions/{PUBLISH_ACTION_ID}/fire"
_NEW_INODE = "inode-new-1"

_HUB = {
    "latest": "26.09.28-02",
    "standard": "26.09.17-02",
    "trailing": "26.09.03-01",
    "tainted": ["26.08.28-01", "26.08.31-01"],
}


@pytest.fixture(autouse=True)
def _prod_enabled(monkeypatch):
    monkeypatch.setenv("DOTCMS_DEVSITE_RELEASENOTES_TOKEN", "auth-token")
    monkeypatch.setenv("DOTCMS_DEVSITE_PROD_URL", _PROD_URL)
    monkeypatch.setenv("DOTCMS_DEVSITE_PROD_RELEASENOTES_TOKEN", "prod-token")
    monkeypatch.setattr(cli, "prod_reader", client_mod.prod_reader)
    monkeypatch.setattr(sync, "_RETRY_DELAYS_SECONDS", (0, 0))
    monkeypatch.setattr(sync, "_READBACK_DELAY_SECONDS", 0)


def _fixture(name: str) -> dict:
    return json.loads((_FIXTURES / name).read_text())


def _body(contentlets: list[dict]) -> dict:
    return {"entity": {"jsonObjectView": {"contentlets": contentlets}}}


def _record(state: dict, inode: str = "inode-old", identifier: str = "evg-1") -> dict:
    return {"contentType": "EvergreenState", "identifier": identifier, "inode": inode,
            "title": "evergreen-state", "state": json.dumps(state, separators=(",", ":"))}


def _route(url: str, *, record=None, rows=None, live: list[bool] | None = None):
    """Route a `_search` endpoint by query: the EvergreenState record, release rows
    (version -> bool, default True) and `+inode:` liveness checks, which answer from
    `live` in order (the last value repeating)."""
    rows = rows or {}
    live = list(live or [True])

    def cb(request):
        q = json.loads(request.body)["query"]
        if "+inode:" in q:
            hit = live.pop(0) if len(live) > 1 else live[0]
            return 200, {}, json.dumps(_body([{"inode": _NEW_INODE}] if hit else []))
        if "EvergreenState" in q:
            return 200, {}, json.dumps(_body([record] if record else []))
        version = q.split("minor_dotraw:")[1].split()[0]
        return 200, {}, json.dumps(_fixture(
            "search_hit_automation.json" if rows.get(version, True) else "search_empty.json"))

    responses_lib.add_callback(responses_lib.POST, url, callback=cb,
                               content_type="application/json")


def _add_fire() -> None:
    responses_lib.add(responses_lib.PUT, _FIRE_URL, json={"entity": {"inode": _NEW_INODE}},
                      status=200)


def _prod() -> CorpsitesClient:
    return client_mod.prod_reader()


def _state_file(tmp_path: Path) -> str:
    f = tmp_path / "hub-state.json"
    f.write_text(json.dumps(_HUB))
    return str(f)


# ---------------------------------------------------------------------------
# client: the read-only prod reader, the fired inode and the liveness check
# ---------------------------------------------------------------------------

@responses_lib.activate
def test_fire_returns_the_written_inode():
    _add_fire()
    inode = CorpsitesClient(token="t").fire({"contentType": "Dotcmsbuilds"}, apply=True)
    assert inode == _NEW_INODE


def test_dry_run_fire_returns_none():
    assert CorpsitesClient(token="t").fire({"contentType": "Dotcmsbuilds"}, apply=False) is None


@pytest.mark.parametrize("unset", ["DOTCMS_DEVSITE_PROD_URL", "DOTCMS_DEVSITE_PROD_RELEASENOTES_TOKEN"])
def test_prod_reader_requires_url_and_token(monkeypatch, unset):
    monkeypatch.setenv(unset, "")
    with pytest.raises(RuntimeError, match=f"{unset} is not set"):
        client_mod.prod_reader()


def test_prod_reader_does_not_need_an_action(monkeypatch):
    """The prod reader never fires, so it must not depend on the action variable."""
    monkeypatch.setattr(client_mod, "PUBLISH_ACTION_ID", "")
    assert client_mod.prod_reader().base_url == _PROD_URL


@responses_lib.activate
def test_prod_reader_refuses_to_fire():
    with pytest.raises(RuntimeError, match="read-only"):
        _prod().fire({"contentType": "Dotcmsbuilds"}, apply=True)
    assert len(responses_lib.calls) == 0


@responses_lib.activate
def test_is_live_queries_prod_by_inode_with_the_prod_token():
    _route(_PROD_SEARCH, live=[True])
    assert _prod().is_live(_NEW_INODE) is True
    call = responses_lib.calls[0]
    assert call.request.url == _PROD_SEARCH
    assert json.loads(call.request.body) == {"query": f"+inode:{_NEW_INODE} +live:true", "limit": 1}
    assert call.request.headers["Authorization"] == "Bearer prod-token"


def test_wait_for_prod_returns_once_the_check_passes():
    answers = iter([False, False, True])
    client_mod.wait_for_prod(lambda: next(answers), "thing")


def test_wait_for_prod_gives_up_after_five_minutes():
    """10 checks, 30 s apart: the 5-minute bound (pushes normally land in about a minute,
    2-3 at worst). Delay is zeroed by conftest, so the attempt count is what is asserted."""
    assert client_mod.PROD_POLL_ATTEMPTS * 30 == 300
    calls = []
    with pytest.raises(RuntimeError, match="thing did not reach prod within 300s"):
        client_mod.wait_for_prod(lambda: calls.append(1) or False, "thing")
    assert len(calls) == client_mod.PROD_POLL_ATTEMPTS


# ---------------------------------------------------------------------------
# publish: every write waits for its inode on prod
# ---------------------------------------------------------------------------

def _publish(client, prod, **overrides):
    kwargs = dict(version="26.07.10-01", release_notes="### Fixes\n- x\n",
                  docker_image="dotcms/dotcms:26.07.10-01_abc1234",
                  released_date="2026-07-16", eol_date="2027-07-16",
                  service_account="user-devbot-0000", apply=True, prod=prod)
    kwargs.update(overrides)
    return publisher.publish(client, **kwargs)


@responses_lib.activate
def test_create_waits_for_the_new_inode_on_prod():
    responses_lib.add(responses_lib.POST, _AUTH_SEARCH, json=_fixture("search_empty.json"))
    responses_lib.add(responses_lib.POST, _AUTH_SEARCH, json=_fixture("search_hit.json"))
    _add_fire()
    _route(_PROD_SEARCH, rows={"26.07.10-01": False}, live=[False, True])

    result = _publish(CorpsitesClient(), _prod())

    assert result.status == "created"
    inode_checks = [json.loads(c.request.body)["query"] for c in responses_lib.calls
                    if c.request.url == _PROD_SEARCH
                    and "+inode:" in json.loads(c.request.body)["query"]]
    assert inode_checks == [f"+inode:{_NEW_INODE} +live:true"] * 2


@responses_lib.activate
def test_update_waits_for_the_new_inode_on_prod():
    responses_lib.add(responses_lib.POST, _AUTH_SEARCH, json=_fixture("search_hit_automation.json"))
    _add_fire()
    _route(_PROD_SEARCH, live=[True])

    result = _publish(CorpsitesClient(), _prod())

    assert result.status == "updated"
    inode_checks = [c for c in responses_lib.calls if c.request.url == _PROD_SEARCH
                    and "+inode:" in json.loads(c.request.body)["query"]]
    assert len(inode_checks) == 1


@responses_lib.activate
def test_push_that_never_lands_is_a_failure():
    responses_lib.add(responses_lib.POST, _AUTH_SEARCH, json=_fixture("search_hit_automation.json"))
    _add_fire()
    _route(_PROD_SEARCH, live=[False])

    with pytest.raises(RuntimeError, match="changelog entry 26.07.10-01 did not reach prod"):
        _publish(CorpsitesClient(), _prod())


@responses_lib.activate
def test_dry_run_publish_never_touches_prod():
    responses_lib.add(responses_lib.POST, _AUTH_SEARCH, json=_fixture("search_hit_automation.json"))
    _route(_PROD_SEARCH)
    _publish(CorpsitesClient(), _prod(), apply=False)
    assert not [c for c in responses_lib.calls if c.request.url == _PROD_SEARCH
                and "+inode:" in json.loads(c.request.body)["query"]]


@responses_lib.activate
def test_fire_response_without_inode_is_a_failure():
    """Without the written inode there is nothing exact to look for on prod, so the run
    fails rather than reporting an unconfirmed push as success."""
    responses_lib.add(responses_lib.POST, _AUTH_SEARCH, json=_fixture("search_hit_automation.json"))
    responses_lib.add(responses_lib.PUT, _FIRE_URL, json={"entity": {}}, status=200)
    _route(_PROD_SEARCH)

    with pytest.raises(RuntimeError, match="no inode"):
        _publish(CorpsitesClient(), _prod())
    assert not [c for c in responses_lib.calls if c.request.url == _PROD_SEARCH
                and "+inode:" in json.loads(c.request.body)["query"]]


# ---------------------------------------------------------------------------
# sync: compare prod as well as auth, check release rows on prod, confirm the push
# ---------------------------------------------------------------------------

@responses_lib.activate
def test_stale_prod_is_re_pushed_even_when_auth_matches():
    """The self-heal: a push that failed earlier leaves auth == Hub but prod stale."""
    _route(_AUTH_SEARCH, record=_record(_HUB))
    _route(_PROD_SEARCH, record=_record({**_HUB, "latest": "26.09.17-02"}), live=[True])
    _add_fire()

    result = sync.reconcile(CorpsitesClient(), _HUB, apply=True, prod=_prod())

    assert result.status == "updated"
    assert result.fields == ["latest"]
    fires = [c for c in responses_lib.calls if c.request.url == _FIRE_URL]
    assert len(fires) == 1
    assert json.loads(json.loads(fires[0].request.body)["contentlet"]["state"]) == _HUB


@responses_lib.activate
def test_auth_and_prod_both_matching_is_unchanged():
    _route(_AUTH_SEARCH, record=_record(_HUB))
    _route(_PROD_SEARCH, record=_record(_HUB))

    result = sync.reconcile(CorpsitesClient(), _HUB, apply=True, prod=_prod())

    assert result.status == "unchanged"
    assert _FIRE_URL not in [c.request.url for c in responses_lib.calls]


@responses_lib.activate
def test_dry_run_reports_stale_prod_as_would_update():
    """The drift check is a dry-run, so it now sees a prod that never caught up."""
    _route(_AUTH_SEARCH, record=_record(_HUB))
    _route(_PROD_SEARCH, record=_record({**_HUB, "tainted": []}))

    result = sync.reconcile(CorpsitesClient(), _HUB, apply=False, prod=_prod())

    assert result.status == "would-update"
    assert result.fields == ["tainted"]
    assert result.prod_current["tainted"] == []


@responses_lib.activate
def test_dry_run_cli_prints_the_prod_side_of_the_diff(tmp_path, capsys):
    _route(_AUTH_SEARCH, record=_record(_HUB))
    _route(_PROD_SEARCH, record=_record({**_HUB, "latest": "26.09.17-02"}))

    assert main(["sync-site", "--state-file", _state_file(tmp_path)]) == 0
    assert "  prod latest: 26.09.17-02 -> 26.09.28-02" in capsys.readouterr().out


@responses_lib.activate
def test_release_rows_are_checked_on_prod():
    """A row that exists on auth but has not reached prod must not advance a track."""
    old = {**_HUB, "latest": "26.09.17-02"}
    _route(_AUTH_SEARCH, record=_record(old), rows={_HUB["latest"]: True})
    _route(_PROD_SEARCH, record=_record(old), rows={_HUB["latest"]: False})

    result = sync.reconcile(CorpsitesClient(), _HUB, apply=False, prod=_prod())

    assert result.missing_rows == {"latest": _HUB["latest"]}
    assert result.desired["latest"] == "26.09.17-02"


@responses_lib.activate
def test_sync_write_waits_for_prod():
    _route(_AUTH_SEARCH, record=_record({**_HUB, "latest": "26.09.17-02"}))
    _route(_PROD_SEARCH, record=_record({**_HUB, "latest": "26.09.17-02"}), live=[False, True])
    _add_fire()

    sync.reconcile(CorpsitesClient(), _HUB, apply=True, prod=_prod())

    inode_checks = [c for c in responses_lib.calls if c.request.url == _PROD_SEARCH
                    and "+inode:" in json.loads(c.request.body)["query"]]
    assert len(inode_checks) == 2


@responses_lib.activate
def test_sync_retries_when_the_push_never_lands():
    """A push that does not land is a RuntimeError, so the existing retries re-fire it."""
    _route(_AUTH_SEARCH, record=_record({**_HUB, "latest": "26.09.17-02"}))
    _route(_PROD_SEARCH, record=_record({**_HUB, "latest": "26.09.17-02"}), live=[False])
    _add_fire()

    with pytest.raises(RuntimeError, match="EvergreenState record did not reach prod"):
        sync.sync_with_retries(CorpsitesClient(), _HUB, prod=_prod())
    assert [c.request.url for c in responses_lib.calls].count(_FIRE_URL) == 3


@responses_lib.activate
def test_more_than_one_prod_record_is_ambiguous():
    _route(_AUTH_SEARCH, record=_record(_HUB))
    two = _body([_record(_HUB), _record(_HUB)])
    responses_lib.add(responses_lib.POST, _PROD_SEARCH, json=two)

    with pytest.raises(publisher.AmbiguousMatchError, match="prod"):
        sync.reconcile(CorpsitesClient(), _HUB, apply=False, prod=_prod())


# ---------------------------------------------------------------------------
# CLI: the prod settings are required, with a clean one-line error
# ---------------------------------------------------------------------------

@pytest.mark.parametrize("unset", ["DOTCMS_DEVSITE_PROD_URL", "DOTCMS_DEVSITE_PROD_RELEASENOTES_TOKEN"])
@responses_lib.activate
def test_publish_without_prod_settings_exits_one(tmp_path, monkeypatch, capsys, caplog, unset):
    monkeypatch.setenv(unset, "")
    notes = tmp_path / "notes.md"
    notes.write_text("### Fixes\n- x\n")

    with caplog.at_level("ERROR"):
        rc = main(["publish", "--version", "26.07.10-01", "--notes-file", str(notes),
                   "--docker-image", "dotcms/dotcms:26.07.10-01_abc1234",
                   "--released-date", "2026-07-16", "--apply"])

    assert rc == 1
    out, err = capsys.readouterr()
    assert "Traceback" not in out and "Traceback" not in err
    assert any(unset in r.message for r in caplog.records)
    assert len(responses_lib.calls) == 0


@pytest.mark.parametrize("unset", ["DOTCMS_DEVSITE_PROD_URL", "DOTCMS_DEVSITE_PROD_RELEASENOTES_TOKEN"])
@responses_lib.activate
def test_sync_site_without_prod_settings_exits_one(tmp_path, monkeypatch, capsys, unset):
    monkeypatch.setenv(unset, "")
    assert main(["sync-site", "--state-file", _state_file(tmp_path)]) == 1
    assert f"::evergreen-sync-error::{unset} is not set" in capsys.readouterr().out
    assert len(responses_lib.calls) == 0


@responses_lib.activate
def test_sync_site_cli_re_pushes_stale_prod(tmp_path, capsys):
    _route(_AUTH_SEARCH, record=_record(_HUB))
    _route(_PROD_SEARCH, record=_record({**_HUB, "standard": "26.09.03-01"}), live=[True])
    _add_fire()

    assert main(["sync-site", "--state-file", _state_file(tmp_path), "--apply"]) == 0
    assert "::evergreen-sync::updated fields=standard" in capsys.readouterr().out


@responses_lib.activate
def test_missing_row_falls_back_to_prods_value_not_auths():
    """If auth already points a track at a version whose row never reached prod, keeping
    auth's value would carry that version to prod. Keep prod's value, and correct auth."""
    on_auth_only = _HUB["latest"]
    old = {**_HUB, "latest": "26.09.17-02"}
    _route(_AUTH_SEARCH, record=_record(_HUB))
    _route(_PROD_SEARCH, record=_record(old), rows={on_auth_only: False}, live=[True])
    _add_fire()

    result = sync.reconcile(CorpsitesClient(), _HUB, apply=True, prod=_prod())

    assert result.missing_rows == {"latest": on_auth_only}
    assert result.desired["latest"] == "26.09.17-02"
    fires = [c for c in responses_lib.calls if c.request.url == _FIRE_URL]
    assert json.loads(json.loads(fires[0].request.body)["contentlet"]["state"])["latest"] == "26.09.17-02"


@responses_lib.activate
def test_prod_record_is_read_live_only():
    """Prod's live version is what visitors see; a draft must not count as prod's state."""
    _route(_AUTH_SEARCH, record=_record(_HUB))
    _route(_PROD_SEARCH, record=_record(_HUB))

    sync.reconcile(CorpsitesClient(), _HUB, apply=False, prod=_prod())

    record_reads = [json.loads(c.request.body)["query"] for c in responses_lib.calls
                    if c.request.url == _PROD_SEARCH
                    and "EvergreenState" in json.loads(c.request.body)["query"]]
    assert record_reads and all(q.endswith(" +live:true") for q in record_reads)


# ---------------------------------------------------------------------------
# publish: hand edits on prod are protected, and prod/auth parity is enforced
# ---------------------------------------------------------------------------

def _prod_rows(fixture):
    """Answer prod's Dotcmsbuilds lookups with `fixture` (a fixture name or a body);
    inode checks say live."""
    body = _fixture(fixture) if isinstance(fixture, str) else fixture

    def cb(request):
        q = json.loads(request.body)["query"]
        if "+inode:" in q:
            return 200, {}, json.dumps(_body([{"inode": _NEW_INODE}]))
        return 200, {}, json.dumps(body)
    responses_lib.add_callback(responses_lib.POST, _PROD_SEARCH, callback=cb,
                               content_type="application/json")


def _hand_edited_on_prod() -> dict:
    """Prod's copy of auth's automation-owned row, last modified by a person on prod."""
    body = _fixture("search_hit_automation.json")
    row = body["entity"]["jsonObjectView"]["contentlets"][0]
    row.update(modUser="user-5a8a91c4-0cd7-4108-bece-549e466409bf", modUserName="Jamie Mauro")
    return body


@responses_lib.activate
def test_entry_hand_edited_on_prod_is_skipped():
    """Auth's copy is the automation's, but someone edited prod's: leave both alone."""
    responses_lib.add(responses_lib.POST, _AUTH_SEARCH, json=_fixture("search_hit_automation.json"))
    _prod_rows(_hand_edited_on_prod())

    result = _publish(CorpsitesClient(), _prod())

    assert result.status == "skipped"
    assert "on prod" in result.reason and "Jamie Mauro" in result.reason
    assert _FIRE_URL not in [c.request.url for c in responses_lib.calls]


@responses_lib.activate
def test_force_overrides_a_hand_edit_on_prod():
    responses_lib.add(responses_lib.POST, _AUTH_SEARCH, json=_fixture("search_hit_automation.json"))
    _prod_rows(_hand_edited_on_prod())
    _add_fire()

    assert _publish(CorpsitesClient(), _prod(), force=True).status == "updated"


@responses_lib.activate
def test_more_than_one_prod_entry_is_ambiguous():
    responses_lib.add(responses_lib.POST, _AUTH_SEARCH, json=_fixture("search_hit_automation.json"))
    _prod_rows("search_two_hits.json")

    with pytest.raises(publisher.AmbiguousMatchError, match="on prod"):
        _publish(CorpsitesClient(), _prod())
    assert _FIRE_URL not in [c.request.url for c in responses_lib.calls]


@responses_lib.activate
def test_entry_on_prod_but_missing_on_auth_is_a_parity_error():
    """Creating it on auth would push a second row with a new identifier to prod."""
    responses_lib.add(responses_lib.POST, _AUTH_SEARCH, json=_fixture("search_empty.json"))
    _prod_rows("search_hit_automation.json")

    with pytest.raises(RuntimeError, match="exists on prod but not on auth"):
        _publish(CorpsitesClient(), _prod())
    assert _FIRE_URL not in [c.request.url for c in responses_lib.calls]


# ---------------------------------------------------------------------------
# sync dry-run: report auth and prod differences side by side
# ---------------------------------------------------------------------------

@responses_lib.activate
def test_dry_run_reports_differences_on_both_sides():
    """Auth lags on `latest`, prod on `tainted`: both belong in the report."""
    _route(_AUTH_SEARCH, record=_record({**_HUB, "latest": "26.09.17-02"}))
    _route(_PROD_SEARCH, record=_record({**_HUB, "tainted": []}))

    result = sync.reconcile(CorpsitesClient(), _HUB, apply=False, prod=_prod())

    assert result.status == "would-update"
    assert result.fields == ["latest", "tainted"]
    assert result.current["latest"] == "26.09.17-02"
    assert result.prod_current["tainted"] == []


@responses_lib.activate
def test_dry_run_cli_prints_both_sides(tmp_path, capsys):
    _route(_AUTH_SEARCH, record=_record({**_HUB, "latest": "26.09.17-02"}))
    _route(_PROD_SEARCH, record=_record({**_HUB, "tainted": []}))

    assert main(["sync-site", "--state-file", _state_file(tmp_path)]) == 0
    out = capsys.readouterr().out
    assert "  latest: 26.09.17-02 -> 26.09.28-02" in out
    assert "  prod tainted: +26.08.28-01,26.08.31-01 -" in out
    assert "::evergreen-sync::would-update fields=latest,tainted" in out


# ---------------------------------------------------------------------------
# identifier parity: auth and prod must hold the same record, or a push adds a second
# ---------------------------------------------------------------------------

@responses_lib.activate
def test_changelog_entry_with_different_identifiers_is_a_parity_error():
    """search_hit.json has a different identifier from search_hit_automation.json."""
    responses_lib.add(responses_lib.POST, _AUTH_SEARCH, json=_fixture("search_hit_automation.json"))
    _prod_rows("search_hit.json")

    with pytest.raises(RuntimeError, match="identifier"):
        _publish(CorpsitesClient(), _prod(), force=True)
    assert _FIRE_URL not in [c.request.url for c in responses_lib.calls]


@responses_lib.activate
def test_evergreen_state_with_different_identifiers_is_a_parity_error():
    _route(_AUTH_SEARCH, record=_record({**_HUB, "latest": "26.09.17-02"}))
    _route(_PROD_SEARCH, record=_record(_HUB, identifier="evg-other"))
    _add_fire()

    with pytest.raises(RuntimeError, match="identifier"):
        sync.reconcile(CorpsitesClient(), _HUB, apply=True, prod=_prod())
    assert _FIRE_URL not in [c.request.url for c in responses_lib.calls]


@responses_lib.activate
def test_missing_row_with_no_prod_value_is_an_error_not_a_dropped_track():
    """Prod has no live record to fall back on: writing the state without the track
    would drop it from the site, so stop instead."""
    _route(_AUTH_SEARCH, record=_record({**_HUB, "latest": "26.09.17-02"}))
    _route(_PROD_SEARCH, record=None, rows={_HUB["latest"]: False})
    _add_fire()

    with pytest.raises(RuntimeError, match="latest"):
        sync.reconcile(CorpsitesClient(), _HUB, apply=True, prod=_prod())
    assert _FIRE_URL not in [c.request.url for c in responses_lib.calls]


@responses_lib.activate
def test_record_missing_on_prod_is_created_by_the_push_when_no_row_is_missing():
    """With every row on prod, a record absent from prod is simply pushed there."""
    _route(_AUTH_SEARCH, record=_record(_HUB))
    _route(_PROD_SEARCH, record=None, live=[True])
    _add_fire()

    assert sync.reconcile(CorpsitesClient(), _HUB, apply=True, prod=_prod()).status == "updated"


@responses_lib.activate
def test_identifier_mismatch_is_not_retried():
    """Like a duplicate record, a parity break cannot fix itself, so the retries (60 s,
    then 180 s) would only delay the failure."""
    _route(_AUTH_SEARCH, record=_record({**_HUB, "latest": "26.09.17-02"}))
    _route(_PROD_SEARCH, record=_record(_HUB, identifier="evg-other"))

    with pytest.raises(sync.ParityError):
        sync.sync_with_retries(CorpsitesClient(), _HUB, prod=_prod())
    record_reads = [c for c in responses_lib.calls if c.request.url == _AUTH_SEARCH
                    and "EvergreenState" in json.loads(c.request.body)["query"]]
    assert len(record_reads) == 1
