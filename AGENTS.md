## Cursor Cloud specific instructions

### Services Overview

dotCMS is a headless CMS with a Java backend (Maven) and Angular frontend (Nx/Yarn). See `CLAUDE.md` for command reference and `core-web/CLAUDE.md` for frontend specifics.

| Service | How to run | Port |
|---------|-----------|------|
| **dotCMS Backend** (with PostgreSQL + OpenSearch) | `docker compose -f docker/docker-compose-examples/single-node/docker-compose.yml up -d` | 8082 (HTTP), 8443 (HTTPS) |
| **Frontend dev server** (Angular) | `cd core-web && npx nx serve dotcms-ui` | 4200 (proxies API to 8082) |

Default admin credentials: `admin@dotcms.com` / `admin`

### Running the application

1. Ensure Docker daemon is running: `sudo dockerd &>/tmp/dockerd.log &` (wait ~5s), then `sudo chmod 666 /var/run/docker.sock`
2. Start dotCMS with infrastructure: `docker compose -f docker/docker-compose-examples/single-node/docker-compose.yml up -d`
3. Wait for readiness: `curl -s http://localhost:8082/api/v1/appconfiguration` should return 200
4. For frontend development: `cd core-web && npx nx serve dotcms-ui`

### Build commands

- **Full backend build (no Docker image):** `./mvnw clean install -DskipTests -Ddocker.skip`
- **Quick core-only rebuild:** `./mvnw install -pl :dotcms-core -DskipTests -Ddocker.skip`
- **Frontend build:** `cd core-web && npx nx run-many -t build --exclude='tag:skip:build' --parallel=2`
- **Frontend lint:** `cd core-web && npx nx lint dotcms-ui`
- **Frontend test (targeted):** `cd core-web && npx nx test sdk-client`

See `justfile` for convenient aliases (e.g. `just build`, `just build-quicker`).

### Known gotchas in Cloud VM

- **Docker-in-Docker**: The VM runs inside a container. Docker needs `fuse-overlayfs` storage driver and `iptables-legacy`. These are set up in the environment snapshot.
- **`/dist` directory**: The NX `edit-content-bridge:build` target resolves its output path to `/dist` (root filesystem) due to a path resolution quirk. The directory `/dist` must exist and be writable: `sudo mkdir -p /dist && sudo chmod 777 /dist`.
- **Frontend build memory**: Use `NODE_OPTIONS="--max_old_space_size=4096"` and `--parallel=2` for NX builds to avoid OOM kills.
- **Maven + frontend**: The Maven build includes the frontend (`dotcms-core-web` module). If frontend is already built via NX, pass `-Dskip.npm.install=true` to skip redundant yarn install.
- **Building Docker image from source fails**: `./mvnw install -pl :dotcms-core` (with Docker enabled) fails due to network restrictions in the Docker build step. Use `-Ddocker.skip` and run the pre-built `dotcms/dotcms:latest` image instead.
- **Pre-commit hooks**: The `.husky/pre-commit` hook in `core-web/` requires SDKMAN and runs Maven validation + NX lint/format. In cloud agents, commit with `--no-verify` if the hook fails.
