# Environment: detect, start, provenance

## Detect a running instance

```bash
curl -sk -o /dev/null -w "%{http_code}" https://localhost:8443/dotAdmin/ --max-time 5      # 200|302 = up
CONTAINER=$(docker ps --format '{{.Names}}' | grep -E 'dotcms-core.*_dotcms$' | head -1)    # discover, never hardcode
# (plain `grep dotcms-core` also matches the opensearch/database sidecars — anchor on the _dotcms suffix)
```

The compose project prefix varies by machine — always discover the container name as above
and use `$CONTAINER` in every `docker exec/inspect/restart` below.
URL `https://localhost:8443/dotAdmin` (self-signed cert). Default dev login: `admin@dotcms.com` / `admin`.
The helpers honor `DOTCMS_URL` if the instance is elsewhere.

## Start if not running

```bash
just dev-run-fixed        # fixed ports: HTTP 8080, HTTPS 8443, mgmt 8090 — designed for agentic/CI use
just dev-wait-ready       # polls /dotmgt/readyz, ~100s budget
```

Postgres + OpenSearch come up first, the app last (~2–4 min). The Maven launcher process may
exit 0 while the container keeps running — that's fine. Fallback readiness loop:

```bash
until curl -sk -o /dev/null -w "%{http_code}" https://localhost:8443/dotAdmin/ | grep -qE "200|302"; do sleep 5; done
```

## Provenance: prove the served build contains the fix

Trust the chain, not the merge badge:

```bash
# 1. Fix is in the local tree
git merge-base --is-ancestor <fixCommit> HEAD && echo OK

# 2. When was the running container's frontend built?
docker inspect <container> --format '{{.Created}}'
docker exec <container> sh -c 'ls -la /srv/dotserver/tomcat/webapps/ROOT/dotAdmin/*.js | head -3'   # bundle mtimes

# 3. What commit was the tree at, at that build time?
git reflog --date=iso | grep <build-date>

# 4. Was the fix an ancestor of that commit?
git merge-base --is-ancestor <fixCommit> <commit-at-build-time> && echo "FIX IN SERVED BUNDLE"
```

Served frontend bundles live at `/srv/dotserver/tomcat/webapps/ROOT/{dotAdmin,dotcms-block-editor,...}/`.

- Timeline evidence beats grepping minified bundles: template details like `animate.enter`
  compile to Angular *instructions*, not attributes, so they don't appear in the consts
  arrays — a structural grep can false-negative on a build that has the fix.
- Grep the served bundle only when the fix introduced a **unique string literal** (literals
  survive minification):

```bash
docker exec <container> sh -c 'grep -l "<unique-literal>" /srv/dotserver/tomcat/webapps/ROOT/dotAdmin/*.js'
```

If the bundle predates the fix: `./mvnw install -pl :dotcms-core --am -DskipTests` and restart.

## API auth (for seeding — see api-seeding.md)

**Use Basic auth on local dev — do NOT mint API tokens.**

```bash
curl -sk -u admin@dotcms.com:admin https://localhost:8443/api/v1/...
```

Why: an API token created with `expirationDays: 1` immediately trips dotCMS's
"Some API Tokens are about to expire" admin warning (`apitoken.expiry.admin.message`),
which then squats over the toolbar in every recording — the QA harness polluting the
environment it films. If an endpoint genuinely requires a JWT, mint one and **revoke it
in teardown**:

```bash
# mint:  POST /api/v1/authentication/api-token  {"user":...,"password":...,"expirationDays":1}
# teardown (mandatory):
curl -sk -u admin@dotcms.com:admin -X PUT https://localhost:8443/api/v1/apitoken/<tokenId>/revoke
# audit what's lingering:  GET /api/v1/apitoken/dotcms.org.1/tokens
```

## Feature flags (many "wrong UI" mysteries are flags)

`ConfigUtils.isFeatureFlagOn(name)` defaults **ON** (`Config.getBooleanProperty(name, true)`).
Flip at runtime without a rebuild — a config-file edit survives `docker restart` (env vars don't):

```bash
docker exec <container> sh -c 'echo "FLAG_NAME=false" >> /srv/dotserver/tomcat/webapps/ROOT/WEB-INF/classes/dotmarketing-config.properties'
docker restart <container>    # ~1–2 min
```

## Frontend unit-test environment

- Package manager is **pnpm via corepack** (not yarn). Try `corepack pnpm ...` directly
  (with `export COREPACK_ENABLE_DOWNLOAD_PROMPT=0`); if corepack/node isn't on PATH,
  activate the repo's Node version first — `.nvmrc` declares it (e.g. `nvm use` if the
  machine uses nvm). Don't assume a specific version-manager install path.
- Jest pattern goes **positionally**: `corepack pnpm nx test <project> -- <pattern>`
  (`--testPathPattern` errors on this Jest).
- Suite-level failure `Cannot find module '@openng/spectator/jest'` with 0 tests run =
  stale `node_modules` after a dependency swap → `corepack pnpm install`, retry. Not a product failure.
- Portlet project names: `portlets-dot-<feature>-portlet`; content-drive is `portlets-content-drive`.
