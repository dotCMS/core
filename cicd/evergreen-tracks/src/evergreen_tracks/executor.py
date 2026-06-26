"""Write side: move a track tag to a digest, and create/delete marker tags.

Moves use `docker buildx imagetools create`, which re-points a tag to an existing
digest without re-pushing layers. Deletes use the Docker Hub API (needs a JWT).
Every mutating call respects `apply`: when False it logs the intended action only.
"""
from __future__ import annotations

import logging
import subprocess

import requests

_HUB = "https://hub.docker.com/v2"
_TIMEOUT = 30
log = logging.getLogger("evergreen_tracks.executor")


def point_tag(repo: str, tag: str, digest: str, *, apply: bool) -> None:
    """Point repo:tag at repo@digest."""
    src = f"{repo}@{digest}"
    dst = f"{repo}:{tag}"
    if not apply:
        log.info("DRY-RUN would point %s -> %s", dst, digest)
        return
    log.info("pointing %s -> %s", dst, digest)
    subprocess.run(
        ["docker", "buildx", "imagetools", "create", "-t", dst, src],
        check=True,
    )


def hub_login(username: str, password: str) -> str:
    """Return a Hub JWT for delete calls."""
    resp = requests.post(
        f"{_HUB}/users/login",
        json={"username": username, "password": password},
        timeout=_TIMEOUT,
    )
    resp.raise_for_status()
    body = resp.json()
    if "token" not in body:
        raise RuntimeError(
            f"Hub login succeeded (HTTP {resp.status_code}) but response contained no 'token' key. "
            f"Response body: {body!r}"
        )
    return body["token"]


def delete_tag(repo: str, tag: str, token: str, *, apply: bool) -> None:
    """Delete repo:tag via the Hub API (used for untaint / release-hold / teardown)."""
    namespace, name = repo.split("/", 1)
    url = f"{_HUB}/repositories/{namespace}/{name}/tags/{tag}/"
    if not apply:
        log.info("DRY-RUN would delete tag %s:%s", repo, tag)
        return
    log.info("deleting tag %s:%s", repo, tag)
    resp = requests.delete(url, headers={"Authorization": f"JWT {token}"}, timeout=_TIMEOUT)
    if resp.status_code not in (200, 202, 204, 404):
        resp.raise_for_status()
