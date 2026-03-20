set positional-arguments := true
home_dir := env_var('HOME')
# dotCMS justfile — command runner for build, dev, and test tasks.
#
# Getting started:
#   brew install just          # or: mise install (if you have mise)
#   just setup                 # installs mise, configures shell, installs all tools
#   just build                 # full build (~8-15 min)
#   just dev-run 8080          # start the stack
#
# IntelliJ plugin: https://plugins.jetbrains.com/plugin/18658-just
# See also: docs/infrastructure/DEV_ENVIRONMENT_SETUP.md

###########################################################
# Core Commands
###########################################################

# Lists all available commands in this justfile
default:
    @just --list --unsorted --justfile {{ justfile() }}

# Installs mise, configures shell for interactive + non-interactive sessions, and installs all tools.
setup:
    #!/usr/bin/env bash
    set -e

    # Install mise if not already installed
    if ! command -v mise &>/dev/null && [ ! -f "$HOME/.local/bin/mise" ]; then
        echo "Installing mise..."
        curl https://mise.run | sh
    fi

    # Use full path in case mise isn't on PATH yet after fresh install
    MISE="${HOME}/.local/bin/mise"
    if command -v mise &>/dev/null; then
        MISE="mise"
    fi

    SHELL_NAME=$(basename "$SHELL")

    add_line_if_missing() {
        local file="$1"
        local line="$2"
        mkdir -p "$(dirname "$file")"
        touch "$file"
        if ! grep -qF "$line" "$file"; then
            echo "$line" >> "$file"
            echo "  ✓ Added to $file"
        else
            echo "  ↩ Already present in $file"
        fi
    }

    echo "Detected shell: $SHELL_NAME"

    case "$SHELL_NAME" in
        zsh)
            add_line_if_missing ~/.zprofile 'eval "$(mise activate zsh --shims)"'
            add_line_if_missing ~/.zshrc    'eval "$(mise activate zsh)"'
            ;;
        bash)
            add_line_if_missing ~/.bash_profile 'eval "$(mise activate bash --shims)"'
            add_line_if_missing ~/.bashrc       'eval "$(mise activate bash)"'
            ;;
        fish)
            add_line_if_missing ~/.config/fish/config.fish 'mise activate fish --shims | source'
            add_line_if_missing ~/.config/fish/config.fish 'mise activate fish | source'
            ;;
        *)
            echo "Unknown shell: $SHELL_NAME, falling back to ~/.profile"
            add_line_if_missing ~/.profile 'export PATH="$HOME/.local/share/mise/shims:$PATH"'
            ;;
    esac

    echo ""
    echo "Installing tools..."
    "$MISE" install

    # Install worktrunk shell integration if available
    if command -v wt &>/dev/null || [ -f "$HOME/.local/share/mise/shims/wt" ]; then
        "$MISE" exec -- wt config shell install 2>/dev/null && \
            echo "  ✓ worktrunk shell integration installed" || true
    fi

    echo ""
    echo "✓ Done. Restart your shell to apply changes."

# Initializes a worktree after creation: tools, deps, hooks, Stencil build.
# Called by wt.toml post-create hook — keeps logic in one place instead of duplicating in wt.toml.
# Also safe to run manually to fix a broken worktree.
worktree-init:
    #!/usr/bin/env bash
    set -euo pipefail
    echo "Initializing worktree..."

    # 1. Tools — ensure mise tools match this branch's .mise.toml
    command -v mise >/dev/null 2>&1 && mise trust 2>/dev/null && mise install --quiet 2>/dev/null || true

    # 2. Frontend deps — yarn install reconciles node_modules against lockfile.
    #    No --frozen-lockfile: copied caches may be from a different branch.
    echo "Installing frontend dependencies..."
    cd core-web && yarn install && cd ..

    # Warn if yarn.lock drifted (cross-branch cache can cause this)
    if ! git diff --quiet -- core-web/yarn.lock 2>/dev/null; then
        echo ""
        echo "WARNING: yarn.lock changed after install (cross-branch cache drift)."
        echo "Review before committing: git diff core-web/yarn.lock"
        echo ""
    fi

    # 3. Stencil webcomponents — the only non-Angular library that needs pre-building.
    #    nx serve dotcms-ui imports @dotcms/dotcms-webcomponents/loader which is a build artifact.
    echo "Building dotcms-webcomponents (Stencil)..."
    cd core-web && NX_DAEMON=false yarn nx build dotcms-webcomponents && cd ..

    # 4. Git hooks — lefthook, after unsetting any stale husky hooksPath
    git config --unset core.hooksPath 2>/dev/null || true
    command -v lefthook >/dev/null 2>&1 && lefthook install 2>/dev/null || true

    echo "✓ Worktree ready"

# Derives a Docker-safe slug from the current branch name (used for image tags and container namespaces)
_worktree-slug:
    @git rev-parse --abbrev-ref HEAD 2>/dev/null \
        | sed 's/[^a-zA-Z0-9._-]/-/g; s/^[.-]*//' | cut -c1-64 \
        || echo "default"

# Image tag: slug + short commit hash (e.g., "my-feature-a1b2c3d4")
_worktree-image-tag:
    @echo "$(just _worktree-slug)-$(git rev-parse --short=8 HEAD 2>/dev/null || echo unknown)"

