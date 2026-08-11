"""Read side: list tags and digests from the Docker Hub API.

Reads are authenticated when DOCKER_USERNAME/DOCKER_TOKEN are set. Hub refuses
ANONYMOUS pagination past offset 1000 ("pagination offset too large for anonymous
requests; sign in to page further"), and dotcms/dotcms is at ~7.8k tags / 79 pages,
so an unauthenticated walk 403s on page 11. Auth is therefore required in practice
for the real repos; it stays optional so tests and small repos need no creds.
"""
from __future__ import annotations

import os
from dataclasses import dataclass

import requests

from .executor import hub_login

_HUB = "https://hub.docker.com/v2"
_TIMEOUT = 30


@dataclass(frozen=True)
class Tag:
    name: str
    digest: str


def _digest_of(result: dict) -> str | None:
    if result.get("digest"):
        return result["digest"]
    images = result.get("images") or []
    if images and images[0].get("digest"):
        return images[0]["digest"]
    return None


def _auth_headers() -> dict[str, str]:
    """Hub JWT header, or {} when no creds are in the environment."""
    username = os.environ.get("DOCKER_USERNAME")
    password = os.environ.get("DOCKER_TOKEN")
    if not username or not password:
        return {}
    return {"Authorization": f"JWT {hub_login(username, password)}"}


def list_tags(repo: str) -> list[Tag]:
    """All tags in the repo with their manifest digests, following pagination."""
    namespace, name = repo.split("/", 1)
    url = f"{_HUB}/namespaces/{namespace}/repositories/{name}/tags?page_size=100"
    # One login per walk, not per page: the JWT outlives a full 79-page listing.
    headers = _auth_headers()
    out: list[Tag] = []
    while url:
        resp = requests.get(url, headers=headers, timeout=_TIMEOUT)
        if resp.status_code == 403 and not headers:
            raise RuntimeError(
                f"Docker Hub refused anonymous pagination of {repo} at {url}. "
                "Set DOCKER_USERNAME and DOCKER_TOKEN — Hub caps anonymous "
                "requests at offset 1000 and this repo has more tags than that."
            )
        resp.raise_for_status()
        body = resp.json()
        for result in body.get("results", []):
            digest = _digest_of(result)
            if result.get("name") and digest:
                out.append(Tag(name=result["name"], digest=digest))
        url = body.get("next")
    return out
