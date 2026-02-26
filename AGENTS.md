# AGENTS.md

Primary reference: `CLAUDE.md` (build commands, coding patterns, architecture).
Frontend reference: `core-web/CLAUDE.md` (Angular/Nx standards).

## Build, test, run

```bash
# Build everything (backend + frontend + Docker image)
./mvnw install -DskipTests                     # ~8-15 min, or: just build

# Quick rebuild after Java changes only
./mvnw install -pl :dotcms-core -DskipTests    # ~2-3 min, or: just build-quicker

# Start dotCMS (Docker-managed DB + OpenSearch + app)
just dev-start-on-port 8082                    # fixed port
just dev-stop                                  # teardown

# Frontend (from core-web/)
cd core-web && npx nx lint dotcms-ui
cd core-web && npx nx test dotcms-ui
cd core-web && npx nx serve dotcms-ui          # dev server on :4200, proxies API to :8080
```

Tests are silently skipped without explicit flags: `-Dcoreit.test.skip=false`, `-Dpostman.test.skip=false`, `-Dkarate.test.skip=false`. Never run the full integration suite (~60 min); target specific classes with `-Dit.test=ClassName`.

## Cursor Cloud specific instructions

### Starting services

1. Start Docker daemon: `sudo dockerd &>/tmp/dockerd.log &` then `sudo chmod 666 /var/run/docker.sock`
2. Build: `./mvnw install -DskipTests`
3. Run: `just dev-start-on-port 8082`
4. Admin UI: `http://localhost:8082/dotAdmin` — credentials `admin@dotcms.com` / `admin`

### Gotchas

- **Docker storage driver must be `vfs`**. The default `fuse-overlayfs` causes dpkg-divert failures during the dotCMS Docker image build. Config: `/etc/docker/daemon.json` → `{"storage-driver": "vfs"}`.
- **iptables must be legacy** for Docker networking: `sudo update-alternatives --set iptables /usr/sbin/iptables-legacy`.
- **Pre-commit hooks require SDKMAN** which isn't installed in Cloud VMs. Use `--no-verify` on commits.
- **Frontend build memory**: standalone NX builds may OOM — use `NODE_OPTIONS="--max_old_space_size=4096"` and `--parallel=2`. The Maven build handles this internally.
