from evergreen_tracks.markers import (
    TRACKS, taint_tag, hold_tag,
    tainted_versions, held_tracks,
)

def test_tag_name_helpers():
    assert taint_tag("26.06.11-01") == "26.06.11-01_tainted"
    assert hold_tag("standard") == "standard_hold"

def test_tainted_versions_extracted():
    tags = ["26.06.11-01", "26.06.11-01_tainted", "26.05.01-01", "latest"]
    assert tainted_versions(tags) == {"26.06.11-01"}

def test_held_tracks_extracted():
    tags = ["standard", "standard_hold", "trailing", "latest_hold"]
    assert held_tracks(tags) == {"standard", "latest"}

def test_held_tracks_ignores_unknown_hold():
    # a _hold suffix on a non-track name is ignored
    tags = ["bogus_hold", "standard_hold"]
    assert held_tracks(tags) == {"standard"}
