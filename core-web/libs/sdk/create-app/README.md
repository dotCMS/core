# @dotcms/create-app

CLI to scaffold a dotCMS frontend project or start a local dotCMS Docker stack.

## Status

Beta. Behavior and flags may change.

## Requirements

- Node.js + npm
- Git
- Docker (for `--local` or `--starter`)
- Internet access (downloads templates and docker-compose)

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
5. Configures UVE via `/api/v1/apps/dotema-config-v2/{siteId}`.
6. Scaffolds selected frontend and runs `npm install`.
7. Prints framework-specific env setup instructions.

### 2) Local mode (`--local`)

Flow:

1. Validates Docker availability.
2. Validates required ports: `8082`, `8443`, `9200`, `9600`.
3. Downloads docker-compose from dotCMS main repo.
4. Runs `docker compose up -d`.
5. Waits for local health check.
6. Authenticates with default local credentials (`admin@dotcms.com` / `admin`).
7. Reads `defaultSite`, configures UVE, scaffolds frontend, runs `npm install`.
8. Prints framework-specific env setup instructions.

### 3) Starter-only local mode (`--starter <url>`)

`--starter` implies local mode.

Flow:

1. Same Docker and port checks as local mode.
2. Downloads docker-compose.
3. Rewrites `CUSTOM_STARTER_URL` in `docker-compose.yml`.
4. Also passes `CUSTOM_STARTER_URL` in compose environment at runtime.
5. Starts containers and waits for health check.
6. Skips frontend scaffold and dotCMS frontend settings flow (token, default site lookup, UVE setup).

Use this when your starter is not compatible with the default frontend sample flow.

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

- macOS/Linux: `lsof -i :8082`
- Windows: `netstat -ano | findstr ":8082"`
- Stop conflicting services or run `docker compose down`.

`zip END header not found` during starter load:

- Starter URL is reachable but not returning a valid ZIP payload.
- Verify artifact URL, repository permissions, and response content type/body.

## Development (this repo)

Build:

```sh
yarn nx build sdk-create-app --skip-nx-cache
```

Lint:

```sh
yarn nx lint sdk-create-app
```

Dist output:

- `dist/libs/sdk/create-app/index.js`
- ESM Node CLI bundle with shebang in production build
- Publishable package includes JavaScript files and `README.md` only (no type declarations)
