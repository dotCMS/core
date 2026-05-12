# Cleanup: `FEATURE_FLAG_NEW_BLOCK_EDITOR`

This document is the runbook to remove all the rollback scaffolding for the new TipTap-v3 block editor once it has cleared QA. It assumes the new editor is the only one customers should ever use and that the legacy `libs/block-editor/` is going to be dropped from the codebase entirely.

A future agent (human or AI) can follow these steps top-to-bottom. Each phase has the exact files to touch and what to do in them. Run the verification commands at the bottom of every phase before moving on.

**Scaffolding was introduced by:** commit `5a1f2ace23` — `feat(block-editor): add NEW_BLOCK_EDITOR_FEATURE_FLAG rollback toggle` (the flag was later renamed to `FEATURE_FLAG_NEW_BLOCK_EDITOR` to follow the `FEATURE_FLAG_*` prefix convention; the original commit subject is preserved here so `git log` searches still hit it).

## Prerequisites

- [ ] New editor has been the default in production for long enough to confirm no customer regressions (e.g., one full release cycle).
- [ ] No customer instance has flipped `FEATURE_FLAG_NEW_BLOCK_EDITOR` to `true` and reported issues.
- [ ] PO/eng-lead approval to drop the legacy lib (this is one-way — the legacy editor is gone after this PR).
- [ ] You are on a fresh feature branch off `main`.

## Phase 1 — Strip the `@if` branches in the 3 Angular consumers

Each consumer wraps both editors in an `@if (isNewBlockEditorEnabled())` / `@else` block. Collapse each one to just the `<dot-block-editor>` branch and remove the flag plumbing in the component class.

### 1.1 — `dot-edit-content-block-editor`

**Template** (`core-web/libs/edit-content/src/lib/fields/dot-edit-content-block-editor/dot-edit-content-block-editor.component.html`)
- Delete the `@else { <dot-old-block-editor> ... }` block.
- Unwrap the `@if (isNewBlockEditorEnabled())` so only `<dot-block-editor>` remains.

**Component class** (`core-web/libs/edit-content/src/lib/fields/dot-edit-content-block-editor/dot-edit-content-block-editor.component.ts`)
- Remove `import { BlockEditorModule } from '@dotcms/block-editor';`
- Remove `import { DotPropertiesService } from '@dotcms/data-access';` (only this symbol — keep other data-access imports if used elsewhere)
- Remove `FeaturedFlags` from `@dotcms/dotcms-models` if no longer referenced
- Remove `import { toSignal } from '@angular/core/rxjs-interop';` if no longer used
- Remove `BlockEditorModule` from the component's `imports: [...]` array
- Remove the `dotPropertiesService` injection and the `isNewBlockEditorEnabled` signal field

**Spec** (`core-web/libs/edit-content/src/lib/fields/dot-edit-content-block-editor/dot-edit-content-block-editor.component.spec.ts`)
- Remove the `DotPropertiesService` mock and any `getFeatureFlag` setup
- Remove tests that exercise the legacy branch (anything querying `dot-old-block-editor`)

### 1.2 — `dot-block-editor-sidebar`

**Template** (`core-web/libs/portlets/edit-ema/portlet/src/lib/components/dot-block-editor-sidebar/dot-block-editor-sidebar.component.html`)
- Same pattern as 1.1 — collapse the `@if/@else` to just the new editor.

**Component class** (`core-web/libs/portlets/edit-ema/portlet/src/lib/components/dot-block-editor-sidebar/dot-block-editor-sidebar.component.ts`)
- Remove `BlockEditorModule` import + entry in `imports: []`
- Remove `DotPropertiesService` injection and the `isNewBlockEditorEnabled` signal
- Remove `FeaturedFlags` and `toSignal` imports if no longer referenced

**Spec** (`core-web/libs/portlets/edit-ema/portlet/src/lib/components/dot-block-editor-sidebar/dot-block-editor-sidebar.component.spec.ts`)
- Remove the `DotPropertiesService` provider/mock
- Drop legacy-branch tests

**Styles** (`core-web/libs/portlets/edit-ema/portlet/src/lib/components/dot-block-editor-sidebar/dot-block-editor-sidebar.component.scss`)
- If any selectors target `dot-old-block-editor`, remove them.

### 1.3 — `dot-content-compare-block-editor`

**Template** (`core-web/libs/portlets/edit-ema/ui/src/lib/dot-content-compare/components/dot-content-compare-block-editor/dot-content-compare-block-editor.component.html`)
- Collapse the `@if/@else` to the new-editor branch.

