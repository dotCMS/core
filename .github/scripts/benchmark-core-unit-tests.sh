#!/usr/bin/env bash
# Throwaway experiment for #38326; not used by the production test matrix.
set -euo pipefail

mode="${1:?expected baseline or reuse}"
args=(-e -B --no-transfer-progress --show-version -Dprod=true -pl :dotcms-core
  -Dmaven.build.cache.enabled=false -DskipTests=false -DfailIfNoTests=true)

case "$mode" in
  baseline)
    ./mvnw "${args[@]}" test 2>&1 | tee benchmark-maven.log
    ;;
  reuse)
    version="$(./mvnw -q -pl :dotcms-core help:evaluate -Dexpression=project.version \
      -DforceStdout -Dstyle.color=never -Dmaven.build.cache.enabled=false | tail -n 1)"
    if [[ ! "$version" =~ ^[a-zA-Z0-9][a-zA-Z0-9._+-]*$ ]]; then
      printf 'Invalid Maven project version: %s\n' "$version" >&2
      exit 1
    fi
    python3 - "$HOME/.m2/repository/com/dotcms/dotcms-core/$version" "$version" <<'PY'
from pathlib import Path, PurePosixPath
import shutil
import sys
from zipfile import ZipFile

repository, version = Path(sys.argv[1]), sys.argv[2]
outputs = [('classes', ''), ('test-classes', '-tests')]
# Validate both archives before removing any previous output.
for directory, suffix in outputs:
    archive = repository / f'dotcms-core-{version}{suffix}.jar'
    with ZipFile(archive) as jar:
        names = jar.namelist()
        if not any(name.endswith('.class') for name in names):
            raise ValueError(f'No compiled classes in {archive}')
        for name in names:
            path = PurePosixPath(name)
            if path.is_absolute() or '..' in path.parts or '\\' in name:
                raise ValueError(f'Unsafe archive entry: {name}')
        if jar.testzip() is not None:
            raise ValueError(f'Corrupt archive: {archive}')
for directory, suffix in outputs:
    destination = Path('dotCMS/target') / directory
    if destination.exists():
        shutil.rmtree(destination)
    destination.mkdir(parents=True)
    with ZipFile(repository / f'dotcms-core-{version}{suffix}.jar') as jar:
        jar.extractall(destination)
print(f'Restored core production and test outputs for {version}')
PY
    # initialize supplies the Byte Buddy agent path without running compilation.
    ./mvnw "${args[@]}" initialize surefire:test 2>&1 | tee benchmark-maven.log
    ;;
  *)
    printf 'Unknown benchmark mode: %s\n' "$mode" >&2
    exit 1
    ;;
esac
