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


def _record(state: dict, inode: str = "inode-old") -> dict:
    return {"contentType": "EvergreenState", "identifier": "evg-1", "inode": inode,
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
        return 200, {}, json.dumps(
            _fixture("search_hit.json" if rows.get(version, True) else "search_empty.json"))

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
    _route(_PROD_SEARCH, live=[False, True])

    result = _publish(CorpsitesClient(), _prod())

    assert result.status == "created"
    prod_checks = [json.loads(c.request.body)["query"] for c in responses_lib.calls
                   if c.request.url == _PROD_SEARCH]
    assert prod_checks == [f"+inode:{_NEW_INODE} +live:true"] * 2


@responses_lib.activate
def test_update_waits_for_the_new_inode_on_prod():
    responses_lib.add(responses_lib.POST, _AUTH_SEARCH, json=_fixture("search_hit_automation.json"))
    _add_fire()
    _route(_PROD_SEARCH, live=[True])

    result = _publish(CorpsitesClient(), _prod())

    assert result.status == "updated"
    assert [c.request.url for c in responses_lib.calls].count(_PROD_SEARCH) == 1


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
    _publish(CorpsitesClient(), _prod(), apply=False)
    assert _PROD_SEARCH not in [c.request.url for c in responses_lib.calls]


@responses_lib.activate
def test_fire_response_without_inode_is_a_failure():
    """Without the written inode there is nothing exact to look for on prod, so the run
    fails rather than reporting an unconfirmed push as success."""
    responses_lib.add(responses_lib.POST, _AUTH_SEARCH, json=_fixture("search_hit_automation.json"))
    responses_lib.add(responses_lib.PUT, _FIRE_URL, json={"entity": {}}, status=200)

    with pytest.raises(RuntimeError, match="no inode"):
        _publish(CorpsitesClient(), _prod())
    assert _PROD_SEARCH not in [c.request.url for c in responses_lib.calls]


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
    assert result.current["tainted"] == []  # the diff shows prod's stale value, not auth's


@responses_lib.activate
def test_dry_run_cli_prints_the_prod_side_of_the_diff(tmp_path, capsys):
    _route(_AUTH_SEARCH, record=_record(_HUB))
    _route(_PROD_SEARCH, record=_record({**_HUB, "latest": "26.09.17-02"}))

    assert main(["sync-site", "--state-file", _state_file(tmp_path)]) == 0
    assert "  latest: 26.09.17-02 -> 26.09.28-02" in capsys.readouterr().out


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
