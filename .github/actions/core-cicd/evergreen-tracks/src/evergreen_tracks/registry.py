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
from functools import lru_cache

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


def _hub_message(resp: requests.Response) -> str:
    """Hub's own error text, when the body carries one."""
    try:
        return (resp.json() or {}).get("message") or ""
    except ValueError:
        return ""


@lru_cache(maxsize=4)
def _jwt(username: str, password: str) -> str:
    """Cached per credential pair: one login per process, not one per request."""
    return hub_login(username, password)


def _auth_headers() -> dict[str, str]:
    """Hub JWT header, or {} when no creds are in the environment."""
    username = os.environ.get("DOCKER_USERNAME")
    password = os.environ.get("DOCKER_TOKEN")
    if not username or not password:
        return {}
    return {"Authorization": f"JWT {_jwt(username, password)}"}


def list_tags(repo: str, *, name_filter: str = "") -> list[Tag]:
    """Tags in the repo with their manifest digests, following pagination.

    `name_filter` applies Hub's SUBSTRING filter (`?name=`), which is what keeps the
    common path off the ~79-page full listing. It is observed behaviour rather than a
    documented contract; if Hub ever drops it the filtered queries simply return the
    whole repo and callers still get a correct — merely slower — answer.
    """
    namespace, name = repo.split("/", 1)
    url = f"{_HUB}/namespaces/{namespace}/repositories/{name}/tags?page_size=100"
    if name_filter:
        url += f"&name={name_filter}"
    # One login per walk, not per page: the JWT outlives a full 79-page listing.
    headers = _auth_headers()
    out: list[Tag] = []
    while url:
        resp = requests.get(url, headers=headers, timeout=_TIMEOUT)
        if resp.status_code == 403 and not headers:
            # Quote Hub's own reason rather than asserting one. The offset cap is the
            # only anonymous 403 observed here (a missing repo or namespace 404s), but
            # the cap keys off the OFFSET alone — even a repo with 3 tags 403s on
            # page 11 — so "this repo has too many tags" would be an invented cause.
            # Credentials are the remedy either way.
            raise RuntimeError(
                f"Docker Hub refused an anonymous read of {repo} at {url} — "
                f"{_hub_message(resp) or f'HTTP {resp.status_code}'}. "
                "Set DOCKER_USERNAME and DOCKER_TOKEN: anonymous requests cannot page "
                "past offset 1000."
            )
        resp.raise_for_status()
        body = resp.json()
        for result in body.get("results", []):
            digest = _digest_of(result)
            if result.get("name") and digest:
                out.append(Tag(name=result["name"], digest=digest))
        url = body.get("next")
    return out
