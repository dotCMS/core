#!/usr/bin/env python3
"""WebDAV regression pass against a running dotCMS.

Exercises what a WebDAV client does when it mounts and uses a site: discover (OPTIONS), browse
(PROPFIND with and without a body), create a folder (MKCOL), upload (PUT), read back (GET), set
properties (PROPPATCH with a body), lock and unlock (LOCK with a lockinfo body, UNLOCK), copy,
rename and move (COPY, MOVE), and delete. It also checks that REPORT, which no dotCMS resource
implements, is still answered as not implemented.

Every step asserts the status a client relies on, so a library or dependency change that breaks
WebDAV at runtime (the servlet failing to start answers every request with 5xx) fails here even
when the build is green. Standard library only.

Usage:
    uv run scripts/webdav-regression.py --url http://localhost:8082 \\
        --user admin@dotcms.com --password admin [--host default]

WebDAV authenticates with Basic: the username plus an API token. The script obtains a short-lived
token from /api/v1/authentication/api-token with the given password, works under a uniquely named
folder, and deletes that folder at the end. Exit status 1 on any failed step.
"""
import argparse
import base64
import http.client
import json
import sys
import time
import urllib.parse
import xml.etree.ElementTree as ET

DAV = "{DAV:}"

PROPFIND_NAMED = b"""<?xml version="1.0" encoding="utf-8"?>
<D:propfind xmlns:D="DAV:">
  <D:prop>
    <D:displayname/><D:resourcetype/><D:getcontentlength/><D:getcontenttype/>
    <D:getlastmodified/><D:creationdate/><D:getetag/><D:supportedlock/><D:lockdiscovery/>
  </D:prop>
</D:propfind>"""

PROPFIND_ALLPROP = b"""<?xml version="1.0" encoding="utf-8"?>
<D:propfind xmlns:D="DAV:"><D:allprop/></D:propfind>"""

PROPPATCH = b"""<?xml version="1.0" encoding="utf-8"?>
<D:propertyupdate xmlns:D="DAV:" xmlns:Z="urn:schemas-microsoft-com:">
  <D:set><D:prop><Z:Win32LastModifiedTime>Mon, 16 Nov 2026 10:00:00 GMT</Z:Win32LastModifiedTime></D:prop></D:set>
</D:propertyupdate>"""

LOCKINFO = b"""<?xml version="1.0" encoding="utf-8"?>
<D:lockinfo xmlns:D="DAV:">
  <D:lockscope><D:exclusive/></D:lockscope><D:locktype><D:write/></D:locktype>
  <D:owner><D:href>webdav-regression</D:href></D:owner>
</D:lockinfo>"""

REPORT = b"""<?xml version="1.0" encoding="utf-8"?>
<D:version-tree xmlns:D="DAV:"><D:prop><D:version-name/></D:prop></D:version-tree>"""


class Client:
    """Minimal WebDAV client over http.client, which accepts any method name."""

    def __init__(self, base_url, user, token):
        parsed = urllib.parse.urlsplit(base_url)
        self.scheme, self.netloc = parsed.scheme, parsed.netloc
        self.auth = "Basic " + base64.b64encode(f"{user}:{token}".encode()).decode()

    def request(self, method, path, body=None, headers=None):
        conn_cls = http.client.HTTPSConnection if self.scheme == "https" else http.client.HTTPConnection
        conn = conn_cls(self.netloc, timeout=60)
        # Every real client sends a User-Agent, and dotCMS's upload path reads it.
        all_headers = {"Authorization": self.auth, "User-Agent": "dotcms-webdav-regression"}
        if body is not None and method not in ("PUT",):
            all_headers["Content-Type"] = 'application/xml; charset="utf-8"'
        all_headers.update(headers or {})
        conn.request(method, path, body=body, headers=all_headers)
        resp = conn.getresponse()
        data = resp.read()
        conn.close()
        return resp.status, dict((k.lower(), v) for k, v in resp.getheaders()), data

    def url(self, path):
        return f"{self.scheme}://{self.netloc}{path}"


def api_token(base_url, user, password):
    """Create a one-day API token for the user."""
    parsed = urllib.parse.urlsplit(base_url)
    conn_cls = http.client.HTTPSConnection if parsed.scheme == "https" else http.client.HTTPConnection
    conn = conn_cls(parsed.netloc, timeout=60)
    body = json.dumps({"user": user, "password": password, "expirationDays": 1, "label": "webdav-regression"})
    conn.request("POST", "/api/v1/authentication/api-token", body=body,
                 headers={"Content-Type": "application/json"})
    resp = conn.getresponse()
    data = resp.read()
    if resp.status != 200:
        sys.exit(f"Could not create an API token: HTTP {resp.status} {data[:200]!r}")
    return json.loads(data)["entity"]["token"]


