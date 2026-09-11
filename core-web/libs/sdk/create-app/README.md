# @dotcms/create-app

CLI to scaffold a dotCMS frontend project or start a local dotCMS Docker stack.

## Status

Beta. Behavior and flags may change.

## Requirements

- Node.js 22.22.3+ and npm
- Git
- Docker (for `--local` or `--starter`)
- Internet access (downloads templates; pulls Docker images)

## Which SDK Version Should I Use?

dotCMS SDKs are published in lockstep with dotCMS itself: every `@dotcms/*` package ships
at the **exact same version number** as the dotCMS release it was built for (e.g. dotCMS
`26.7.14-1` → `@dotcms/client@26.7.14-1`, `@dotcms/react@26.7.14-1`, and so on).

**Simple rule of thumb: use the SDK version that matches your dotCMS instance's version.**

You don't have to upgrade the SDK every time dotCMS releases a new version (or vice versa).
Most releases don't change anything the SDKs rely on, so an older SDK usually keeps working
fine against a newer dotCMS instance. Occasionally, though, a release does include a real
breaking change — and if your SDK is older than that point, it will stop working correctly.

You don't need to track this yourself: your dotCMS instance always knows the oldest SDK
version it still supports, and the SDK checks itself against it automatically. If you're
using an SDK that's too old, you'll see a clear warning in your console telling you to
upgrade.

**Recommendation:** pin your SDKs to the same version as your dotCMS instance, and only bump
them when you upgrade dotCMS — or when the console tells you to.