# Derives a short, unique, human-readable identifier from the worktree slug.
# Format: <prefix_24>_<sha256_8> — e.g., "cursor_development_envir_0e809b4d"
# Guarantees uniqueness (hash) while remaining identifiable (prefix).
_worktree-id:
    #!/usr/bin/env bash
    SLUG=$(just _worktree-slug)
    PREFIX=$(echo "$SLUG" | sed 's/[^a-zA-Z0-9_]/_/g' | cut -c1-24)
    HASH=$(printf '%s' "$SLUG" | shasum -a 256 2>/dev/null || printf '%s' "$SLUG" | sha256sum)
    HASH=${HASH%% *}
    echo "${PREFIX}_${HASH:0:8}"

# Returns 0 if shared services are running
_shared-services-running:
    @docker inspect dotcms-shared-db --format '{{ "{{.State.Running}}" }}' 2>/dev/null | grep -q true

# Derives a PostgreSQL-safe database name for the given module and context.
# Format: <module>_<worktree-id> — unique and within PostgreSQL's 63-char limit.
_shared-dbname module="dotcms-core":
    #!/usr/bin/env bash
    MODULE=$(echo '{{ module }}' | sed 's/[^a-zA-Z0-9_]/_/g')
    WTID=$(just _worktree-id)
    echo "${MODULE}_${WTID}"

# Creates per-worktree database in shared PostgreSQL if it doesn't exist
_ensure-shared-db module="dotcms-core":
    #!/usr/bin/env bash
    DB_NAME=$(just _shared-dbname {{ module }})
    EXISTS=$(docker exec dotcms-shared-db psql -U postgres -tAc \
        "SELECT 1 FROM pg_database WHERE datname='${DB_NAME}'" 2>/dev/null)
    if [ "$EXISTS" != "1" ]; then
        echo "Creating database ${DB_NAME}..."
        docker exec dotcms-shared-db createdb -U postgres "${DB_NAME}"
    fi

# Full build without tests — filters output to phase transitions and errors. Full log saved to /tmp.
# Tags the image with worktree slug + commit SHA, plus mutable aliases.
build:
    #!/usr/bin/env bash
    set -o pipefail
    WTAG=$(just _worktree-image-tag)
    WSLUG=$(just _worktree-slug)
    LOG=$(mktemp /tmp/dotcms-build.XXXXXX)
    echo "Building dotCMS (full) — image: dotcms/dotcms-test:${WTAG} — log: $LOG"
    ./mvnw -DskipTests clean install \
        -Ddocker.version.tag="$WTAG" \
        -Dcontext.name="$WSLUG" \
        2>&1 | tee "$LOG" | \
        grep --line-buffered -E '^\[INFO\] (---|BUILD)|^\[ERROR\]|DOCKER>' || true
    if grep -q 'BUILD FAILURE' "$LOG"; then
        echo ""
        echo "=== Build errors ==="
        grep '^\[ERROR\]' "$LOG" | grep -v '^\[ERROR\] *$' | head -30
        echo "Full log: $LOG"
        exit 1
    fi
    # Tag aliases: mutable branch tag + backward-compat snapshot
    docker tag "dotcms/dotcms-test:${WTAG}" "dotcms/dotcms-test:${WSLUG}" 2>/dev/null || true
    docker tag "dotcms/dotcms-test:${WTAG}" "dotcms/dotcms-test:1.0.0-SNAPSHOT" 2>/dev/null || true
    echo "Full log: $LOG"

# Builds the project without tests and disables Maven build cache
build-no-cache:
    ./mvnw -DskipTests clean install -Dmaven.build.cache.enabled=false

# Builds the project without running tests, skip using docker or creating image
build-no-docker:
    ./mvnw -DskipTests clean install -Ddocker.skip

# Builds the project and runs the default test suite
build-test:
    ./mvnw clean install

# Performs a quick build, installing all modules to the local repository without running tests
build-quick:
    ./mvnw -DskipTests install

# Builds dotcms-core incrementally, showing phase/error progress. Full log saved to /tmp.
# Tags the image with worktree slug + commit SHA, plus mutable aliases.
#
# WARNING: Only builds dotcms-core (-pl :dotcms-core). Dependencies like core-web are pulled
# from ~/.m2/repository/ — whatever was last installed by ANY worktree's `just build`.
# The embedded frontend WAR may be from a different branch. This is fine when:
#   - Testing backend-only changes with `nx serve` at :4200 (frontend comes from source)
#   - The .m2 WAR is recent and from the same branch
# Use `just build` (full) when you need the Docker image to be fully self-consistent.
build-quicker:
    #!/usr/bin/env bash
    set -o pipefail
    WTAG=$(just _worktree-image-tag)
    WSLUG=$(just _worktree-slug)
    LOG=$(mktemp /tmp/dotcms-build.XXXXXX)

    # Check if the .m2 core-web WAR is from this branch (warn if not)
    PROPS="$HOME/.m2/repository/com/dotcms/dotcms-core-web/1.0.0-SNAPSHOT/dotcms-core-web-1.0.0-SNAPSHOT-build.properties"
    if [ -f "$PROPS" ]; then
        WAR_REV=$(grep '^revision=' "$PROPS" 2>/dev/null | cut -d= -f2)
        if [ -n "$WAR_REV" ]; then
            if git merge-base --is-ancestor "$WAR_REV" HEAD 2>/dev/null; then
                WAR_AGE=$(( ($(date +%s) - $(grep '^timestamp=' "$PROPS" | cut -d= -f2) / 1000) / 86400 ))
                echo "Embedded frontend: rev ${WAR_REV} (${WAR_AGE}d old, on this branch)"
            else
                WAR_BRANCH=$(git branch --contains "$WAR_REV" 2>/dev/null | head -1 | sed 's/^[* ]*//')
                echo "⚠ Embedded frontend: rev ${WAR_REV} (from ${WAR_BRANCH:-unknown branch}, NOT on this branch)"
                echo "  The Docker image will have a mismatched frontend."
                echo "  Use :4200 (nx serve) for frontend, or run 'just build' for a consistent image."
            fi
        fi
    else
        echo "⚠ No core-web WAR in .m2 — run 'just build' first for a complete image."
    fi

    echo "Building dotcms-core — image: dotcms/dotcms-test:${WTAG} — log: $LOG"
    ./mvnw -pl :dotcms-core -DskipTests install \
        -Ddocker.version.tag="$WTAG" \
        -Dcontext.name="$WSLUG" \
        2>&1 | tee "$LOG" | \
        grep --line-buffered -E '^\[INFO\] (---|BUILD)|^\[ERROR\]|DOCKER>' || true
    if grep -q 'BUILD FAILURE' "$LOG"; then
        echo ""
        echo "=== Build errors ==="
        grep '^\[ERROR\]' "$LOG" | grep -v '^\[ERROR\] *$' | head -30
        echo "Full log: $LOG"
        exit 1
    fi
    # Tag aliases: mutable branch tag + backward-compat snapshot
    docker tag "dotcms/dotcms-test:${WTAG}" "dotcms/dotcms-test:${WSLUG}" 2>/dev/null || true
    docker tag "dotcms/dotcms-test:${WTAG}" "dotcms/dotcms-test:1.0.0-SNAPSHOT" 2>/dev/null || true
    echo "Full log: $LOG"

