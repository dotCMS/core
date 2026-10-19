"""Tests for `state` and `release-title` (hub snapshot, as-if preview, title transform).

Registry I/O is patched out so tests run offline.
"""
from __future__ import annotations

import json
from unittest.mock import patch

import pytest
import requests

from evergreen_tracks.cli import as_if, hub_state, main
from evergreen_tracks.registry import Tag

_EXPECTED = {
    "latest": "26.09.28-02",
    "standard": "26.09.17-02",
    "trailing": "26.09.03-01",
    "tainted": ["26.08.28-01", "26.08.31-01"],
}


def _tags(drop: tuple[str, ...] = (), trailing: str = "sha:c") -> list[Tag]:
    pairs = [
        ("26.08.20-01", "sha:t"), ("26.08.28-01", "sha:x"), ("26.08.31-01", "sha:y"),
        ("26.09.03-01", "sha:c"), ("26.09.17-02", "sha:s"), ("26.09.28-02", "sha:l"),
        ("latest", "sha:l"), ("standard", "sha:s"), ("trailing", trailing),
        ("26.08.31-01_tainted", "sha:y"), ("26.08.28-01_tainted", "sha:x"),
        ("standard_hold", "sha:s"), ("foo_tainted", "sha:z"),
    ]
    return [Tag(n, d) for n, d in pairs if n not in drop]


def _run(tmp_path, *extra):
    out = tmp_path / "hub-state.json"
    rc = main(["state", "--repo", "dotcms/dotcms", "--out", str(out), *extra])
    return rc, out


def test_hub_state_snapshot():
    with patch("evergreen_tracks.cli.list_tags", return_value=_tags()):
        assert hub_state("dotcms/dotcms") == _EXPECTED


def test_state_writes_file(tmp_path):
    with patch("evergreen_tracks.cli.list_tags", return_value=_tags()):
        rc, out = _run(tmp_path)
    assert rc == 0
    assert json.loads(out.read_text()) == _EXPECTED


@pytest.mark.parametrize("tags", [
    _tags(drop=("standard",)),          # track tag missing
    _tags(trailing="sha:nomatch"),      # digest matches no GA tag
])
def test_state_incomplete_read_writes_nothing(tmp_path, tags):
    with patch("evergreen_tracks.cli.list_tags", return_value=tags):
        rc, out = _run(tmp_path)
    assert rc == 1
    assert not out.exists()


def test_state_hub_error_writes_nothing(tmp_path):
    with patch("evergreen_tracks.cli.list_tags", side_effect=requests.HTTPError("503")):
        rc, out = _run(tmp_path)
    assert rc == 1
    assert not out.exists()


def test_as_if_pure():
    s = dict(_EXPECTED)
    assert as_if(s, "taint", "26.07.01-01", "")["tainted"] == ["26.07.01-01", *_EXPECTED["tainted"]]
    assert as_if(s, "taint", "26.08.28-01", "")["tainted"] == _EXPECTED["tainted"]
    assert as_if(s, "untaint", "26.08.28-01", "")["tainted"] == ["26.08.31-01"]
    assert as_if(s, "untaint", "26.01.01-01", "")["tainted"] == _EXPECTED["tainted"]
    assert as_if(s, "hold", "26.08.20-01", "standard")["standard"] == "26.08.20-01"
    assert as_if(s, "release-hold", "", "standard") == _EXPECTED
    assert s == _EXPECTED  # input not mutated


def test_state_as_if_taint(tmp_path):
    with patch("evergreen_tracks.cli.list_tags", return_value=_tags()):
        rc, out = _run(tmp_path, "--as-if-action", "taint", "--as-if-version", "26.07.01-01")
    assert rc == 0
    assert "26.07.01-01" in json.loads(out.read_text())["tainted"]


@pytest.mark.parametrize("extra", [
    ["--as-if-action", "taint"],
    ["--as-if-action", "taint", "--as-if-version", "26.07.01_lts_v1"],
    ["--as-if-action", "hold", "--as-if-version", "26.07.01-01", "--as-if-track", "bogus"],
    ["--as-if-action", "hold", "--as-if-version", "26.07.01-01"],
])
def test_state_as_if_usage_errors_make_no_hub_call(tmp_path, extra):
    with patch("evergreen_tracks.cli.list_tags") as lt:
        rc, out = _run(tmp_path, *extra)
    assert rc == 2
    lt.assert_not_called()
    assert not out.exists()


def test_release_title_prints_only_title(capsys):
    rc = main(["release-title", "--action", "taint", "--title", "Release 26.08.31-01"])
    assert rc == 0
    assert capsys.readouterr().out == "⚠️ TAINTED Release 26.08.31-01\n"