> **On an LTS release?** LTS releases don't currently get their own matching SDK version.
> Until that's addressed, use the SDK version published for the closest regular release at
> or before your LTS version.
>
> Want more background on how dotCMS releases and support windows work? See
> [Release & Support Lifecycle](https://dev.dotcms.com/docs/release-support-lifecycle).

## Quick Start

```sh
npx @dotcms/create-app my-app
```

Global install:

```sh
npm install -g @dotcms/create-app
create-dotcms-app my-app
```

## CLI

```sh
create-dotcms-app [projectName] [options]
```

| Option | Description |
|---|---|
| `-f, --framework <framework>` | Framework: `nextjs`, `astro`, `angular`, `angular-ssr` |
| `-d, --directory <path>` | Parent or target directory |
| `--local` | Use local dotCMS with Docker |
| `--starter <url>` | Custom starter ZIP URL (local-only; sets `CUSTOM_STARTER_URL`) |
| `--url <url>` | dotCMS URL for cloud mode |
| `-u, --username <username>` | dotCMS username for cloud mode |
| `-p, --password <password>` | dotCMS password for cloud mode |
| `-V, --version` | Show CLI version |

Framework aliases:

- `next`, `next.js` -> `nextjs`
- `ng` -> `angular`
- `angular-server` -> `angular-ssr`

## Behavior by Mode

### 1) Cloud mode (existing dotCMS instance)

Used when you do not pass `--local` or `--starter` and choose cloud in prompts.

Flow:

1. Validates URL, project name, and flags.
2. Checks dotCMS health at `/api/v1/appconfiguration`.
3. Authenticates (up to 3 attempts).
4. Reads `defaultSite` from `/api/v1/site/defaultSite`.
5. Configures UVE via `/api/v1/apps/dotema-config-v2/{siteId}`. **Optional** — if this fails the
   CLI warns, explains how to finish it by hand, and carries on.
6. Scaffolds selected frontend and runs `npm install`.
7. Writes `.env` with your host, site ID and token (see [Your `.env`](#your-env)).

### 2) Local mode (`--local`)

Flow:

1. Validates Docker availability.
2. Checks the ports this stack publishes: `8082`, `8443` and `8090`. A dotCMS already running on
   `8082` is not treated as a conflict — see [If dotCMS is already running](#if-dotcms-is-already-running).
3. Writes the **bundled** `docker-compose.yml` into the project directory (see
   [The bundled Docker stack](#the-bundled-docker-stack)).
4. Runs `docker compose up -d --wait`, which blocks until every service reports healthy, streaming
   progress and elapsed time so a long first pull is never a silent spinner.
5. Waits for readiness on `/dotmgt/readyz`, falling back to `/api/v1/appconfiguration`.
6. Authenticates with default local credentials (`admin@dotcms.com` / `admin`).
7. Reads `defaultSite`, configures UVE (optional — a failure warns and continues), scaffolds the
   frontend, runs `npm install`.
8. Writes `.env` with your host, site ID and token (see [Your `.env`](#your-env)).

### 3) Starter-only local mode (`--starter <url>`)

`--starter` implies local mode.

Flow:

1. Same Docker and port checks as local mode.
2. Writes the bundled `docker-compose.yml`.
3. Rewrites `CUSTOM_STARTER_URL` in `docker-compose.yml`.
4. Also passes `CUSTOM_STARTER_URL` in compose environment at runtime.
5. Starts containers and waits for health check.
6. Skips frontend scaffold and dotCMS frontend settings flow (token, default site lookup, UVE setup).

Use this when your starter is not compatible with the default frontend sample flow.

## The bundled Docker stack

`--local` and `--starter` write a `docker-compose.yml` that **ships inside this package**. It is
no longer downloaded from the `dotCMS/core` repository at run time, so the stack you get is the one
this CLI version was tested against, rather than whatever is currently on `main`.

The stack is `db` (PostgreSQL), `opensearch`, and `dotcms`. `dotcms` starts only after both
dependencies report **healthy**, and carries `restart: unless-stopped`, so it no longer races
Postgres and exit at startup.

### Published ports

| Port | Binding | Purpose |
| --- | --- | --- |
| `8082` | all interfaces | dotCMS HTTP |
| `8443` | all interfaces | dotCMS HTTPS |
| `8090` | **`127.0.0.1` only** | dotCMS management endpoints |

PostgreSQL and OpenSearch publish **no** ports — they are reachable only from inside the compose
network, so running your own Postgres or OpenSearch on the usual ports does not conflict.

> **Why 8090 is loopback-only.** It serves `/dotmgt/livez`, `/dotmgt/readyz`, `/dotmgt/health` and
> `/dotmgt/metrics`, and dotCMS authorizes those purely by the port a request arrives on — there is
> no credential check and no IP allow-list. Binding it to `0.0.0.0` would expose your instance's
> health and metrics to everyone on your network. It is bound to `127.0.0.1` deliberately; do not
> "fix" it to a wildcard.

### Using a different compose file

Set `DOTCMS_COMPOSE_URL` to fetch one from a URL instead of using the bundled file:

```bash
DOTCMS_COMPOSE_URL=https://example.com/my-compose.yml npx @dotcms/create-app my-app --local
```

This is an escape hatch for hotfixes. The file must keep a single-line `CUSTOM_STARTER_URL:` entry
or `--starter` will fail against it.

## If dotCMS is already running

A dotCMS on `8082` from a previous run is not a conflict — the CLI probes it and offers a choice:

```
⚠  Found a dotCMS already running at http://localhost:8082
   Docker project "my-app" · Up 8 minutes (healthy)

? How would you like to continue?
❯ Use this instance for my project
  Replace it with a clean instance
  Cancel
```

**Replace** stops that Docker project and removes its volumes (`docker compose -p <project> down -v`)
before starting fresh. It is offered only when a Compose project owns the port — something started
outside Compose is not the CLI's to destroy.

Reuse requires the instance to pass a readiness check **and** issue a token; anything else on `8082`
is still a hard failure.

In a non-interactive run (CI, or no TTY) the CLI auto-reuses and prints a notice. It never replaces
without being asked.

## Your `.env`

The CLI writes `.env` into your project with the values the scaffolded app reads — `NEXT_PUBLIC_*`
for Next.js, `PUBLIC_*` for Astro. You do not need to copy anything by hand.

An existing `.env` is never overwritten: the CLI prints the values instead so you can merge them.
Angular has no `.env` — it reads a TypeScript `environment` object, so the values are printed for
you to paste into the environment files.

## Examples

Interactive:

```sh
npx @dotcms/create-app my-blog
```

Local + specific framework:

```sh
npx @dotcms/create-app my-blog --local --framework nextjs
```

Starter-only local:

```sh
npx @dotcms/create-app my-blog --starter https://repo.example.com/path/starter.zip
```

Cloud with flags:

```sh
npx @dotcms/create-app my-blog \
  --framework angular \
  --url https://demo.dotcms.com \
  --username admin@dotcms.com \
  --password admin
```

Debug errors with stack traces:

```sh
DEBUG=1 npx @dotcms/create-app my-blog --local
```

## Validation Rules

- URLs must include protocol (`http://` or `https://`).
- Project names are validated for path traversal, invalid characters, reserved Windows names, and length.
- If local mode is selected (`--local` or `--starter`), cloud flags are ignored with a warning.
- Existing non-empty target directory requires confirmation before cleanup.

## Troubleshooting

Docker not available:

- Install/start Docker Desktop, then retry.

Ports already in use:

- If it is a dotCMS from a previous run, the CLI offers to reuse or replace it — see
  [If dotCMS is already running](#if-dotcms-is-already-running).
- Otherwise, find the owner: `lsof -i :8082` (macOS/Linux) or
  `netstat -ano | findstr ":8082"` (Windows), then stop it or run `docker compose down`.

`zip END header not found` during starter load:

- Starter URL is reachable but not returning a valid ZIP payload.
- Verify artifact URL, repository permissions, and response content type/body.

## Development (this repo)

Build:

```sh
pnpm nx build sdk-create-app --skip-nx-cache
```

Lint:

```sh
pnpm nx lint sdk-create-app
```

Verify the package ships correctly (asserts the compose asset reaches `dist/` and the npm tarball):

```sh
pnpm nx verify-package sdk-create-app
```

Dist output:

- `dist/libs/sdk/create-app/index.js`
- ESM Node CLI bundle with shebang in production build
- Publishable package includes JavaScript files and `README.md` only (no type declarations)