# Builds the project for production, skipping tests
build-prod:
    ./mvnw -DskipTests clean install -Pprod

# Builds core-web module
build-core-web:
    ./mvnw clean install -pl :dotcms-core-web -am -DskipTests

# Builds core-web module with Nx cache reset
build-core-web-reset-nx:
    ./mvnw clean install -pl :dotcms-core-web -am -DskipTests -Dnx.reset

# Builds core-web module with Nx cache reset
build-test-core-web:
    ./mvnw clean install -pl :dotcms-core-web -am

# Runs a comprehensive test suite including core integration and postman tests, suitable for final validation
build-test-full:
    ./mvnw clean install -Dcoreit.test.skip=false -Dpostman.test.skip=false -Dkarate.test.skip=false

# Builds a specified module without its dependencies, defaulting to the core server (dotcms-core)
build-select-module module="dotcms-core":
    ./mvnw install -pl :{{ module }} -DskipTests=true

# Builds a specified module along with its required dependencies
build-select-module-deps module=":dotcms-core":
    ./mvnw install -pl {{ module }} --am -DskipTests=true

# Starts dotCMS on the given port. Defaults to current worktree's image.
# port=""         → read from .dev-port (set by `wt switch --create` via hash_port), else 8082
# image=""        → auto-detect from worktree (dotcms/dotcms-test:{slug})
# image="default" → use stock 1.0.0-SNAPSHOT (no build needed — for frontend devs)
# image="<full>"  → use as-is (e.g., another worktree's tag)
# mode=""         → auto-detect (shared if running, else local)
# mode="shared"   → force shared services (error if not running)
# mode="local"    → force per-worktree sidecars via fabric8
dev-run port="" image="" mode="":
    #!/usr/bin/env bash
    set -euo pipefail
    just dev-stop 2>/dev/null || true
    WSLUG=$(just _worktree-slug)
    # --- Resolve port: explicit > .dev-port > default 8082 ---
    PORT="{{ port }}"
    if [ -z "$PORT" ] && [ -f .dev-port ]; then
        PORT=$(cat .dev-port | tr -d '[:space:]')
    fi
    PORT="${PORT:-8082}"

    # --- Resolve image ---
    if [ -z "{{ image }}" ]; then
        IMG="dotcms/dotcms-test:${WSLUG}"
        if ! docker image inspect "$IMG" >/dev/null 2>&1; then
            echo "No image for this worktree (${WSLUG}). Trying 1.0.0-SNAPSHOT..."
            IMG="dotcms/dotcms-test:1.0.0-SNAPSHOT"
        fi
    elif [ "{{ image }}" = "default" ]; then
        IMG="dotcms/dotcms-test:1.0.0-SNAPSHOT"
    else
        IMG="{{ image }}"
    fi
    if ! docker image inspect "$IMG" >/dev/null 2>&1; then
        echo "Image $IMG not found. Run 'just build' first, or use image=default." >&2
        exit 1
    fi

    # --- Detect shared vs local mode ---
    USE_SHARED=false
    if [ "{{ mode }}" = "shared" ]; then
        USE_SHARED=true
    elif [ "{{ mode }}" != "local" ] && just _shared-services-running 2>/dev/null; then
        USE_SHARED=true
    fi

    if [ "$USE_SHARED" = "true" ]; then
        DB_NAME=$(just _shared-dbname dotcms-core)
        CLUSTER_ID=$(just _worktree-id)
        just _ensure-shared-db dotcms-core
        echo "Starting $IMG on :${PORT} (shared services, db: ${DB_NAME}, namespace: ${WSLUG})"
        ./mvnw -pl :dotcms-core -Pdocker-start,shared-services \
            -Dtomcat.port="$PORT" \
            -Ddotcms.image.name="$IMG" \
            -Dcontext.name="$WSLUG" \
            -Ddot.db.name="$DB_NAME" \
            -Ddot.cluster.id="$CLUSTER_ID"
    else
        echo "Starting $IMG on :${PORT} (namespace: ${WSLUG})"
        ./mvnw -pl :dotcms-core -Pdocker-start \
            -Dtomcat.port="$PORT" \
            -Ddotcms.image.name="$IMG" \
            -Dcontext.name="$WSLUG"
    fi
    just dev-urls

