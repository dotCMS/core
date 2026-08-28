# Contract: The hook chain

**Feature**: `37267-converge-closing-step`

Defines the exact content of `.specify/extensions.yml` after this change, and the dispatch
semantics the shipped skills already implement. This is the whole of AC-002.

---

## Target file: `.specify/extensions.yml`

Existing `before_plan` entry is preserved verbatim (AC-005). Two entries are added.

```yaml
hooks:
  before_plan:
    command: speckit.adr-context
    optional: false
    description: "Consult dotCMS/platform-adrs for ADRs relevant to this work before planning (binding input; never creates ADRs)"

  after_implement:
    command: speckit.converge
    optional: false
    description: "Assess the codebase against spec/plan/tasks and append remaining work; the mandatory closing step before PR 2"

  after_converge:
    command: speckit.docs-converge
    optional: false
    description: "Extend the gap analysis to documentation drift (docs/, CLAUDE.md, openapi.yaml, Javadoc); append-only, never edits docs"
```

The file's header comment must be extended to explain the chain, matching the existing comment
style (it currently explains only the ADR hook's purpose).

### Conformance checks

```bash
python3 -c "
import yaml; h = yaml.safe_load(open('.specify/extensions.yml'))['hooks']
assert h['after_implement']['command'] == 'speckit.converge'
assert h['after_implement']['optional'] is False
assert h['after_converge']['command'] == 'speckit.docs-converge'
assert h['after_converge']['optional'] is False
assert h['before_plan']['command'] == 'speckit.adr-context'   # unchanged
print('extensions.yml OK')"
```

---

## Dispatch semantics (already implemented — do not re-implement)

Verified in the shipped skills. **No skill file needs editing to make this work.**

| Hook key | Dispatched by | Where |
|---|---|---|
| `after_implement` | `.claude/skills/speckit-implement/SKILL.md` | §"Mandatory Post-Execution Hooks" (≈ lines 182–225) |
| `after_converge` | `.claude/skills/speckit-converge/SKILL.md` | Execution Step 9 |

Both implement the identical algorithm:

1. If `.specify/extensions.yml` is absent → skip silently.
2. If the YAML **fails to parse** → skip hook checking **silently** and continue.
3. Drop hooks with `enabled: false`; absent `enabled` means enabled.
4. If a hook declares a **non-empty `condition`** → **skip it** (evaluation is left to a
   HookExecutor that does not exist here).
5. Map `command` to a slash command: `.` → `-`.
6. `optional: false` → emit the `EXECUTE_COMMAND:` block **and actually invoke** the command,
   waiting for it to finish.

### Two failure modes this creates, both silent

Steps 2 and 4 are the reason the conformance check above is an explicit task rather than a
review item:

- **Malformed YAML** disables *every* hook — including the existing ADR gate — with no error
  shown to anyone.
- **A stray `condition:` key** disables just that hook, equally silently.

Neither produces a message. The only defense is asserting the parse and the key values.

---

## Resulting flow

```
/speckit-implement
        │ completes the task list
        ▼ after_implement (optional: false)
/speckit-converge ──── code findings ────▶ appends "## Phase N: Convergence"
        │
        ▼ after_converge (optional: false)
/speckit-docs-converge ── doc findings ──▶ appends "## Phase N+1: Documentation Convergence"
        │
        ├── either appended tasks ──▶ developer re-runs /speckit-implement ──▶ (loop)
        └── both converged ─────────▶ open PR 2
```

**The loop is developer-driven.** Neither skill re-invokes `/speckit-implement`; the chain runs
once per implement invocation and terminates. This is what makes a mandatory automatic hook safe
to add.

## Non-goals for this contract

- No `before_implement`, `before_converge`, or `after_plan` hook is added.
- No `.github/workflows/**` change — convergence is not a CI gate (AC-005).
- No shipped skill file is edited; their sha256 values in
  `.specify/integrations/claude.manifest.json` must be unchanged after this work.
