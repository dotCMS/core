# E2E Playwright serial describe blocks (#36570)

Research spike: classify `test.describe.configure({ mode: 'serial' })` in `core-web/apps/dotcms-ui-e2e` and enable intra-file parallelism when CI runs with `workers: 2` ([#36567](https://github.com/dotCMS/core/issues/36567)).

**Canonical suite:** [`core-web/apps/dotcms-ui-e2e/`](../../core-web/apps/dotcms-ui-e2e/) — see [README](../../core-web/apps/dotcms-ui-e2e/README.md) and [AGENTS.md](../../core-web/apps/dotcms-ui-e2e/AGENTS.md).

## Objective

Determine which serial blocks are **required for correctness** vs. **protect shared `let` / API load policy**, and document the safest refactors so Playwright can run more tests in parallel within a file.

## Boundaries

- **Always:** API setup/teardown; unique names (`testSuffix`, `uniqueSuffix()`); per-test content type when removing serial.
- **Ask first:** `workers` > 2; CI sharding ([#36568](https://github.com/dotCMS/core/issues/36568)); CI setup changes ([#36569](https://github.com/dotCMS/core/issues/36569)).
- **Never:** leave describe/module-level `let` without serial when tests run in parallel; duplicate relationship payloads outside `relationship.fixture.ts`.

## Configuration context

| Setting | Value |
|---------|--------|
| `fullyParallel` | `true` in [`playwright.config.ts`](../../core-web/apps/dotcms-ui-e2e/playwright.config.ts) |
| CI `workers` | `2` when `CI=true` (Maven sets this in `dotcms-ui-e2e/pom.xml`) |
| Serial blocks | **20** `configure({ mode: 'serial' })` calls in **15** spec files (~79 tests inside serial scopes) |

Serial blocks **still serialize tests within a file** even when CI uses 2 workers; removing unnecessary serial unlocks overlap between tests in the same spec.

## Inventory and classification

| File | Line | Describe / scope | Tests | Stated reason | Classification | Isolation strategy |
|------|------|------------------|-------|---------------|----------------|---------------------|
| `host-folder-field-default.spec.ts` | 11 | Default Host/Folder Selection | 4 | *(none)* | Refactorable → **done in spike** | Per-test CT + `deleteContentType` |
| `host-folder-field-prefill.spec.ts` | 11 | Folder Context Pre-fill | 4 | *(none)* | Refactorable → **done in spike** | Per-test CT + folders + delete |
| `host-folder-field-nested.spec.ts` | 11 | Nested Folder Pre-fill | 1 | *(none)* | Can merge/split → **serial removed** | Single test; no serial needed |
| `host-folder-field-file-asset.spec.ts` | 8 | File Asset pre-fill | 1 | *(none)* | Can merge/split → **serial removed** | Single test |
| `binary-field.spec.ts` | 43 | *(file root)* | 6 + 1 nested | *(none)* | Refactorable → **done in spike** | Per-test CT create/delete |
| `binary-field-system-options.spec.ts` | 49 | binary field systemOptions | 4 | *(none)* | Refactorable | Local `const ct` per test |
| `binary-field-image-editor.spec.ts` | 40, 75 | new / legacy editor | 1 each | *(none)* | Can merge/split → **serial removed** | Single test per describe |
| `file-field.spec.ts` | 43 | *(file root)* | 6 | *(none)* | Refactorable | Same as binary-field |
| `image-field.spec.ts` | 43 | *(file root)* | 5 | *(none)* | Refactorable | Same as binary-field |
| `relationship-field-select.spec.ts` | 19, 169, 292, 446 | Single/Multiple/Inline/Menu disabled | 5+5+4+4+1 | Shared `let` / API | Refactorable | Per-test locals or `test.extend`; see cardinality spec |
| `relationship-field-edit.spec.ts` | 17, 107 | Add More / Remove Relations | 3, 3 | API churn, not shared UI | Refactorable | Per-test setup; monitor backend |
| `relationship-field-advanced.spec.ts` | 75, 197 | Multiple fields / showFields | 2, 2 | API churn | Refactorable | Same |
| `relationship-field-table.spec.ts` | 90 | Search and Filter | 3 | API churn | Refactorable | Same |
| `relationship-field-chips.spec.ts` | 19 | Status & Locale Chips | 1 | Shared `let` (#36155) | Can merge/split → **serial removed** | Single test |
| `workflow-actions-dialog.spec.ts` | 57 | Workflow action wizard | 7 | *(none)* | **Required serial** | Shared `beforeAll` contentlet inode + workflow step; split per nested describe or keep serial |

**Reference (already parallel):** `relationship-field-cardinality.spec.ts` — uses `testSuffix` per test, no serial.

## Experiment (spike subset)

Three specs refactored to remove serial and use per-test isolation:

1. `host-folder-field-default.spec.ts`
2. `host-folder-field-prefill.spec.ts`
3. `binary-field.spec.ts`

### Commands

From `core-web/` with dotCMS on `:8080`:

```bash
CURRENT_ENV=dev HEADLESS=true pnpm nx e2e dotcms-ui-e2e --configuration=dev -- \
  --workers=2 \
  src/tests/edit-content/fields/host-folder-field/host-folder-field-default.spec.ts \
  src/tests/edit-content/fields/host-folder-field/host-folder-field-prefill.spec.ts \
  src/tests/edit-content/fields/file-upload-fields/binary-field/binary-field.spec.ts
```

### Results

| Run | Workers | Serial removed? | Pass | Duration | Notes |
|-----|---------|-----------------|------|----------|-------|
| A | 1 | Yes (post-refactor subset) | 16/16 | **53.3s** | `CURRENT_ENV=ci`, local dotCMS `:8080`, 2026-07-22 |
| B | 2 | Yes (post-refactor subset) | 16/16 | **27.9s** | Same subset; **~48% faster** vs Run A |

Pre-refactor baseline with `mode: 'serial'` on these files would not overlap tests within each file under `workers: 2`, so Run B demonstrates the intended gain after isolation refactors.

### Estimated suite impact (range)

With `workers: 2` and one dotCMS backend:

- **Already merged (#36567):** file-level parallelism across specs.
- **After P0 refactors (file-upload + host-folder):** most non-relationship serial tests can overlap within files — rough order **5–15%** additional Playwright phase savings vs. workers=2 alone, depending on spec duration distribution.
- **After P1 (relationship select/edit):** largest remaining serial surface (~38 tests); potential **10–25%** additional savings if stable under API load.
- **workflow-actions-dialog:** keep serial or split into 3 files with separate `beforeAll` contentlets (P2).

## Prioritized follow-up work

| Priority | Scope | Effort | Impact |
|----------|--------|--------|--------|
| P0 | `file-field.spec.ts`, `image-field.spec.ts`, `binary-field-system-options.spec.ts` | ~2–4 h | Same pattern as `binary-field.spec.ts` |
| P0 | Remaining host-folder (if any serial left) | ~1 h | Low count |
| P1 | `relationship-field-select.spec.ts` | ~4–6 h | ~18 tests in serial loops |
| P1 | `relationship-field-edit`, `advanced`, `table` | ~3–4 h | API churn; watch flakiness |
| P2 | `workflow-actions-dialog.spec.ts` | ~2–3 h | True shared inode or split describes |

If Playwright phase remains **> 30 min** after P0+P1, consider sharding ([#36568](https://github.com/dotCMS/core/issues/36568)).

### Follow-up tasks (opened from spike)

- [#36683](https://github.com/dotCMS/core/issues/36683) — P0 file-upload specs
- [#36684](https://github.com/dotCMS/core/issues/36684) — P1 relationship-field specs

## Related issues

- [#36570](https://github.com/dotCMS/core/issues/36570) — this spike
- [#36567](https://github.com/dotCMS/core/issues/36567) — CI workers = 2 (closed)
- [#36568](https://github.com/dotCMS/core/issues/36568) — shard matrix
- [#36569](https://github.com/dotCMS/core/issues/36569) — CI setup overhead