# Starts the dotCMS application in a Docker container on a dynamic port, running in the foreground
dev-run-debug:
    ./mvnw -pl :dotcms-core -Pdocker-start,debug

# Maps paths in the docker container to local paths, useful for development
dev-run-map-dev-paths:
    ./mvnw -pl :dotcms-core -Pdocker-start -Pmap-dev-paths

# Starts the dotCMS application in debug mode with suspension, useful for troubleshooting
dev-run-debug-suspend port="8082":
    ./mvnw -pl :dotcms-core -Pdocker-start,debug-suspend -Dtomcat.port={{ port }}

# Prints all access URLs for the running dotCMS instance. Host ports discovered live from Docker.
dev-urls:
    #!/usr/bin/env bash
    CONTAINER=$(just _dotcms-container) || exit 1
    APP=$(docker port "$CONTAINER" 8080 2>/dev/null | cut -d: -f2)
    MGMT=$(docker port "$CONTAINER" 8090 2>/dev/null | cut -d: -f2)
    echo ""
    echo "  dotAdmin  → http://localhost:${APP}/dotAdmin"
    echo "  REST API  → http://localhost:${APP}/api/v1/"
    echo "  Health    → http://localhost:${MGMT}/dotmgt/livez"
    echo ""

# Stops current worktree's dev containers. In shared mode, stops only the app container.
# In local mode, stops app + sidecars. Safe no-op if not running.
dev-stop:
    #!/usr/bin/env bash
    WSLUG=$(just _worktree-slug)
    CONTAINER="dotbuild_dotcms-core_${WSLUG}_dotcms"
    # Direct stop handles shared-mode containers (not fabric8-managed)
    if docker inspect "$CONTAINER" >/dev/null 2>&1; then
        docker stop "$CONTAINER" 2>/dev/null || true
        docker rm "$CONTAINER" 2>/dev/null || true
    fi
    # Maven stop handles local-mode sidecars (safe no-op if nothing to stop)
    ./mvnw -pl :dotcms-core -Pdocker-stop -Dcontext.name="$WSLUG" 2>/dev/null || true

# Stops all dev containers and restarts on the given port. Use after a backend rebuild.
# Port resolution: explicit > .dev-port > 8082 (same as dev-run).
dev-restart port="" image="" mode="":
    just dev-stop
    just dev-run {{ port }} {{ image }} {{ mode }}

# Starts the Angular frontend dev server on :4200. Discovers backend port from Docker; falls back to :8080.
# port=""  → 4200 (default). Use a different port for parallel worktrees (e.g., 4201, 4202).
dev-start-frontend port="4200":
    #!/usr/bin/env bash
    set -euo pipefail
    if [ -f ".frontend.pid" ] && kill -0 "$(cat .frontend.pid)" 2>/dev/null; then
        echo "Frontend dev server already running (PID=$(cat .frontend.pid))"
        exit 0
    fi
    # Discover the backend app port from Docker. Falls back to 8080 if no container is running.
    # 3-stage discovery: prefer current worktree's container, fall back to any.
    WSLUG=$(just _worktree-slug)
    CONTAINER=$(docker ps --filter "status=running" \
                          --format '{{ "{{.Names}}" }}' \
                          | grep -E "^dotbuild_dotcms-core_${WSLUG}_dotcms$" | head -1 || true)
    if [ -z "$CONTAINER" ]; then
        CONTAINER=$(docker ps --filter "status=running" \
                              --format '{{ "{{.Names}}\t{{.Image}}" }}' \
                              | awk -F'\t' '/dotcms\/dotcms-test:/{print $1}' | head -1)
    fi
    if [ -z "$CONTAINER" ]; then
        CONTAINER=$(docker ps --filter "status=running" \
                              --format '{{ "{{.Names}}" }}' \
                              | grep -E '^dotbuild_.*_dotcms$' | head -1)
    fi
    if [ -n "$CONTAINER" ]; then
        APP_PORT=$(docker port "$CONTAINER" 8080 2>/dev/null | cut -d: -f2)
    fi
    DOTCMS_HOST="http://localhost:${APP_PORT:-8080}"
    FE_PORT="{{ port }}"
    echo "Proxying API to $DOTCMS_HOST — serving on :${FE_PORT}"
    cd core-web
    # DOTCMS_HOST is read by proxy-dev.conf.mjs to set the proxy target.
    # export ensures it's visible to the nohup child process.
    # NX_DAEMON=false prevents the Nx daemon from serializing parallel worktree builds.
    export DOTCMS_HOST
    export NX_DAEMON=false
    NODE_OPTIONS="--max_old_space_size=4096" nohup yarn nx serve dotcms-ui --port="$FE_PORT" > ../.frontend.log 2>&1 &
    disown $!
    echo $! > ../.frontend.pid
    echo "Frontend dev server started (PID=$!) → http://localhost:${FE_PORT}/dotAdmin"
    echo "Logs: just dev-frontend-logs"

