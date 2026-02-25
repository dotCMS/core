## Cursor Cloud specific instructions

### Services Overview

dotCMS is a headless CMS with a Java backend (Maven) and Angular frontend (Nx/Yarn). See `CLAUDE.md` for command reference and `core-web/CLAUDE.md` for frontend specifics.

| Service | How to run | Port |
|---------|-----------|------|
| **dotCMS Backend** (Maven Docker plugin) | `just dev-run` or `./mvnw -pl :dotcms-core -Pdocker-start -Ddocker.glowroot.enabled=true` | 8082 (HTTP) |
| **Frontend dev server** (Angular) | `cd core-web && npx nx serve dotcms-ui` | 4200 (proxies API to 8080) |

The Maven `docker-start` profile builds and runs dotCMS from source in a Docker container alongside PostgreSQL and OpenSearch, matching CI/CD. This is preferred over `docker-compose` examples which use pre-built images.

Default admin credentials: `admin@dotcms.com` / `admin`

### Build and run

1. **Build everything** (includes Docker image): `./mvnw install -DskipTests` or `just build`
2. **Start dotCMS**: `just dev-run` (runs `./mvnw -pl :dotcms-core -Pdocker-start -Ddocker.glowroot.enabled=true`)
3. **Stop dotCMS**: `just dev-stop` (runs `./mvnw -pl :dotcms-core -Pdocker-stop`)
4. **Quick core rebuild** (after code changes): `just build-quicker` (runs `./mvnw -pl :dotcms-core -DskipTests install`)

See `justfile` for the full list of convenient aliases.

### Frontend commands

- **Install deps**: Handled automatically by `./mvnw install` via `frontend-maven-plugin` in the `dotcms-core-web` module. For standalone use: `cd core-web && yarn install --frozen-lockfile`
- **Build**: `cd core-web && npx nx run-many -t build --exclude='tag:skip:build' --parallel=2`
- **Lint**: `cd core-web && npx nx lint dotcms-ui`
- **Test (targeted)**: `cd core-web && npx nx test sdk-client`

### Known gotchas in Cloud VM

- **Docker storage driver**: Must use `vfs` (not `fuse-overlayfs`). The `fuse-overlayfs` driver causes dpkg-divert failures during the dotCMS Docker image build. The daemon config at `/etc/docker/daemon.json` should have `{"storage-driver": "vfs"}`.
- **Docker daemon startup**: Run `sudo dockerd &>/tmp/dockerd.log &` then `sudo chmod 666 /var/run/docker.sock` before using Docker.
- **`/dist` directory**: The NX `edit-content-bridge:build` target resolves its output path to `/dist` (root filesystem). The directory must exist and be writable: `sudo mkdir -p /dist && sudo chmod 777 /dist`.
- **Frontend build memory**: Use `NODE_OPTIONS="--max_old_space_size=4096"` and `--parallel=2` for standalone NX builds to avoid OOM kills. The Maven build handles this internally.
- **Pre-commit hooks**: The `.husky/pre-commit` hook requires SDKMAN. In cloud agents, commit with `--no-verify`.
- **iptables**: Must use legacy iptables for Docker networking: `sudo update-alternatives --set iptables /usr/sbin/iptables-legacy`.
