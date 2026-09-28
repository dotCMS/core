"""contracts/cli-interface.md's exit-code table: 3 (auth failure — nothing written, no
credential leak), 2 (invalid config), 1 (some entries failed), 0 (clean run)."""

import sys

import pytest

import migrate_custom_fields as mcf
from conftest import json_response, load_json


def run_main(monkeypatch, argv):
    monkeypatch.setattr(sys, "argv", ["migrate_custom_fields.py", *argv])
    with pytest.raises(SystemExit) as exc_info:
        mcf.main()
    return exc_info.value.code


def test_auth_failure_exits_3_and_writes_nothing(mock_dotcms, workdir, monkeypatch, capsys):
    monkeypatch.setattr(mcf, "DOTCMS_PASS", "sUpers3cr3t-test-only-value")

    def handler(request):
        return json_response({"message": "unauthorized"}, status=401)

    mock_dotcms(handler)

    code = run_main(monkeypatch, ["--workdir", str(workdir), "pull"])

    assert code == 3
    assert not workdir.exists(), "an auth failure must write nothing at all"

    captured = capsys.readouterr()
    assert "DOTCMS_USER" in captured.err
    assert "DOTCMS_PASS" in captured.err
    assert "sUpers3cr3t-test-only-value" not in captured.err
    assert "sUpers3cr3t-test-only-value" not in captured.out


def test_bad_config_exits_2_when_push_has_no_manifest(mock_dotcms, workdir, monkeypatch):
    def handler(request):
        if request.url.path == "/api/v1/contenttype":
            return json_response(load_json("contenttype_list_page1.json"))
        raise AssertionError(f"unexpected request: {request.method} {request.url}")

    mock_dotcms(handler)
    # workdir is a fresh tmp_path — no manifest.json exists yet.

    code = run_main(monkeypatch, ["--workdir", str(workdir), "push"])

    assert code == 2


def test_some_entries_failed_exits_1(seeded_push_instance, workdir, monkeypatch):
    _transport, _manifest, state = seeded_push_instance
    state.publish_status = 500  # the live publish request itself fails

    code = run_main(monkeypatch, ["--workdir", str(workdir), "push"])

    assert code == 1


def test_clean_run_exits_0(seeded_push_instance, workdir, monkeypatch):
    code = run_main(monkeypatch, ["--workdir", str(workdir), "push"])

    assert code == 0