# Stops the Angular frontend dev server started by dev-start-frontend
dev-stop-frontend:
    #!/usr/bin/env bash
    PID_FILE=".frontend.pid"
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if kill "$PID" 2>/dev/null; then
            echo "Stopped frontend dev server (PID=$PID)"
        else
            echo "Process $PID already stopped"
        fi
        rm -f "$PID_FILE"
    else
        # No PID file — try to find the process by scanning common frontend ports
        for port in 4200 4201 4202 4203 4204; do
            PID=$(lsof -ti :"$port" 2>/dev/null \
                  || ss -Htlnp "sport = :$port" 2>/dev/null | grep -oE 'pid=[0-9]+' | grep -oE '[0-9]+' \
                  || fuser "$port/tcp" 2>/dev/null \
                  || true)
            if [ -n "$PID" ]; then
                kill "$PID" && echo "Stopped frontend dev server on :$port (PID=$PID)"
                exit 0
            fi
        done
        echo "No frontend dev server found on ports 4200-4204"
    fi

# Tail the Angular frontend dev server log (blocking — Ctrl-C to exit)
dev-frontend-logs:
    tail -f .frontend.log

# Show last 40 lines of the frontend dev server log (non-blocking snapshot)
dev-frontend-status:
    @tail -40 .frontend.log 2>/dev/null || echo "No frontend log found — is the dev server running? (just dev-start-frontend)"

# Blocks until the frontend dev server is serving, with progress and early error detection.
# Fails fast if the build has errors rather than waiting for timeout.
dev-frontend-wait:
    #!/usr/bin/env bash
    set -euo pipefail
    LOG=".frontend.log"
    PID_FILE=".frontend.pid"
    if [ ! -f "$PID_FILE" ]; then
        echo "No frontend server running — start with: just dev-start-frontend" >&2; exit 1
    fi
    PID=$(cat "$PID_FILE")
    # Discover the port from the log (set by dev-start-frontend)
    FE_PORT=$(grep -oE 'serving on :[0-9]+' "$LOG" 2>/dev/null | tail -1 | grep -oE '[0-9]+' || echo "4200")
    echo "Waiting for frontend on :${FE_PORT}..."
    for i in $(seq 1 90); do
        # Fail fast: check if process died
        if ! kill -0 "$PID" 2>/dev/null; then
            echo "Frontend process exited. Last log lines:" >&2
            tail -10 "$LOG" >&2
            exit 1
        fi
        # Fail fast: check for build errors
        if grep -qE "Cannot find module|Unable to resolve|error Command failed" "$LOG" 2>/dev/null; then
            echo "Frontend build failed:" >&2
            grep -E "Cannot find module|Unable to resolve|error Command failed" "$LOG" | head -5 >&2
            exit 1
        fi
        # Check if serving
        HTTP=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:${FE_PORT}/dotAdmin/" 2>/dev/null || echo "000")
        if [ "$HTTP" = "200" ]; then
            echo "Frontend ready → http://localhost:${FE_PORT}/dotAdmin"
            exit 0
        fi
        # Progress: show build phase from log
        if [ $((i % 10)) -eq 0 ]; then
            PHASE=$(tail -1 "$LOG" 2>/dev/null | head -c 80)
            echo "  ${i}s: $PHASE"
        fi
        sleep 3
    done
    echo "Timed out after 270s. Check: just dev-frontend-status" >&2
    exit 1

# Start everything: backend + frontend + wait for both. One command to go from zero to working.
# Starts backend (using .dev-port or default), then frontend, waits for both to be healthy.
dev port="" fe-port="4200":
    #!/usr/bin/env bash
    set -euo pipefail
    echo "Starting dotCMS stack..."

    # 1. Backend
    echo "→ Starting backend..."
    just dev-run {{ port }}

    # 2. Frontend
    echo "→ Starting frontend on :{{ fe-port }}..."
    just dev-start-frontend {{ fe-port }}

    # 3. Wait for frontend
    just dev-frontend-wait

    # 4. Summary
    just dev-urls
    echo ""
    echo "  Frontend → http://localhost:{{ fe-port }}/dotAdmin (live reload)"

# Internal helper: finds the running dotCMS container. Prefers current worktree, falls back to any.
# 3-stage discovery: 1) exact worktree match, 2) any dotcms-test image, 3) name pattern.
_dotcms-container:
    #!/usr/bin/env bash
    WSLUG=$(just _worktree-slug)
    # Stage 1: exact match for current worktree's container
    NAMES=$(docker ps --filter "status=running" \
                      --format '{{ "{{.Names}}" }}' \
                      | grep -E "^dotbuild_dotcms-core_${WSLUG}_dotcms$" || true)
    # Stage 2: any container running a dotcms-test image
    if [ -z "$NAMES" ]; then
        NAMES=$(docker ps --filter "status=running" \
                          --format '{{ "{{.Names}}\t{{.Image}}" }}' \
                          | awk -F'\t' '/dotcms\/dotcms-test:/{print $1}')
    fi
    # Stage 3: name pattern fallback (handles rebuilt images where tag was stripped)
    if [ -z "$NAMES" ]; then
        NAMES=$(docker ps --filter "status=running" \
                          --format '{{ "{{.Names}}" }}' \
                          | grep -E '^dotbuild_.*_dotcms$')
    fi
    if [ -z "$NAMES" ]; then
        echo "No dotCMS container running" >&2; exit 1
    fi
    LINE_COUNT=$(printf '%s\n' "$NAMES" | wc -l | tr -d ' ')
    if [ "$LINE_COUNT" -gt 1 ]; then
        echo "Multiple dotCMS containers running — use 'just dev-containers' to identify:" >&2
        printf '%s\n' "$NAMES" >&2
        exit 1
    fi
    echo "$NAMES"