def multistatus(data):
    """Parse a 207 body and return {href: {property local name: element}} for properties found (200).

    A body that is not a multistatus document yields an empty result; the step that fetched it has
    already been recorded as failed.
    """
    result = {}
    try:
        document = ET.fromstring(data)
    except ET.ParseError:
        return result
    for response in document.iter(f"{DAV}response"):
        href = urllib.parse.unquote(response.findtext(f"{DAV}href", "").strip())
        props = {}
        for propstat in response.iter(f"{DAV}propstat"):
            if " 200 " in (propstat.findtext(f"{DAV}status") or ""):
                for prop in propstat.iter(f"{DAV}prop"):
                    for child in prop:
                        props[child.tag.split("}")[-1]] = child
        result[href.rstrip("/")] = props
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--url", required=True, help="dotCMS base URL, e.g. http://localhost:8082")
    parser.add_argument("--user", required=True)
    parser.add_argument("--password", required=True)
    parser.add_argument("--host", default="default", help="site to work in (default: the starter's 'default' site)")
    args = parser.parse_args()

    client = Client(args.url, args.user, api_token(args.url, args.user, args.password))
    root = f"/webdav/live/1/{args.host}"
    folder = f"{root}/webdav-regression-{int(time.time())}"
    failures = []

    def step(name, method, path, expect, body=None, headers=None):
        status, resp_headers, data = client.request(method, path, body, headers)
        ok = status in expect
        print(f"{'PASS' if ok else 'FAIL'}  {name}: {method} {path} -> {status}")
        if not ok:
            failures.append(f"{name}: expected {sorted(expect)}, got {status} {data[:300]!r}")
        return status, resp_headers, data

    def check(name, condition, detail=""):
        print(f"{'PASS' if condition else 'FAIL'}  {name}")
        if not condition:
            failures.append(f"{name} {detail}")

    # Mount: discovery and a browse of the site root.
    _, headers, _ = step("discover", "OPTIONS", f"{root}/", {200})
    check("DAV header advertises class 1 and 2", "1" in headers.get("dav", "") and "2" in headers.get("dav", ""),
          headers.get("dav", ""))
    step("browse root, no body", "PROPFIND", f"{root}/", {207}, headers={"Depth": "1"})
    _, _, data = step("browse root, allprop body", "PROPFIND", f"{root}/", {207}, PROPFIND_ALLPROP, {"Depth": "0"})
    check("allprop lists resourcetype", any("resourcetype" in p for p in multistatus(data).values()))

    # Folder and upload.
    step("create folder", "MKCOL", f"{folder}/", {201})
    content = b"webdav regression content\n"
    step("upload", "PUT", f"{folder}/a.txt", {201, 204}, content, {"Content-Type": "text/plain"})
    _, _, data = step("download", "GET", f"{folder}/a.txt", {200})
    check("downloaded content matches upload", data == content, repr(data[:100]))

    # Browse the folder with named properties.
    _, _, data = step("browse folder, named props", "PROPFIND", f"{folder}/", {207}, PROPFIND_NAMED, {"Depth": "1"})
    listing = multistatus(data)
    file_props = next((p for h, p in listing.items() if h.endswith("/a.txt")), None)
    check("listing contains the uploaded file", file_props is not None, str(list(listing)))
    if file_props is not None:
        length = file_props.get("getcontentlength")
        check("file has its content length", length is not None and length.text == str(len(content)),
              "" if length is None else length.text)
        check("file reports supportedlock", "supportedlock" in file_props)
    folder_props = listing.get(folder.rstrip("/"))
    check("folder is a collection", folder_props is not None and folder_props.get("resourcetype") is not None
          and folder_props["resourcetype"].find(f"{DAV}collection") is not None)

    # Properties: the PROPPATCH body path. Clients such as Windows Explorer send this after upload.
    _, _, data = step("set property", "PROPPATCH", f"{folder}/a.txt", {207}, PROPPATCH)
    check("PROPPATCH answers a multistatus body", b"multistatus" in data, repr(data[:200]))

    # Lock, write under the lock, unlock.
    _, headers, data = step("lock", "LOCK", f"{folder}/a.txt", {200}, LOCKINFO,
                            {"Timeout": "Second-300", "Depth": "0"})
    token = headers.get("lock-token", "").strip("<>")
    check("LOCK returns a lock token", bool(token), repr(data[:200]))
    _, _, data = step("browse locked file", "PROPFIND", f"{folder}/a.txt", {207}, PROPFIND_NAMED, {"Depth": "0"})
    # dotCMS answers lockdiscovery empty even while locked (its resources report no current lock);
    # the property's presence in the 207 is what clients parse.
    locked_props = next(iter(multistatus(data).values()), {})
    check("PROPFIND reports lockdiscovery", "lockdiscovery" in locked_props, repr(data[:300]))
    step("upload under the lock", "PUT", f"{folder}/a.txt", {201, 204}, content,
         {"Content-Type": "text/plain", "If": f"(<{token}>)"})
    step("unlock", "UNLOCK", f"{folder}/a.txt", {204}, headers={"Lock-Token": f"<{token}>"})

    # Copy, rename, move into a subfolder.
    step("copy", "COPY", f"{folder}/a.txt", {201, 204}, headers={"Destination": client.url(f"{folder}/b.txt")})
    step("rename", "MOVE", f"{folder}/b.txt", {201, 204}, headers={"Destination": client.url(f"{folder}/c.txt")})
    step("create subfolder", "MKCOL", f"{folder}/sub/", {201})
    step("move into subfolder", "MOVE", f"{folder}/c.txt", {201, 204},
         headers={"Destination": client.url(f"{folder}/sub/c.txt")})
    _, _, data = step("browse after moves", "PROPFIND", f"{folder}/", {207}, PROPFIND_NAMED, {"Depth": "1"})
    hrefs = set(multistatus(data))
    check("renamed file left its old name", not any(h.endswith("/b.txt") or h.endswith("/c.txt") for h in hrefs),
          str(hrefs))
    step("moved file is in the subfolder", "GET", f"{folder}/sub/c.txt", {200})

    # REPORT is not implemented by any dotCMS resource, and must stay that way.
    step("report not implemented", "REPORT", f"{folder}/a.txt", {501}, REPORT, {"Depth": "0"})

    # Delete everything this run created.
    step("delete file", "DELETE", f"{folder}/a.txt", {204})
    step("delete folder", "DELETE", f"{folder}/", {204})

    print()
    if failures:
        print(f"{len(failures)} failed:")
        for failure in failures:
            print("  " + failure)
        sys.exit(1)
    print("All WebDAV steps passed.")


if __name__ == "__main__":
    main()
