import json
import pathlib
import responses
from evergreen_tracks.registry import list_tags, tag_digests

FIXTURE = pathlib.Path(__file__).parent / "fixtures" / "hub_tags.json"

@responses.activate
def test_list_tags_paginates_and_returns_name_digest():
    payload = json.loads(FIXTURE.read_text())
    payload["next"] = None  # single page for the test
    responses.add(
        responses.GET,
        "https://hub.docker.com/v2/namespaces/dotcms/repositories/dotcms-test/tags",
        json=payload, status=200,
    )
    tags = list_tags("dotcms/dotcms-test")
    assert all(isinstance(t.name, str) and t.digest.startswith("sha256:") for t in tags)
    assert "26.03.12-01" in {t.name for t in tags}

@responses.activate
def test_tag_digests_maps_name_to_digest():
    payload = json.loads(FIXTURE.read_text())
    payload["next"] = None
    responses.add(
        responses.GET,
        "https://hub.docker.com/v2/namespaces/dotcms/repositories/dotcms-test/tags",
        json=payload, status=200,
    )
    digests = tag_digests("dotcms/dotcms-test")
    assert digests["26.03.12-01"].startswith("sha256:")