# Checks dotCMS health via the management port. Host port discovered live from Docker.
dev-health:
    #!/usr/bin/env bash
    CONTAINER=$(just _dotcms-container) || exit 1
    PORT=$(docker port "$CONTAINER" 8090 2>/dev/null | cut -d: -f2)
    if [ -z "$PORT" ]; then
        echo "Management port (8090) not mapped on $CONTAINER" >&2; exit 1
    fi
    RESULT=$(curl -sf "http://localhost:$PORT/dotmgt/livez" 2>/dev/null) || {
        echo "unhealthy — $CONTAINER :$PORT" >&2; exit 1
    }
    echo "$RESULT — $CONTAINER :$PORT"

# Polls until dotCMS is healthy (max 3 min). Use after dev-run or a restart.
dev-wait:
    #!/usr/bin/env bash
    echo -n "Waiting for dotCMS"
    TRIES=0
    until just dev-health > /dev/null 2>&1; do
        printf '.'
        TRIES=$((TRIES + 1))
        if [ "$TRIES" -ge 60 ]; then
            echo " timed out after 3 minutes" >&2; exit 1
        fi
        sleep 3
    done
    echo ""
    just dev-health

# Cleans up Docker volumes associated with the current worktree's development environment
dev-clean-volumes:
    #!/usr/bin/env bash
    WSLUG=$(just _worktree-slug)
    ./mvnw -pl :dotcms-core -Pdocker-clean-volumes -Dcontext.name="$WSLUG"

# Lists locally built dotCMS images with tag, size, and age
dev-images:
    @docker images dotcms/dotcms-test --format 'table {{ "{{.Tag}}\t{{.Size}}\t{{.CreatedSince}}" }}'

# Lists all running dotCMS containers across worktrees
dev-containers:
    @docker ps --filter "name=dotbuild_" --format 'table {{ "{{.Names}}\t{{.Image}}\t{{.Ports}}\t{{.Status}}" }}'

# Stops ALL dotCMS dev containers across all worktrees
dev-stop-all:
    #!/usr/bin/env bash
    CONTAINERS=$(docker ps --filter "name=dotbuild_" -q)
    [ -z "$CONTAINERS" ] && echo "No dotCMS containers running" && exit 0
    echo "$CONTAINERS" | xargs docker stop && echo "$CONTAINERS" | xargs docker rm
    echo "Stopped all dotCMS dev containers"

# Starts the dotCMS application in a Tomcat container on port 8087, running in the foreground
dev-tomcat-run port="8087":
    ./mvnw -pl :dotcms-core -Ptomcat-run -Pdebug -Dservlet.port={{ port }}

dev-tomcat-stop:
    ./mvnw -pl :dotcms-core -Ptomcat-stop -Dcontext.name=local-tomcat

# Starts dotCMS with JMX monitoring enabled for connecting from localhost
dev-run-jmx:
    ./mvnw -pl :dotcms-core -Pdocker-start -Djmx.enable=true

# Starts dotCMS with JMX monitoring on custom ports
dev-run-jmx-ports jmx_port="9999" rmi_port="9998":
    ./mvnw -pl :dotcms-core -Pdocker-start -Djmx.enable=true -Djmx.port={{ jmx_port }} -Djmx.rmi.port={{ rmi_port }}

# Starts dotCMS with both JMX monitoring and debug enabled
dev-run-jmx-debug:
    ./mvnw -pl :dotcms-core -Pdocker-start,jmx-debug -Djmx.debug.enable=true

# Starts dotCMS with JMX, debug, and Glowroot profiler enabled
dev-run-jmx-debug-glowroot:
    ./mvnw -pl :dotcms-core -Pdocker-start,jmx-debug,glowroot -Djmx.debug.enable=true -Ddocker.glowroot.enabled=true

###########################################################
# Shared Services (multi-worktree)
###########################################################

# Starts shared DB + OpenSearch for multi-worktree development. Run once, use from all worktrees.
dev-shared-start:
    docker compose -f docker/docker-compose-shared-services.yml -p dotcms-shared up -d --wait
    @echo ""
    @echo "Shared services ready:"
    @echo "  PostgreSQL → localhost:15432"
    @echo "  OpenSearch → localhost:19200"
    @echo "  OS Upgrade → localhost:19201"
    @echo ""
    @echo "Now run 'just dev-run <port>' in any worktree."
    @echo ""
    @echo "NOTE: These containers auto-restart on boot (~3GB RAM)."
    @echo "Run 'just dev-shared-stop' when you no longer need them."

# Stops shared services (does not remove volumes)
dev-shared-stop:
    docker compose -f docker/docker-compose-shared-services.yml -p dotcms-shared down

# Shows shared services status
dev-shared-status:
    @docker compose -f docker/docker-compose-shared-services.yml -p dotcms-shared ps

# Stops shared services and removes all data volumes
dev-shared-clean:
    docker compose -f docker/docker-compose-shared-services.yml -p dotcms-shared down -v

# Drops current worktree's database from shared PostgreSQL
dev-shared-drop-db module="dotcms-core":
    #!/usr/bin/env bash
    DB_NAME=$(just _shared-dbname {{ module }})
    echo "Dropping database ${DB_NAME}..."
    docker exec dotcms-shared-db dropdb -U postgres --if-exists "${DB_NAME}"

