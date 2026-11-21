#!/usr/bin/env bash
# Test the throwaway benchmark runner without invoking Maven or running Java tests.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
RUNNER="$ROOT/.github/scripts/benchmark-core-unit-tests.sh"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

if [[ ! -f "$RUNNER" ]]; then
  printf 'FAIL: benchmark runner has not been implemented: %s\n' "$RUNNER" >&2
  exit 1
fi

fixture() {
  rm -rf "${WORK:?}/project" "${WORK:?}/home"
  mkdir -p "$WORK/project/dotCMS/target/classes" "$WORK/home"
  export HOME="$WORK/home" BENCHMARK_TEST_VERSION="9.9.9-BENCHMARK"
  export BENCHMARK_TEST_LOG="$WORK/maven.log" BENCHMARK_TEST_EXIT=0
  : > "$BENCHMARK_TEST_LOG"
  printf 'stale\n' > "$WORK/project/dotCMS/target/classes/stale.class"
  cat > "$WORK/project/mvnw" <<'MAVEN'
#!/usr/bin/env bash
set -euo pipefail
printf '%s\n' "$*" >> "$BENCHMARK_TEST_LOG"
case "$*" in
  *-Dexpression=project.version*) printf '%s\n' "$BENCHMARK_TEST_VERSION" ;;
  *-Dexpression=settings.localRepository*) printf '%s/.m2/repository\n' "$HOME" ;;
  *) exit "$BENCHMARK_TEST_EXIT" ;;
esac
MAVEN
  chmod +x "$WORK/project/mvnw"
  export BENCHMARK_TEST_JARS="$HOME/.m2/repository/com/dotcms/dotcms-core/$BENCHMARK_TEST_VERSION"
  python3 - <<'PY'
import os
from pathlib import Path
from zipfile import ZipFile
root = Path(os.environ['BENCHMARK_TEST_JARS'])
root.mkdir(parents=True)
prefix = 'dotcms-core-' + os.environ['BENCHMARK_TEST_VERSION']
for suffix, files in [
    ('', {'example/Core.class': b'core', 'META-INF/beans.xml': b'production resource'}),
    ('-tests', {'example/CoreTest.class': b'test', 'fixture.json': b'test resource'}),
]:
    with ZipFile(root / (prefix + suffix + '.jar'), 'w') as jar:
        for name, data in files.items():
            jar.writestr(name, data)
PY
}

run_benchmark() {
  (cd "$WORK/project" && bash "$RUNNER" "$1") > "$WORK/output.log" 2>&1
}

assert_no_test_execution() {
  if grep -Eq '(^|[[:space:]])(test|surefire:test)([[:space:]]|$)' "$BENCHMARK_TEST_LOG"; then
    printf 'FAIL: invalid artifacts must be rejected before executing tests\n' >&2
    exit 1
  fi
}

fixture
run_benchmark baseline
grep -Eq '(^|[[:space:]])test([[:space:]]|$)' "$BENCHMARK_TEST_LOG"
[[ ! -e "$WORK/project/dotCMS/target/test-classes/example/CoreTest.class" ]]
printf 'PASS: baseline retains the existing test lifecycle\n'

fixture
run_benchmark reuse
python3 - "$WORK/project" <<'PY'
from pathlib import Path
import sys
root = Path(sys.argv[1]) / 'dotCMS' / 'target'
expected = {
    'classes/example/Core.class': b'core',
    'classes/META-INF/beans.xml': b'production resource',
    'test-classes/example/CoreTest.class': b'test',
    'test-classes/fixture.json': b'test resource',
}
for name, data in expected.items():
    assert (root / name).read_bytes() == data, name
assert not (root / 'classes/stale.class').exists(), 'stale outputs survived restoration'
PY
grep -q 'surefire:test' "$BENCHMARK_TEST_LOG"
grep -Eq '(^|[[:space:]])initialize([[:space:]]|$)' "$BENCHMARK_TEST_LOG"
grep -q -- '-Dmaven.build.cache.enabled=false' "$BENCHMARK_TEST_LOG"
grep -q -- '-DfailIfNoTests=true' "$BENCHMARK_TEST_LOG"
if grep -Eq '(^|[[:space:]])(clean|compile|test-compile|test|package|install|verify)([[:space:]]|$)' "$BENCHMARK_TEST_LOG"; then
  printf 'FAIL: reuse must not invoke a compilation lifecycle\n' >&2
  exit 1
fi
printf 'PASS: reuse restores classes/resources and executes tests without compilation\n'

for suffix in '' '-tests'; do
  fixture
  rm "$BENCHMARK_TEST_JARS/dotcms-core-$BENCHMARK_TEST_VERSION$suffix.jar"
  if run_benchmark reuse; then
    printf 'FAIL: missing %s JAR was accepted\n' "${suffix:-production}" >&2
    exit 1
  fi
  assert_no_test_execution
  printf 'PASS: missing %s JAR fails before tests\n' "${suffix:-production}"
done

for suffix in '' '-tests'; do
  fixture
  printf 'not a zip archive\n' > "$BENCHMARK_TEST_JARS/dotcms-core-$BENCHMARK_TEST_VERSION$suffix.jar"
  if run_benchmark reuse; then
    printf 'FAIL: corrupt %s JAR was accepted\n' "${suffix:-production}" >&2
    exit 1
  fi
  assert_no_test_execution
  printf 'PASS: corrupt %s JAR fails before tests\n' "${suffix:-production}"
done

for mode in baseline reuse; do
  fixture
  export BENCHMARK_TEST_EXIT=17
  if run_benchmark "$mode"; then
    printf 'FAIL: %s swallowed a Maven failure\n' "$mode" >&2
    exit 1
  fi
  printf 'PASS: %s propagates a Maven failure\n' "$mode"
done
