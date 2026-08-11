#!/usr/bin/env bash
#
# Experiments on sealing the Permissionable / Inode hierarchy. Needs nothing but a JDK 22+ — no Maven,
# no dotCMS classpath, no network. Every claim in README.md is this script's output.
#
#   1. Seal it where it lives, unnamed module     -> fails, and the errors are the whole point
#   2. The same sources inside a named module     -> compiles, both resolvers carry no `default`
#   3. The Inode permits clause people expect     -> fails; the real hierarchy is not that shape
#   4. Add an asset type, touch no resolver       -> the top-level switch stops compiling
#   5. Add an Inode subclass, touch no resolver   -> compiles in SILENCE, until Inode goes abstract
#   6. Drop the eight sub-interface cases         -> not exhaustive; the tax, priced
#   7. An OSGi-style plugin in its own module     -> compile error, then IncompatibleClassChangeError
#
# The only delta between 1 and 2 is whether module-info.java is handed to javac.

set -u

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SRC="${HERE}/src"
WORK="$(mktemp -d)"
LOG="${WORK}/javac.log"
trap 'rm -rf "${WORK}"' EXIT

REL=22   # unnamed patterns (`case Host _`) are final in 22

rule() { printf '\n\033[1m%s\033[0m\n%s\n' "$1" "$(printf '─%.0s' {1..78})"; }
srcs() { find "$1" -name '*.java' ${2:+! -name "$2"}; }

# compile <out-dir> <src-dir> [skip-file] [output-filter] -> prints cleaned diagnostics, sets STATUS
# The filter is applied inside the function on purpose: piping the call itself would lose STATUS.
STATUS=0
compile() {
    local out="$1" dir="$2" skip="${3:-}" filter="${4:-cat}"
    javac --release ${REL} -d "${out}" $(srcs "${dir}" "${skip}") > "${LOG}" 2>&1
    STATUS=$?
    sed "s|^${WORK}/[a-zA-Z0-9]*/|src/|; s|^${SRC}/|src/|" "${LOG}" | eval "${filter}"
}
verdict() { echo "exit: ${STATUS}   ($1)"; }

# ── 1. Unnamed module: every permitted subtype must be in the same package ───────────────────────
rule "1. Sealed across packages, WITHOUT module-info.java (the unnamed module)"
compile "${WORK}/out1" "${SRC}" module-info.java "grep -E 'error:' | head -5"
echo "     ...one error per permitted subtype, in every permits clause — $(grep -c 'error:' "${LOG}") in total"
verdict "non-zero expected — this is the wall"

# ── 2. Named module: permits may cross packages ──────────────────────────────────────────────────
rule "2. The same sources WITH module-info.java — Permissionable, Inode, WebAsset, Contentlet sealed"
compile "${WORK}/out2" "${SRC}"
verdict "0 expected — sealed two levels deep, and neither resolver has a default"

# ── 3. The Inode permits clause everyone writes from memory ──────────────────────────────────────
rule "3. Inode permits IHTMLPage, Container, Link, Contentlet — the shape people expect"
cp -r "${SRC}" "${WORK}/src3"
cat > "${WORK}/src3/com/dotmarketing/beans/Inode.java" <<'JAVA'
package com.dotmarketing.beans;

import com.dotmarketing.business.Permissionable;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.links.model.Link;

public sealed class Inode implements Permissionable permits

        IHTMLPage, Container, Link, Contentlet {
}
JAVA
compile "${WORK}/out3" "${WORK}/src3" "" "head -22"
echo "     ...$(grep -c 'not allowed to extend' "${LOG}") of those in total: one per real Inode subclass the clause left out"
verdict "non-zero expected — and every error is a fact about the hierarchy"

# ── 4. A new asset type at the top level ─────────────────────────────────────────────────────────
rule "4. A fifteenth Permissionable — Experiment — and PermissionResolver is left untouched"
cp -r "${SRC}" "${WORK}/src4"
mkdir -p "${WORK}/src4/com/dotcms/experiments/model"
cat > "${WORK}/src4/com/dotcms/experiments/model/Experiment.java" <<'JAVA'
package com.dotcms.experiments.model;

import com.dotmarketing.business.Permissionable;

/** A new asset type, added the way asset types actually get added. */
public final class Experiment implements Permissionable {
}
JAVA
perl -0pi -e 's/(Rule, UserProxy, PermissionableProxy,)/$1\n        com.dotcms.experiments.model.Experiment,/' \
     "${WORK}/src4/com/dotmarketing/business/Permissionable.java"
compile "${WORK}/out4" "${WORK}/src4"
verdict "non-zero expected — the compiler hands you the work list"

# ── 5. A new Inode subclass. This is the one that does not go the way it should ───────────────────
rule "5a. A seventh Inode subclass — Rating — and neither resolver is touched"
cp -r "${SRC}" "${WORK}/src5"
mkdir -p "${WORK}/src5/com/dotmarketing/portlets/rating/model"
cat > "${WORK}/src5/com/dotmarketing/portlets/rating/model/Rating.java" <<'JAVA'
package com.dotmarketing.portlets.rating.model;

