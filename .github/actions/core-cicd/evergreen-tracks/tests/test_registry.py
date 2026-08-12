import json
import pathlib
import pytest
import responses
from evergreen_tracks.registry import list_tags

TAGS_URL = "https://hub.docker.com/v2/namespaces/dotcms/repositories/dotcms-test/tags"


@pytest.fixture(autouse=True)
def _no_ambient_hub_creds(monkeypatch):
    """Neutralise DOCKER_USERNAME / DOCKER_TOKEN inherited from the shell.

    `list_tags` reads them to decide whether to authenticate, so a developer or runner
    with Docker creds exported would send the anonymous-path tests through a real
    `hub_login()` POST that `responses` never registered — turning a deterministic test
    into an environment-dependent one. Tests that WANT auth set the vars themselves.
    """
    monkeypatch.delenv("DOCKER_USERNAME", raising=False)
    monkeypatch.delenv("DOCKER_TOKEN", raising=False)


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
def test_list_tags_authenticates_when_creds_present(monkeypatch):
    """With creds set, every page request carries the Hub JWT — anonymous walks 403 past
    offset 1000 on the real repos (#37025), so losing this header re-breaks promotion."""
    monkeypatch.setenv("DOCKER_USERNAME", "bot")
    monkeypatch.setenv("DOCKER_TOKEN", "pat")
    responses.add(responses.POST, "https://hub.docker.com/v2/users/login",
                  json={"token": "jwt-abc"}, status=200)
    fixture_data = json.loads(FIXTURE.read_text())
    responses.add(responses.GET, TAGS_URL,
                  json={"count": 1, "next": None, "results": fixture_data["results"][:1]},
                  status=200)

    list_tags("dotcms/dotcms-test")

    tag_calls = [c for c in responses.calls if c.request.url.startswith(TAGS_URL)]
    assert tag_calls, "expected at least one tag-listing request"
    assert all(c.request.headers.get("Authorization") == "JWT jwt-abc" for c in tag_calls)


@responses.activate
def test_list_tags_anonymous_403_explains_itself():
    """A creds-less 403 must name the cause, not surface a bare HTTPError traceback.

    It must quote Hub's OWN reason: the offset cap is the only 403 seen here today, so
    hardcoding that cause would silently mislabel any future one.
    """
    responses.add(
        responses.GET, TAGS_URL,
        json={"message": "pagination offset too large for anonymous requests"}, status=403,
    )
    with pytest.raises(RuntimeError, match="DOCKER_USERNAME") as err:
        list_tags("dotcms/dotcms-test")
    assert "pagination offset too large for anonymous requests" in str(err.value)


@responses.activate
def test_list_tags_403_without_a_json_body_still_raises_cleanly():
    """Hub 403s with an HTML/empty body must not turn into a JSON decode error."""
    responses.add(responses.GET, TAGS_URL, body="<html>gateway</html>", status=403)
    with pytest.raises(RuntimeError, match="HTTP 403"):
        list_tags("dotcms/dotcms-test")
