#!/usr/bin/env bash
#
# The same question as ../run.sh, asked of the REAL sources instead of a model: what does the actual
# dotCMS compiler say when Permissionable, Inode and ContentType are declared `sealed` in place?
#
# Each stage applies one patch to dotCMS/src/main/java, runs the real Maven compile — with the real
# classpath, the real Immutables annotation processor and the real 8,000-odd source files — records
# what came back, and reverts. The tree is left exactly as it was found.
#
#   A   seal Permissionable                       -> the wall, priced: one line changed, N errors
#   B   seal Inode                                -> the same wall one level down
#   B2  seal Inode with one subtype left out       -> control: does javac name what is missing?
#   C   seal ContentType, in its own package       -> no module rule in the way. Something else is.
#   D   seal SimpleContentType over its GENERATED subclasses -> the annotation-processor question
#
# Roughly 45s per stage. Needs the dotCMS build prerequisites (Java 25 via `sdk env install`).

set -uo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${HERE}/../../../.." && pwd)"
PATCHES="${HERE}/patches"
RESULTS="${HERE}/results"
JAVA_SRC="dotCMS/src/main/java"

cd "${ROOT}"

if ! git diff --quiet -- "${JAVA_SRC}"; then
    echo "refusing to run: ${JAVA_SRC} has uncommitted changes, and this script reverts with git checkout."
    exit 2
fi

revert() { git checkout --quiet -- "${JAVA_SRC}" 2>/dev/null || true; }
trap revert EXIT

mkdir -p "${RESULTS}"
rule() { printf '\n\033[1m%s\033[0m\n%s\n' "$1" "$(printf '─%.0s' {1..78})"; }

# count <log> <needle> — distinct occurrences. Annotation processing makes javac report every error
# twice, once per round, so the raw count is double throughout; `sort -u` is what makes it honest.
count() { grep "error:" "$1" | sort -u | grep -c "$2"; }

stage() {
    local patch="$1" title="$2"
    rule "${title}"
    local log="${RESULTS}/$(basename "${patch}" .patch).log"

    git apply "${PATCHES}/${patch}" || { echo "patch no longer applies — the anchors moved"; return 1; }
    ./mvnw compile -pl :dotcms-core -DskipTests -Dmaven.build.cache.enabled=false > "${log}" 2>&1
    local status=$?
    revert

    # trimmed, committable evidence next to the raw log (which is gitignored)
    {
        echo "# $(basename "${patch}" .patch)"
        echo "#"
        echo "# exit ${status}; javac reports each diagnostic twice, once per annotation-processing round"
        echo
        grep "error:" "${log}" | sed "s|.*/core/||; s|^\[ERROR\] ||" | sort -u
    } > "${log%.log}.errors.txt"

    if [[ ${status} -eq 0 ]]; then
        echo "BUILD SUCCESS — it compiles."
    else
        echo "distinct errors: $(grep 'error:' "${log}" | sort -u | wc -l | tr -d ' ')"
        printf '  %3s  cannot extend a sealed class in a different package\n' "$(count "${log}" 'in a different package')"
        printf '  %3s  sealed, non-sealed or final modifiers expected\n'      "$(count "${log}" 'modifiers expected')"
        printf '  %3s  anonymous classes must not extend sealed classes\n'    "$(count "${log}" 'anonymous classes must not')"
        printf '  %3s  class is not allowed to extend sealed class\n'         "$(count "${log}" 'not allowed to extend')"
    fi
    echo "exit: ${status}   (full output: results/$(basename "${log}"))"
}

stage A-seal-permissionable.patch \
      "A. The real Permissionable, sealed over all 22 of its declared subtypes — ONE file changed"
stage B-seal-inode.patch \
      "B. The real Inode, sealed over its six direct subclasses — ONE file changed"
stage B2-seal-inode-incomplete.patch \
      "B2. Control: the same clause with UserComment left out. Does javac name it?"
stage C-seal-contenttype.patch \
      "C. ContentType, sealed in place — all nine subclasses share its package, so packages are not the issue"
stage D-seal-simplecontenttype-generated.patch \
      "D. SimpleContentType, sealed over the classes Immutables GENERATES for it"

rule "Done"
git diff --quiet -- "${JAVA_SRC}" && echo "tree reverted cleanly." || echo "WARNING: ${JAVA_SRC} is still dirty."
