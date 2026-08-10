#!/usr/bin/env bash
#
# Three experiments on sealing the Permissionable hierarchy. Needs nothing but a JDK 22+ — no Maven,
# no dotCMS classpath, no network. Every claim in README.md is this script's output.
#
#   1. Sealed across packages, unnamed module  -> fails, and the error is the whole point
#   2. The same sources inside a named module  -> compiles, resolver has no `default`
#   3. Experiment 2 plus one more asset type   -> the resolver stops compiling on its own
#
# The only delta between 1 and 2 is whether module-info.java is handed to javac.

set -u

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SRC="${HERE}/src"
WORK="$(mktemp -d)"
trap 'rm -rf "${WORK}"' EXIT

rule() { printf '\n\033[1m%s\033[0m\n%s\n' "$1" "$(printf '─%.0s' {1..78})"; }

# ── 1. Unnamed module: every permitted subtype must be in the same package ───────────────────────
rule "1. Sealed across packages, WITHOUT module-info.java (the unnamed module)"
javac --release 22 -d "${WORK}/out1" \
      $(find "${SRC}" -name '*.java' ! -name 'module-info.java') 2>&1 | sed 's|^.*/src/|src/|'
echo "exit: ${PIPESTATUS[0]}   (non-zero expected — this is the wall)"

# ── 2. Named module: permits may cross packages ──────────────────────────────────────────────────
rule "2. The very same sources, WITH module-info.java"
javac --release 22 -d "${WORK}/out2" \
      $(find "${SRC}" -name '*.java') 2>&1 | sed 's|^.*/src/|src/|'
echo "exit: ${PIPESTATUS[0]}   (0 expected — sealed across packages, and the resolver has no default)"

# ── 3. Add an asset type; nobody touches the resolver ────────────────────────────────────────────
rule "3. One more permitted type — WorkflowAction — and the resolver is left untouched"
cp -r "${SRC}" "${WORK}/src3"
mkdir -p "${WORK}/src3/com/dotmarketing/portlets/workflows/model"
cat > "${WORK}/src3/com/dotmarketing/portlets/workflows/model/WorkflowAction.java" <<'JAVA'
package com.dotmarketing.portlets.workflows.model;

import com.dotmarketing.business.Permissionable;

public final class WorkflowAction implements Permissionable {

    public String getPermissionType() {
        return "WorkflowAction";
    }
}
JAVA
# add it to the permits clause, and nothing else
perl -0pi -e 's/permits Contentlet, Folder, Identifier, Inode \{/permits Contentlet, Folder, Identifier, Inode,\n        com.dotmarketing.portlets.workflows.model.WorkflowAction {/' \
     "${WORK}/src3/com/dotmarketing/business/Permissionable.java"

javac --release 22 -d "${WORK}/out3" \
      $(find "${WORK}/src3" -name '*.java') 2>&1 | sed "s|^${WORK}/src3/|src/|"
echo "exit: ${PIPESTATUS[0]}   (non-zero expected — the compiler hands you the work list)"