**Component class** (`core-web/libs/portlets/edit-ema/ui/src/lib/dot-content-compare/components/dot-content-compare-block-editor/dot-content-compare-block-editor.component.ts`)
- Remove `BlockEditorModule`, `DotBlockEditorComponent`, `DotPropertiesService`, and `FeaturedFlags` imports
- Remove the `AnyBlockEditor` type alias
- Remove `getEditorInstance()` helper — call `this.blockEditor.editor()` directly (the new editor exposes `editor` as a signal getter)
- Update the `@ViewChild` types from `AnyBlockEditor` back to `DotCMSEditorComponent`
- Remove the `dotPropertiesService` injection and `isNewBlockEditorEnabled` signal
- Remove `toSignal` import if no longer used
- Remove `Editor` import from `@tiptap/core` if it was only used by the `AnyBlockEditor` union

**Spec** (`core-web/libs/portlets/edit-ema/ui/src/lib/dot-content-compare/components/dot-content-compare-block-editor/dot-content-compare-block-editor.component.spec.ts`)
- Drop the `DotPropertiesService` mock
- Drop legacy-branch assertions

### 1.4 — Edit-content field tests (incidental consumer)

**File:** `core-web/libs/edit-content/src/lib/components/dot-edit-content-field/dot-edit-content-field.component.spec.ts`

This spec imports from `@dotcms/new-block-editor`. After the rename in Phase 5, the import path will change automatically via tsconfig. No manual edit required *here*, but verify after Phase 5.

### Phase 1 verification

```bash
yarn nx run edit-content:lint
yarn nx run portlets-edit-ema-portlet:lint
yarn nx run portlets-edit-ema-ui:lint
grep -rn "isNewBlockEditorEnabled\|dot-old-block-editor" libs/ apps/ dotCMS/  # should be empty
```

## Phase 2 — Restore `gridBlock` augmentation key in the new editor

The collision-avoidance rename is no longer needed once the legacy lib is gone.

**File:** `core-web/libs/new-block-editor/src/lib/editor/extensions/nodes/grid.extension.ts`

Find this block:

```ts
declare module '@tiptap/core' {
    interface Commands<ReturnType> {
        newBlockEditorGridBlock: {
            insertGrid: () => ReturnType;
            setGridColumns: (columns: number[]) => ReturnType;
        };
    }
}
```

Restore the original augmentation key (matches the runtime node name):

```ts
declare module '@tiptap/core' {
    interface Commands<ReturnType> {
        gridBlock: {
            insertGrid: () => ReturnType;
            setGridColumns: (columns: number[]) => ReturnType;
        };
    }
}
```

Also drop the comment block above the declaration that explains the namespace was for collision avoidance.

## Phase 3 — Strip the legacy web-component registration

**File:** `core-web/apps/dotcms-block-editor/src/main.ts`

- Remove `import { DotBlockEditorComponent } from '@dotcms/block-editor';`
- Remove the entire `if (!customElements.get('dotcms-old-block-editor')) { ... }` block (registers the legacy custom element)
- Keep the `dotcms-block-editor` registration (the new editor)

**File:** `core-web/apps/dotcms-block-editor/src/app/app.config.ts`

- Remove `import { BlockEditorModule } from '@dotcms/block-editor';`
- Remove `import { importProvidersFrom } from '@angular/core';` if not used elsewhere in the file
- Remove the `importProvidersFrom(BlockEditorModule)` entry from `providers: [...]`
- Update the JSDoc comment block — drop the paragraph explaining `BlockEditorModule` is needed for the legacy custom element

### Phase 3 verification

```bash
yarn nx run dotcms-block-editor:build:production
grep -rn "dotcms-old-block-editor\|DotBlockEditorComponent" apps/ libs/  # should be empty (after Phase 4)
```

## Phase 4 — Delete the legacy lib

Now safe to delete — nothing references it anymore.

```bash
rm -rf core-web/libs/block-editor
```

**Path mapping** (`core-web/tsconfig.base.json`)

Delete this line:

```json
"@dotcms/block-editor": ["libs/block-editor/src/public-api.ts"],
```

(It will be re-introduced pointing at the renamed new-block-editor in Phase 5.)

**Nx workspace bookkeeping**
- If `nx.json`, `workspace.json`, or any other root config references `block-editor` as a project, remove those entries (or let `nx graph` flag stale references after Phase 5).

### Phase 4 verification

```bash
ls core-web/libs/block-editor 2>&1 | grep "No such" && echo "deleted"
yarn nx graph --file=/tmp/graph.json 2>&1 | grep -i error  # nx should not error
```

