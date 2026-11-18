# Contract: The append-only write to `tasks.md`

**Feature**: `37267-converge-closing-step`

The single write both skills in the chain are permitted to make. This is AC-004, stated
precisely enough to test.

---

## The write

Exactly one operation: append one `## Phase N: …` section to the **end** of `tasks.md`.
Nothing else in the file is read-modify-written, and no other file is written at all.

| Skill | Header it writes |
|---|---|
| `/speckit-converge` (shipped) | `## Phase N: Convergence` |
| `/speckit-docs-converge` (new) | `## Phase N: Documentation Convergence` |

Distinct headers so a reader can tell which pass produced which tasks. Since docs-converge runs
*after* converge in the same chain, its phase number is naturally one higher.

### Numbering

Both computed **at write time**, after the previous skill in the chain has already written:

- `N` = highest existing phase number in `tasks.md`, + 1
- Task IDs = `T{M+1:03d}`, `T{M+2:03d}`, … where `M` = highest existing `T###` **anywhere** in
  the file (not just in the last phase)

Sequential invocation makes this deterministic; no coordination between the skills is needed.

### Task line format

```markdown
- [ ] T042 <imperative description> in <file-path> per <source-ref> (<gap-type>)
```

- `<file-path>` is **mandatory** for documentation tasks — AC-004 requires each to name the file
  to update. A task a developer cannot act on without re-deriving the target is not actionable.
- `<source-ref>`: `FR-003`, `SC-002`, `US1/AC2`, `plan: storage decision`, `Constitution IV`.
- `<gap-type>`: `missing` | `partial` | `contradicts` | `unrequested`.

### Ordering within a phase

Severity-descending: `CRITICAL` → `HIGH` → `MEDIUM` → `LOW`.

AC-003's *"documentation findings are ordered after constitution violations"* is satisfied
**structurally**: converge writes constitution-violation tasks first, in the earlier phase; every
documentation task lands in the later phase. No cross-skill sorting is required, and none is
attempted.

---

## Invariants

1. **Zero findings ⇒ zero bytes.** `converged` leaves the file **byte-for-byte unchanged** — not
   even an empty phase header. Verified by comparing `sha256sum tasks.md` before and after.
2. **Existing IDs are immutable.** Never reused, renumbered, reordered, or deleted.
3. **Prior phases are immutable.** A second Documentation Convergence phase is appended *below*
   the first; the earlier one is not touched, merged, or pruned.
4. **`tasks.md` is the only file written.** Not `spec.md`, not `plan.md`, not `docs/`, not
   `openapi.yaml`, not source.
5. **Append at the end**, never inserted mid-file.
6. **No duplicate findings** *(clarification 2026-08-28)*. A finding already represented by a task
   in a prior Convergence phase — checked or unchecked — is not appended again. This is what makes
   the loop terminate: successive passes yield strictly fewer new tasks, and a checked task marks
   a finding the developer consciously accepted, which must not be re-raised.
7. **Nothing is written during an incomplete run** *(clarification 2026-08-28)*. If `tasks.md`
   still has unchecked non-`[GATE]` tasks, the pass reports `implementation_incomplete` and
   writes zero bytes.

> Invariants 6 and 7 are implemented in `speckit-docs-converge` only. The shipped
> `/speckit-converge` is not edited (research R1), so its findings may recur; Quick Start §3/§9
> handle that by defining the gate as *converged, or every remaining finding consciously
> accepted*.

---

## Why `tasks.md` and nothing else

`tasks.md` is gitignored by design (Quick Start §8 — a *process artifact*, not a durable
reference). That is what makes an automatic, mandatory, appending hook safe: **the chain's output
never reaches PR 2.** A reviewer sees the code and the docs the tasks produced, never the
bookkeeping.

It also constrains the emitted tasks: they must be self-contained instructions to change a named
file, because the record of *why* they existed disappears with the untracked file.

---

## Conformance checks

```bash
T=specs/<feature>/tasks.md

# Invariant 1 — byte-for-byte no-op on a complete feature
before=$(shasum -a 256 "$T"); /speckit-docs-converge; after=$(shasum -a 256 "$T")
[ "$before" = "$after" ] && echo "converged: no-op OK"

# Invariants 2+3 — nothing above the new phase moved
cp "$T" /tmp/before.md; /speckit-docs-converge
diff <(sed -n '1,/^## Phase.*Documentation Convergence/p' /tmp/before.md) \
     <(sed -n '1,/^## Phase.*Documentation Convergence/p' "$T")

# Invariant 4 — tasks.md is the only file written (it is gitignored, so the tree stays clean)
git status --porcelain   # must show no unexpected modifications
```