# Lists all worktree databases in shared PostgreSQL
dev-shared-list-dbs:
    @docker exec dotcms-shared-db psql -U postgres -c \
        "SELECT datname, pg_size_pretty(pg_database_size(datname)) AS size FROM pg_database WHERE datname NOT IN ('postgres','template0','template1','dotcms') ORDER BY datname"

# Deletes OpenSearch indexes for the current worktree from both shared instances.
# Index prefix: cluster_<worktree-id>.* (see ESIndexAPI.java:152)
dev-shared-drop-indexes:
    #!/usr/bin/env bash
    set -euo pipefail
    WTID=$(just _worktree-id)
    PREFIX="cluster_${WTID}."
    echo "Deleting indexes matching ${PREFIX}* from shared OpenSearch instances..."
    for container in dotcms-shared-es dotcms-shared-os-upgrade; do
        if docker inspect "$container" --format '{{ "{{.State.Running}}" }}' 2>/dev/null | grep -q true; then
            # List matching indexes first
            INDEXES=$(docker exec "$container" curl -sf "http://localhost:9200/_cat/indices/${PREFIX}*?h=index" 2>/dev/null || true)
            if [ -n "$INDEXES" ]; then
                echo "  ${container}: deleting $(echo "$INDEXES" | wc -l | tr -d ' ') indexes"
                docker exec "$container" curl -sf -X DELETE "http://localhost:9200/${PREFIX}*" >/dev/null
            else
                echo "  ${container}: no indexes matching ${PREFIX}*"
            fi
        else
            echo "  ${container}: not running, skipping"
        fi
    done

# Removes all shared data for the current worktree: database + OpenSearch indexes.
# Safe to run while shared services are still serving other worktrees.
dev-shared-drop-worktree:
    just dev-shared-drop-db
    just dev-shared-drop-indexes
    @echo "Worktree data cleaned from shared services."

# Returns 0 if any dotCMS app containers are using shared services
_shared-services-in-use:
    @docker network inspect dotcms-shared --format '{{ "{{range .Containers}}{{.Name}} {{end}}" }}' 2>/dev/null \
        | grep -q 'dotbuild_'

# Stops shared services only if no worktree app containers are connected.
# Use from automation/agents — safe no-op if other worktrees are still running.
dev-shared-stop-if-idle:
    #!/usr/bin/env bash
    if just _shared-services-in-use 2>/dev/null; then
        echo "Shared services still in use by:"
        docker network inspect dotcms-shared --format '{{ "{{range .Containers}}{{.Name}} {{end}}" }}' 2>/dev/null \
            | tr ' ' '\n' | grep 'dotbuild_' | sed 's/^/  /'
        echo "Not stopping. Use 'just dev-shared-stop' to force."
    else
        echo "No worktrees using shared services. Stopping..."
        just dev-shared-stop
    fi

# Testing Commands

# Executes a specified set of Postman tests
test-postman collections='ai':
    ./mvnw -pl :dotcms-postman verify -Dpostman.test.skip=false -Pdebug -Dpostman.collections={{ collections }}

# Stops Postman-related Docker containers
postman-stop:
    ./mvnw -pl :dotcms-postman -Pdocker-stop -Dpostman.test.skip=false

test-karate collections='KarateCITests#defaults':
    ./mvnw -pl :dotcms-test-karate verify -Dkarate.test.skip=false -Pdebug -Dit.test={{ collections }}

# Runs all integration tests
test-integration:
    ./mvnw -pl :dotcms-integration verify -Dcoreit.test.skip=false

# Runs only the open-search integration tests
test-integration-open-search:
   ./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dopensearch.upgrade.test=true

# Suspends execution for debugging integration tests
test-integration-debug-suspend:
    ./mvnw -pl :dotcms-integration verify -Dcoreit.test.skip=false -Pdebug-suspend

# Just rebuild core and its deps without docker, e.g. pickup changes for it tests
build-core-with-deps:
    ./mvnw install -pl :dotcms-core --am -DskipTests -Ddocker.skip=true

# Just rebuild core without docker, e.g. pickup changes for it tests
build-core-only:
    ./mvnw install -pl :dotcms-core  -DskipTests -Ddocker.skip=true

# Prepares the environment for running integration tests in an IDE
test-integration-ide:
    ./mvnw -pl :dotcms-integration pre-integration-test -Dcoreit.test.skip=false -Dopensearch.upgrade.test=true -Dtomcat.port=8080 -Dmaven.build.cache.enabled=false

# Stops integration test services
test-integration-stop:
    ./mvnw -pl :dotcms-integration -Pdocker-stop -Dcoreit.test.skip=false

test-postman-ide:
    ./mvnw -pl :dotcms-test-postman pre-integration-test -Dpostman.test.skip=false -Dtomcat.port=8080

test-karate-ide:
    ./mvnw -pl :dotcms-test-karate pre-integration-test -Dkarate.test.skip=false -Dtomcat.port=8080

# Executes Playwright E2E tests
# Removes any leftover container from compose or a previous run so Maven can create it
test-e2e:
    ./mvnw -pl :dotcms-ui-e2e verify -De2e.test.skip=false -De2e.test.env=ci -Dmaven.build.cache.skipCache=true

# Docker Commands
# Runs a published dotCMS Docker image on a dynamic port
docker-ext-run tag='latest':
    ./mvnw -pl :dotcms-core -Pdocker-start -Dcontext.name=ext-{{ tag }} -Ddotcms.image.name=dotcms/dotcms:{{ tag }}

# Runs a Docker image from a specific build for testing
docker-test-ext-run tag='main':
    ./mvnw -pl :dotcms-core -Pdocker-start -Dcontext.name=test-ext-{{ tag }} -Ddotcms.image.name=ghcr.io/dotcms/dotcms_test:{{ tag }}