import com.dotmarketing.beans.Inode;

/** A new Inode subclass. Nothing above the Inode branch has any reason to know it exists. */
public final class Rating extends Inode {
}
JAVA
perl -0pi -e 's/(WebAsset, Structure, Field, Category, FileUpload, UserComment)/$1,\n        com.dotmarketing.portlets.rating.model.Rating/' \
     "${WORK}/src5/com/dotmarketing/beans/Inode.java"
compile "${WORK}/out5" "${WORK}/src5"
verdict "0 — SILENCE. Rating takes the 'case Inode _' arm, which exists only because Inode is concrete"

rule "5b. The same addition, with Inode made abstract and that last case removed"
cp -r "${WORK}/src5" "${WORK}/src5b"
perl -0pi -e 's/public sealed class Inode/public sealed abstract class Inode/' \
     "${WORK}/src5b/com/dotmarketing/beans/Inode.java"
perl -0pi -e 's/[^\n]*the experiment did not see coming.*?case Inode _ ->[^\n]*\n//s' \
     "${WORK}/src5b/com/dotmarketing/business/InodePermissionResolver.java"
compile "${WORK}/out5b" "${WORK}/src5b"
verdict "non-zero expected — now, and only now, sealing Inode earns its keep"

# ── 6. What the eight non-sealed sub-interfaces cost ─────────────────────────────────────────────
rule "6. Delete the eight sub-interface cases from PermissionResolver, change nothing else"
cp -r "${SRC}" "${WORK}/src6"
perl -0pi -e 's/[^\n]*the eight-case tax.*?case IHTMLPage _ ->[^\n]*\n//s' \
     "${WORK}/src6/com/dotmarketing/business/PermissionResolver.java"
echo "     removed: $(( $(grep -cE '^ +case ' "${SRC}/com/dotmarketing/business/PermissionResolver.java") - $(grep -cE '^ +case ' "${WORK}/src6/com/dotmarketing/business/PermissionResolver.java") )) cases, out of $(grep -cE '^ +case ' "${SRC}/com/dotmarketing/business/PermissionResolver.java")"
compile "${WORK}/out6" "${WORK}/src6"
verdict "non-zero expected — sealing the root does not close its sub-interfaces"

# ── 7. The OSGi wall: a plugin lives in another module, by definition ────────────────────────────
rule "7a. An OSGi-style plugin implements the sealed Permissionable from its own module"
mkdir -p "${WORK}/plugin/com/acme/plugin"
cat > "${WORK}/plugin/module-info.java" <<'JAVA'
module dotcms.plugin {
    requires dotcms.permissions;
}
JAVA
cat > "${WORK}/plugin/com/acme/plugin/AcmeAsset.java" <<'JAVA'
package com.acme.plugin;

import com.dotmarketing.business.Permissionable;

/** What a dotCMS plugin that owns a permissionable asset does today, with no ceremony at all. */
public final class AcmeAsset implements Permissionable {
}
JAVA
cat > "${WORK}/plugin/com/acme/plugin/AcmeMain.java" <<'JAVA'
package com.acme.plugin;

public final class AcmeMain {

    public static void main(final String... args) {
        System.out.println("loaded: " + new AcmeAsset().getPermissionType());
    }
}
JAVA
javac --release ${REL} --module-path "${WORK}/out2" -d "${WORK}/outPlugin" \
      $(srcs "${WORK}/plugin") > "${LOG}" 2>&1
STATUS=$?
sed "s|^${WORK}/plugin/|plugin/|" "${LOG}"
verdict "non-zero expected — and there is no permits clause it could be added to: see 7b"

rule "7b. The same plugin, compiled against the API as published today, run against a sealed one"
mkdir -p "${WORK}/api/com/dotmarketing/business"
cat > "${WORK}/api/module-info.java" <<'JAVA'
module dotcms.permissions {
    exports com.dotmarketing.business;
}
JAVA
cat > "${WORK}/api/com/dotmarketing/business/Permissionable.java" <<'JAVA'
package com.dotmarketing.business;

/** Permissionable as a plugin sees it today: an ordinary, open interface. */
public interface Permissionable {

    default String getPermissionType() {
        return getClass().getCanonicalName();
    }
}
JAVA
javac --release ${REL} -d "${WORK}/outApi" $(srcs "${WORK}/api") 2>&1
javac --release ${REL} --module-path "${WORK}/outApi" -d "${WORK}/outPlugin2" $(srcs "${WORK}/plugin") 2>&1
echo "     the plugin compiles fine against the open API (exit ${?}). Now swap in the sealed module:"
java --module-path "${WORK}/out2:${WORK}/outPlugin2" -m dotcms.plugin/com.acme.plugin.AcmeMain > "${LOG}" 2>&1
STATUS=$?
head -2 "${LOG}"
verdict "non-zero expected — the same-module rule is enforced again at class load"

rule "Done"
echo "Nothing in this directory is part of the Maven build. Exactly two of these are supposed to pass."
