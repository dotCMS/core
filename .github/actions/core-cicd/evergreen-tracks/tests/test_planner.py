import datetime as dt
from evergreen_tracks.calver import parse_release
from evergreen_tracks.planner import TrackState, Move, plan

TODAY = dt.date(2026, 6, 30)

def releases(*tags):
    return [parse_release(t) for t in tags]

REL = releases(
    "26.06.29-01",  # 1 day old
    "26.06.16-01",  # 14 days old
    "26.06.02-01",  # 28 days old
    "26.05.20-01",  # 41 days old
)

def track(name, days, current):
    return TrackState(name=name, threshold_days=days, current_version=current)

def test_standard_picks_newest_at_least_14_days_old():
    moves = plan(REL, tainted=set(), held=set(),
                 tracks=[track("standard", 14, current="26.06.02-01")], today=TODAY)
    assert moves == [Move(track="standard", target_version="26.06.16-01")]

def test_trailing_picks_newest_at_least_28_days_old():
    moves = plan(REL, tainted=set(), held=set(),
                 tracks=[track("trailing", 28, current="26.05.20-01")], today=TODAY)
    assert moves == [Move(track="trailing", target_version="26.06.02-01")]

def test_no_move_when_already_on_target():
    moves = plan(REL, tainted=set(), held=set(),
                 tracks=[track("standard", 14, current="26.06.16-01")], today=TODAY)
    assert moves == []

def test_seed_when_track_missing():
    moves = plan(REL, tainted=set(), held=set(),
                 tracks=[track("standard", 14, current=None)], today=TODAY)
    assert moves == [Move(track="standard", target_version="26.06.16-01")]

def test_monotonic_never_moves_backward():
    # current is already newer than the newest 28-day-eligible release
    moves = plan(REL, tainted=set(), held=set(),
                 tracks=[track("trailing", 28, current="26.06.16-01")], today=TODAY)
    assert moves == []

def test_taint_skips_to_next_eligible():
    # 26.06.16-01 would be standard's target, but it's tainted -> next older eligible
    moves = plan(REL, tainted={"26.06.16-01"}, held=set(),
                 tracks=[track("standard", 14, current="26.05.20-01")], today=TODAY)
    assert moves == [Move(track="standard", target_version="26.06.02-01")]

def test_track_on_tainted_release_stays_when_no_newer_eligible():
    # current is tainted, nothing newer is eligible+clean -> stay put (no move, no rollback)
    moves = plan(releases("26.06.16-01"), tainted={"26.06.16-01"}, held=set(),
                 tracks=[track("standard", 14, current="26.06.16-01")], today=TODAY)
    assert moves == []

def test_held_track_emits_no_move():
    moves = plan(REL, tainted=set(), held={"standard"},
                 tracks=[track("standard", 14, current="26.06.02-01")], today=TODAY)
    assert moves == []

def test_latest_threshold_zero_picks_freshest():
    moves = plan(REL, tainted=set(), held=set(),
                 tracks=[track("latest", 0, current="26.06.16-01")], today=TODAY)
    assert moves == [Move(track="latest", target_version="26.06.29-01")]