## Phase 5 — Rename `libs/new-block-editor` → `libs/block-editor`

Do this with the Nx generator so all path mappings, project.json, and jest.config.ts updates happen atomically:

```bash
yarn nx g @nx/workspace:move --project new-block-editor --destination block-editor --no-interactive
```

This moves the directory and rewrites:
- `core-web/tsconfig.base.json` — adds `"@dotcms/block-editor": ["libs/block-editor/src/index.ts"]` (and removes `@dotcms/new-block-editor` if the alias matches the project name)
- `libs/block-editor/project.json` — `name: "block-editor"`
- `libs/block-editor/jest.config.ts` — `displayName: "block-editor"`
- All consumer `import` paths from `@dotcms/new-block-editor` → `@dotcms/block-editor`

### Manual fix-ups after the generator

The generator usually handles imports correctly, but verify:

```bash
grep -rn "@dotcms/new-block-editor" core-web/ dotCMS/  # should be empty
grep -rn "libs/new-block-editor" core-web/  # should be empty (project.json, nx.json, etc.)
```

Files to **manually inspect and update if the generator missed them**:
- `core-web/apps/dotcms-block-editor/src/main.ts`
- `core-web/apps/dotcms-block-editor/src/app/app.config.ts`
- `core-web/libs/edit-content/src/lib/fields/dot-edit-content-block-editor/dot-edit-content-block-editor.component.ts`
- `core-web/libs/edit-content/src/lib/components/dot-edit-content-field/dot-edit-content-field.component.spec.ts`
- `core-web/libs/portlets/edit-ema/portlet/src/lib/components/dot-block-editor-sidebar/dot-block-editor-sidebar.component.ts`
- `core-web/libs/portlets/edit-ema/portlet/src/lib/components/dot-block-editor-sidebar/dot-block-editor-sidebar.component.spec.ts`
- `core-web/libs/portlets/edit-ema/ui/src/lib/dot-content-compare/components/dot-content-compare-block-editor/dot-content-compare-block-editor.component.ts`

### Path-mapping cleanup

If the generator left both aliases pointing at the same lib, drop the `@dotcms/new-block-editor` alias. Final `core-web/tsconfig.base.json` should have **only one** entry:

```json
"@dotcms/block-editor": ["libs/block-editor/src/index.ts"],
```

If the lib has a `lib/*` deep-import alias, keep it under the new name:

```json
"@dotcms/block-editor/*": ["libs/block-editor/src/lib/*"]
```

### Phase 5 verification

```bash
yarn nx run block-editor:lint
yarn nx run block-editor:test
yarn nx run dotcms-block-editor:build:production
grep -rn "@dotcms/new-block-editor\|new-block-editor" core-web/  # should be empty
```

## Phase 6 — Remove the backend feature flag

### 6.1 — Java enum

**File:** `dotCMS/src/main/java/com/dotcms/featureflag/FeatureFlagName.java`

Remove the constant:

```java
String FEATURE_FLAG_NEW_BLOCK_EDITOR = "FEATURE_FLAG_NEW_BLOCK_EDITOR";
```

### 6.2 — Configuration whitelist

**File:** `dotCMS/src/main/java/com/dotcms/rest/api/v1/system/ConfigurationResource.java`

Remove `FeatureFlagName.FEATURE_FLAG_NEW_BLOCK_EDITOR` from the whitelist array (around the `getConfigurationProperties` method).

### 6.3 — Default property

**File:** `dotCMS/src/main/resources/dotmarketing-config.properties`

Delete:

```properties
## New TipTap-v3 Block Editor (rollback safety: legacy editor renders by default)
FEATURE_FLAG_NEW_BLOCK_EDITOR=false
```

### 6.4 — JSP guard

**File:** `dotCMS/src/main/webapp/html/portlet/ext/contentlet/field/edit_field.jsp`

Find the section that conditionally chooses the custom-element tag (around the block-editor field rendering). Replace the `ConfigUtils.isFeatureFlagOn(...)` ternary with a hardcoded `<dotcms-block-editor>` tag. Drop the `blockEditorTag` String variable entirely.

Before:

```jsp
String blockEditorTag = ConfigUtils.isFeatureFlagOn("FEATURE_FLAG_NEW_BLOCK_EDITOR")
        ? "dotcms-block-editor"
        : "dotcms-old-block-editor";
...
const blockEditor = document.createElement('<%=blockEditorTag%>');
```

After:

```jsp
const blockEditor = document.createElement('dotcms-block-editor');
```

### 6.5 — Frontend enum

**File:** `core-web/libs/dotcms-models/src/lib/shared-models.ts`

