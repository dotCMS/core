import json
import pathlib
import responses
from evergreen_tracks.registry import list_tags, tag_digests

FIXTURE = pathlib.Path(__file__).parent / "fixtures" / "hub_tags.json"

@responses.activate
def test_list_tags_paginates_and_returns_name_digest():
    """Two-page pagination: first response has 'next' pointing to page 2, second has next=null."""
    fixture_data = json.loads(FIXTURE.read_text())
    # Page 1: single result with next pointing to page 2
    page1 = {
        "count": 2,
        "next": "https://hub.docker.com/v2/namespaces/dotcms/repositories/dotcms-test/tags?page=2&page_size=100",
        "previous": None,
        "results": [fixture_data["results"][0]],  # 26.03.12-01
    }
    # Page 2: single result with next=null
    page2 = {
        "count": 2,
        "next": None,
        "previous": "https://hub.docker.com/v2/namespaces/dotcms/repositories/dotcms-test/tags?page=1&page_size=100",
        "results": [fixture_data["results"][1]],  # 26049-docker-build-and-publish
    }
    responses.add(
        responses.GET,
        "https://hub.docker.com/v2/namespaces/dotcms/repositories/dotcms-test/tags",
        json=page1, status=200,
    )
    responses.add(
        responses.GET,
        "https://hub.docker.com/v2/namespaces/dotcms/repositories/dotcms-test/tags",
        json=page2, status=200,
    )
    tags = list_tags("dotcms/dotcms-test")
    tag_names = {t.name for t in tags}
    # Tags from both pages must appear
    assert "26.03.12-01" in tag_names
    assert "26049-docker-build-and-publish" in tag_names
    assert all(isinstance(t.name, str) and t.digest.startswith("sha256:") for t in tags)

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
