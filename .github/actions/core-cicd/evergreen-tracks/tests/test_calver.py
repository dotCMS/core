import datetime as dt
import pytest
from evergreen_tracks.calver import Release, parse_release, age_days, newest

def test_parse_ga_release():
    r = parse_release("26.06.11-01")
    assert r == Release(version="26.06.11-01", date=dt.date(2026, 6, 11), build=1)

def test_parse_rejects_non_ga():
    for tag in ["latest", "standard", "1.2.3-SNAPSHOT",
                "25.07.10_lts_v12", "26.06.11-01_java25", "26.06.11-01_tainted"]:
        assert parse_release(tag) is None

def test_age_days():
    r = parse_release("26.06.01-01")
    assert age_days(r, today=dt.date(2026, 6, 15)) == 14

def test_newest_breaks_tie_by_build():
    a = parse_release("26.06.11-01")
    b = parse_release("26.06.11-02")
    assert newest([a, b]) == b

def test_newest_prefers_later_date():
    a = parse_release("26.06.11-09")
    b = parse_release("26.06.12-01")
    assert newest([a, b]) == b

def test_newest_empty_is_none():
    assert newest([]) is None
