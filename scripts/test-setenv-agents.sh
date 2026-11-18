#!/bin/sh
# Checks the opt-in JVM agent wiring in setenv.sh (Glowroot, OpenTelemetry, Pyroscope).
# Usage: scripts/test-setenv-agents.sh   (exits non-zero on the first failure)

SETENV="$(cd "$(dirname "$0")/.." && pwd)/dotCMS/src/main/resources/container/tomcat9/bin/setenv.sh"
WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT

# Source setenv.sh in a clean env with the given VAR=value pairs, then print the variable named last.
run() {
    want=$1; shift
    env -i PATH="$PATH" CATALINA_HOME="$WORK" CATALINA_TMPDIR="$WORK" DOTCMS_DISABLE_TEMP_CLEANUP=true \
        JAVA_OPTS_MEMORY= JAVA_OPTS_DIRECT= "$@" \
        sh -c '. "$0" >/dev/null 2>&1; eval "printf %s \"\$$1\""' "$SETENV" "$want"
}

fail() { echo "FAIL: $1"; exit 1; }
has() { case "$1" in *"$2"*) ;; *) fail "$3: expected '$2' in '$1'" ;; esac; }
hasnt() { case "$1" in *"$2"*) fail "$3: did not expect '$2' in '$1'" ;; esac; }

# Off by default: no agent attached, no OTEL_/PYROSCOPE_ defaults leaked into the env
opts=$(run CATALINA_OPTS)
hasnt "$opts" "opentelemetry-javaagent.jar" "default off (otel)"
hasnt "$opts" "pyroscope.jar" "default off (pyroscope)"
hasnt "$opts" "glowroot.jar" "default off (glowroot)"
[ -z "$(run OTEL_TRACES_SAMPLER)" ] || fail "default off: OTEL_TRACES_SAMPLER was exported"
[ -z "$(run PYROSCOPE_PROFILER_EVENT)" ] || fail "default off: PYROSCOPE_PROFILER_EVENT was exported"

# OTel on: one agent, traces-only defaults
opts=$(run CATALINA_OPTS OTEL_JAVAAGENT_ENABLED=true)
has "$opts" "-javaagent:$WORK/otel/opentelemetry-javaagent.jar" "otel on"
[ "$(printf '%s' "$opts" | grep -o 'opentelemetry-javaagent.jar' | wc -l | tr -d ' ')" = 1 ] || fail "otel on: agent added more than once"
[ "$(run OTEL_SERVICE_NAME OTEL_JAVAAGENT_ENABLED=true)" = dotcms ] || fail "otel on: service name default"
[ "$(run OTEL_METRICS_EXPORTER OTEL_JAVAAGENT_ENABLED=true)" = none ] || fail "otel on: metrics exporter default"
[ "$(run OTEL_LOGS_EXPORTER OTEL_JAVAAGENT_ENABLED=true)" = none ] || fail "otel on: logs exporter default"
[ "$(run OTEL_TRACES_SAMPLER_ARG OTEL_JAVAAGENT_ENABLED=true)" = 0.1 ] || fail "otel on: sampler arg default"

# Operator values win over defaults
[ "$(run OTEL_TRACES_SAMPLER OTEL_JAVAAGENT_ENABLED=true OTEL_TRACES_SAMPLER=always_on)" = always_on ] || fail "otel override: sampler"
[ "$(run OTEL_METRICS_EXPORTER OTEL_JAVAAGENT_ENABLED=true OTEL_METRICS_EXPORTER=otlp)" = otlp ] || fail "otel override: metrics exporter"
# service.name given via resource attributes must not be shadowed by the OTEL_SERVICE_NAME default
[ -z "$(run OTEL_SERVICE_NAME OTEL_JAVAAGENT_ENABLED=true OTEL_RESOURCE_ATTRIBUTES=service.name=tenant-a,env=prod)" ] || fail "otel override: service.name in resource attributes"

# Pyroscope on
opts=$(run CATALINA_OPTS PYROSCOPE_AGENT_ENABLED=true)
has "$opts" "-javaagent:$WORK/pyroscope/pyroscope.jar" "pyroscope on"
hasnt "$opts" "opentelemetry-javaagent.jar" "pyroscope on (otel stays off)"
[ "$(run PYROSCOPE_PROFILER_EVENT PYROSCOPE_AGENT_ENABLED=true)" = itimer ] || fail "pyroscope on: profiler event default"
[ "$(run PYROSCOPE_APPLICATION_NAME PYROSCOPE_AGENT_ENABLED=true PYROSCOPE_APPLICATION_NAME=cust)" = cust ] || fail "pyroscope override: application name"

# Anything other than "true" leaves the agents off
opts=$(run CATALINA_OPTS OTEL_JAVAAGENT_ENABLED=false PYROSCOPE_AGENT_ENABLED=yes)
hasnt "$opts" "-javaagent:$WORK/otel" "otel=false"
hasnt "$opts" "-javaagent:$WORK/pyroscope" "pyroscope=yes"

# Glowroot enabled but its jar removed from the image: start without it instead of failing JVM startup
opts=$(run CATALINA_OPTS GLOWROOT_ENABLED=true OTEL_JAVAAGENT_ENABLED=true)
hasnt "$opts" "glowroot" "glowroot jar missing"
has "$opts" "-javaagent:$WORK/otel/opentelemetry-javaagent.jar" "glowroot jar missing (otel unaffected)"

# Alongside Glowroot: all three agents attach, and Glowroot's own settings are unchanged
mkdir -p "$WORK/glowroot" && : > "$WORK/glowroot/glowroot.jar"
opts=$(run CATALINA_OPTS GLOWROOT_ENABLED=true OTEL_JAVAAGENT_ENABLED=true PYROSCOPE_AGENT_ENABLED=true)
has "$opts" "-javaagent:$WORK/glowroot/glowroot.jar" "all three (glowroot)"
has "$opts" "-Dglowroot.conf.dir=" "all three (glowroot conf)"
has "$opts" "-javaagent:$WORK/otel/opentelemetry-javaagent.jar" "all three (otel)"
has "$opts" "-javaagent:$WORK/pyroscope/pyroscope.jar" "all three (pyroscope)"

echo "PASS: setenv.sh agent wiring"