Remove the `FEATURE_FLAG_NEW_BLOCK_EDITOR` member from the `FeaturedFlags` enum.

### Phase 6 verification

```bash
grep -rn "FEATURE_FLAG_NEW_BLOCK_EDITOR" core-web/ dotCMS/  # should be empty
./mvnw compile -pl :dotcms-core --am  # backend compiles
yarn nx run dotcms-models:lint
```

## Phase 7 — Drop tippy.js dependency

After Phase 4 deleted the legacy lib, no code in this repo imports tippy.js anymore. The new editor uses floating-ui via ngx-tiptap. Confirm and remove.

### 7.1 — Sanity grep

```bash
grep -rn "from 'tippy.js'\|from \"tippy.js\"\|require('tippy.js')" core-web/  # must be empty
```

If anything shows up, fix that file before proceeding (the new editor must not depend on tippy).

### 7.2 — Remove from package.json

**File:** `core-web/package.json`

Delete the `tippy.js` line from `dependencies`:

```json
"tippy.js": "^6.3.7",
```

**File:** `core-web/yarn.lock`

Run `yarn install` to regenerate the lockfile without tippy.

```bash
yarn install
```

### Phase 7 verification

```bash
grep -n "tippy" core-web/package.json  # empty
grep -n "tippy.js@" core-web/yarn.lock  # empty
yarn nx run dotcms-block-editor:build:production
```

## Phase 8 — Restore production bundle budget

**File:** `core-web/apps/dotcms-block-editor/project.json`

The budget was bumped to `4mb`/`3.5mb` to fit both editors in one bundle. After Phase 4, the new editor alone should fit comfortably under the original budget. Restore:

```json
"budgets": [
    {
        "type": "initial",
        "maximumWarning": "2.5mb",
        "maximumError": "3mb"
    },
    ...
]
```

**Important:** run a production build first to confirm the new-editor-only bundle is actually under 3 MB. If it's not, set the budget to whatever fits with a small headroom — don't ratchet down to 3 MB if reality is 3.2 MB.

```bash
yarn nx run dotcms-block-editor:build:production --skip-nx-cache 2>&1 | grep "Initial total"
```

## Phase 9 — Remove this document and the related TODO entries

**File:** `core-web/libs/block-editor/TODO.md` (was `core-web/libs/new-block-editor/TODO.md` before Phase 5)

Remove:
- The `## Cleanup (post-flag-removal)` section (its job is done)
- The completed `[x] Add a feature flag to let users switch...` checkbox under Features

**This file** (`core-web/libs/block-editor/CLEANUP_FEATURE_FLAG.md`)

Delete it. The cleanup is done; there's nothing left to roll back.

```bash
rm core-web/libs/block-editor/CLEANUP_FEATURE_FLAG.md
```

## Phase 10 — Final verification

```bash
# No references to the flag anywhere
grep -rn "FEATURE_FLAG_NEW_BLOCK_EDITOR\|isNewBlockEditorEnabled\|dot-old-block-editor\|dotcms-old-block-editor" core-web/ dotCMS/

# No references to the old lib alias
grep -rn "@dotcms/new-block-editor" core-web/

# No tippy
grep -rn "tippy.js\|from 'tippy" core-web/

# Builds + lints + tests
yarn nx run block-editor:lint
yarn nx run block-editor:test
yarn nx run dotcms-block-editor:build:production
yarn nx run edit-content:lint
yarn nx run portlets-edit-ema-portlet:lint
yarn nx run portlets-edit-ema-ui:lint
yarn nx run data-access:test --testPathPattern=dot-properties
./mvnw compile -pl :dotcms-core --am
```

All four greps should return zero matches. All builds/lints/tests should pass.

## Things to KEEP — do not remove

These were introduced alongside the flag but are general improvements that benefit the whole codebase:

- **`shareReplay(1)` cache in `DotPropertiesService.getFeatureFlag`** (`core-web/libs/data-access/src/lib/dot-properties/dot-properties.service.ts`). It deduplicates HTTP calls for any feature flag, not just this one. Leave it in place.
- **The TipTap v3 typecheck patches in the new editor** (e.g., the `getEditorElement` helper, the `setContent` options object, the storage type augmentation pattern). These match the actual v3 API and aren't flag-specific.

## Rollback during cleanup

If anything breaks during this cleanup and you need to abort partway through:

```bash
git checkout main -- core-web/ dotCMS/
```

…or revert the cleanup commit you started building. The original feature-flag commit (`5a1f2ace23`) is still on `main`, so the legacy editor + flag remain a working safety net until this cleanup PR merges.