# Stops a running Docker container based on the specified tag
docker-ext-stop tag='latest':
    ./mvnw -pl :dotcms-core -Pdocker-stop -Dcontext.name=ext-{{ tag }}

# Generate a cli uber-jar
cli-build-uber-jar:
    ./mvnw -pl :dotcms-cli package -DskipTests=true

cli-build-native: check-native-deps
    #!/usr/bin/env bash
    set -eo pipefail # Exit on error
    source ${HOME}/.sdkman/bin/sdkman-init.sh  # Load SDKMAN normally in you .bashrc or .zshrc
    # The above is not required when running from your shell if you have SDKMAN in your .bashrc or .zshrc
    sdk env install # ensure right version of java is downloaded and used for the branch
    sdk install java 21.0.2-graalce # Does nothing if already installed
    export GRAALVM_HOME=$(sdk home java 21.0.2-graalce) # Could be added to your .bashrc or .zshrc
    ./mvnw -pl :dotcms-cli -DskipTests -Pnative package


cli-build-test-native: check-native-deps
    #!/usr/bin/env bash
    set -eo pipefail
    source ${HOME}/.sdkman/bin/sdkman-init.sh
    # The above is not required when running from your shell if you have SDKMAN in your .bashrc or .zshrc
    sdk env install # ensure right version of java is downloaded and used for the branch
    sdk install java 21.0.2-graalce # Does nothing if already installed
    export GRAALVM_HOME=$(sdk home java 21.0.2-graalce) # Env var Could be added to your .bashrc or .zshrc
    ./mvnw -pl :dotcms-cli -Pnative verify

run-built-cli *ARGS:
    java -jar tools/dotcms-cli/cli/target/dotcms-cli-1.0.0-SNAPSHOT-runner.jar {{ARGS}}


run-java-cli-native *ARGS:
    tools/dotcms-cli/cli/target/dotcms-cli-1.0.0-SNAPSHOT-runner {{ARGS}}


run-jmeter-tests:
    ./mvnw verify -Djmeter.test.skip=false -pl :dotcms-test-jmeter

###########################################################
# Useful Maven Helper Commands
###########################################################

# Generates a dependency tree for the compile scope and saves it to a file
maven-dependencies:
    ./mvnw dependency:tree -Dscope=compile > dependencies.txt

# Displays updates for project dependencies in a text file
maven-updates:
    ./mvnw versions:display-dependency-updates > updates.txt

# Checks for updates to project dependencies and prints to console
maven-check-updates:
    ./mvnw versions:display-dependency-updates

# Checks for updates to Maven plugins used in the project
maven-check-plugin-updates:
    ./mvnw versions:display-plugin-updates

# Checks for updates to properties defined in the pom.xml that control dependency versions
maven-property-updates:
    ./mvnw versions:display-property-updates

# Check if xcode is installed if on a mac.  Only required for native builds
check-native-deps:
    #!/usr/bin/env bash
    set -eo pipefail
    # Determine the operating system
    OS=$(uname)

    # Install dependencies based on the package manager
    if [ "$OS" = "Linux" ]; then
        if command -v apt-get >/dev/null; then
            sudo apt-get update
            sudo apt-get install -y build-essential libz-dev zlib1g-dev
        elif command -v dnf >/dev/null; then
            sudo dnf install -y gcc glibc-devel zlib-devel libstdc++-static
        else
            echo "Unsupported package manager. Please install the required packages manually. See https://quarkus.io/guides/building-native-image"
            exit 1
        fi
    elif [ "$OS" = "Darwin" ]; then
        if ! command -v xcode-select >/dev/null; then
            echo "Xcode is not installed. Installing Xcode...";
            xcode-select --install;
        else
            echo "Xcode is already installed.";
        fi
    else
        echo "Unsupported operating system. Please install the required packages manually."
        exit 1
    fi

###########################################################
# Dependency Commands
###########################################################

# Installs all dependencies for the current project
install-all-mac-deps: install-jdk-mac check-docker-mac check-git-mac

# Installs SDKMAN for managing Java JDK versions
install-sdkman-mac:
    @if [ -d "${HOME}/.sdkman/bin" ]; then \
        echo "SDKMAN is already installed."; \
    else \
        echo "SDKMAN is not installed, installing now..."; \
        curl -s "https://get.sdkman.io" | bash; \
        source "${HOME}/.sdkman/bin/sdkman-init.sh"; \
    fi

# Installs the latest version of Java JDK using SDKMAN
install-jdk-mac: install-sdkman-mac
    #!/usr/bin/env bash
    set -eo pipefail
    source ~/.sdkman/bin/sdkman-init.sh
    sdk env install java

# Checks if Docker is installed and running
check-docker-mac:
    @if ! command -v docker >/dev/null; then \
        echo "Docker is not installed."; \
        echo "Install docker with : brew install docker --cask"; \
        echo " or download from : https://docs.docker.com/get-docker/"; \
        exit 1; \
    else \
        echo "Docker is installed."; \
        if ! docker info >/dev/null 2>&1; then \
            echo "Docker is not running ..."; \
            exit 1; \
        else \
            echo "Docker is running."; \
        fi; \
    fi

check-git-mac:
    @if ! command -v git >/dev/null; then \
        echo "Git is not installed. Installing Git..."; \
        brew install git; \
    else \
        git --version; \
        echo "Git is already installed."; \
    fi
