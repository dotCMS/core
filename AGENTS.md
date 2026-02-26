# dotCMS

Headless/hybrid CMS — Java 21 backend (Maven), Angular frontend (Nx/Yarn), Docker infrastructure. Migrating to Java 25 with parallel CI workflows; core source uses Java 11 release compatibility.

## Build, test, run

```bash
./mvnw install -DskipTests                        # full build (~8-15 min), or: just build
./mvnw install -pl :dotcms-core -DskipTests        # core only (~2-3 min), or: just build-quicker

just dev-start-on-port 8082                        # start (DB + OpenSearch + app in Docker)
just dev-stop                                      # teardown

cd core-web && npx nx serve dotcms-ui              # frontend dev server on :4200
cd core-web && npx nx lint dotcms-ui               # frontend lint
cd core-web && npx nx test dotcms-ui               # frontend test
```

### Testing

Target specific classes — never run the full suite (~60 min):

```bash
./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=MyTestClass
./mvnw verify -pl :dotcms-postman -Dpostman.test.skip=false -Dpostman.collections=ai
```

Tests are **silently skipped** without explicit `skip=false` flags: `-Dcoreit.test.skip=false`, `-Dpostman.test.skip=false`, `-Dkarate.test.skip=false`.

## References

- `justfile` — run with `just <command>` or read as a Maven command reference
- `docs/README.md` — full documentation index (Java, Angular, testing, REST, Docker, CI/CD)
- `core-web/CLAUDE.md` — frontend standards and Nx commands
- `.cursor/rules/` — domain-specific rules loaded by file pattern (Java, frontend, tests, docs)

## Cursor Cloud specific instructions

### Starting services

1. Start Docker: `sudo dockerd &>/tmp/dockerd.log &` then `sudo chmod 666 /var/run/docker.sock`
2. Build: `./mvnw install -DskipTests`
3. Run: `just dev-start-on-port 8082`
4. Admin UI: `http://localhost:8082/dotAdmin` — `admin@dotcms.com` / `admin`

### Gotchas

- **Docker storage driver must be `vfs`** — `fuse-overlayfs` causes dpkg-divert failures. Config: `/etc/docker/daemon.json` → `{"storage-driver": "vfs"}`
- **iptables must be legacy** — `sudo update-alternatives --set iptables /usr/sbin/iptables-legacy`
- **Pre-commit hooks require SDKMAN** — use `--no-verify` on commits
- **Frontend OOM** — standalone NX builds need `NODE_OPTIONS="--max_old_space_size=4096" --parallel=2`
