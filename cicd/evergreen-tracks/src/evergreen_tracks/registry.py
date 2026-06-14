"""Read side: list tags and digests from the Docker Hub API. Public repos need no auth."""
from __future__ import annotations

from dataclasses import dataclass

import requests

_HUB = "https://hub.docker.com/v2"
_TIMEOUT = 30


@dataclass(frozen=True)
class Tag:
    name: str
    digest: str


def _split_repo(repo: str) -> tuple[str, str]:
    namespace, name = repo.split("/", 1)
    return namespace, name


def _digest_of(result: dict) -> str | None:
    if result.get("digest"):
        return result["digest"]
    images = result.get("images") or []
    if images and images[0].get("digest"):
        return images[0]["digest"]
    return None


def list_tags(repo: str) -> list[Tag]:
    """All tags in the repo with their manifest digests, following pagination."""
    namespace, name = _split_repo(repo)
    url = f"{_HUB}/namespaces/{namespace}/repositories/{name}/tags?page_size=100"
    out: list[Tag] = []
    while url:
        resp = requests.get(url, timeout=_TIMEOUT)
        resp.raise_for_status()
        body = resp.json()
        for result in body.get("results", []):
            digest = _digest_of(result)
            if result.get("name") and digest:
                out.append(Tag(name=result["name"], digest=digest))
        url = body.get("next")
    return out


def tag_digests(repo: str) -> dict[str, str]:
    """Map of tag name -> manifest digest."""
    return {t.name: t.digest for t in list_tags(repo)}
