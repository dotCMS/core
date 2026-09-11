# Contract: Diff allowlist enforcement check

**Requirements**: FR-001, FR-001a, FR-002, FR-002a · **Consumer**: CI (required check) + developers

This is the contract that replaces manual QA. It is the reason a reviewer can approve a ~1,000-file
diff without auditing it, so its precision matters more than any other artifact in this feature.

## Interface

```
core-web/tools/verify-test-only-diff.mjs [--base <ref>] [--json]

exit 0  every changed path matched an allowed category
exit 1  at least one changed path matched none, or matched a denied category
exit 2  invocation error (no merge base, not a git repo)
```

**Input**: `git diff --name-only <merge-base>..HEAD`. The base defaults to the merge base with the
PR target branch.

**Output**: for each changed path, the category that admitted it; for each rejection, the path and
why. `--json` emits the same as a machine-readable object for the count-parity report to embed.

## Categories

Allowed — a path must match **exactly one**:

| Category | Patterns |
|---|---|
| `spec` | `**/*.spec.ts`, `**/*.spec.tsx`, `**/*.test.ts` |
| `test-support` | `**/test-setup.ts`, `**/src/test.ts` |
| `test-config` | `**/jest.config.*`, `**/vite.config.*`, `**/vitest.config.*`, `**/vitest-base.config.*`, `**/tsconfig.spec.json`, `**/karma.conf.js`, `jest.preset.js` |
| `workspace-config` | `nx.json`, `package.json`, `pnpm-lock.yaml`, `**/project.json` |
| `build-invocation` | `core-web/pom.xml` |
| `migration-tooling` | `core-web/tools/**` |
| `docs` | `**/*.md`, `.cursor/rules/*.mdc` |

Denied — **checked before** the allow list, because these paths would otherwise match `spec` or
`test-config`:

| Category | Patterns | Why |
|---|---|---|
| `e2e` | `apps/dotcms-ui-e2e/**`, `**/playwright.config.*` | FR-002a. E2E specs are also `*.spec.ts`, so an allow-only list would admit them. |
| `stencil` | `libs/dotcms-webcomponents/**` | Out of scope; its config would match `test-config`. |

Everything else — every product source path — is rejected.

## Behavioral requirements

1. **Deny beats allow.** A path matching both a denied and an allowed category is rejected. Ordering
   the check the other way silently readmits every E2E spec.
2. **Unmatched is rejected, never ignored.** A new path shape that matches nothing fails the build.
   The check must never default to permitting the unknown — that inverts its purpose.
3. **Deterministic and self-contained.** No network, no dependency beyond `git` and Node. It must run
   identically on a developer machine and in CI.
4. **The categories are the output.** Reporting *which* category admitted each file is what lets a
   reviewer confirm the shape of the diff at a glance instead of trusting a boolean.

## Red-gate proof (required before any project migrates)

| Scenario | Expected |
|---|---|
| Edit any product source file | **exit 1**, path named |
| Edit `apps/dotcms-ui-e2e/src/tests/login/*.spec.ts` | **exit 1** — proves deny-beats-allow |
| Edit `libs/dotcms-webcomponents/stencil.config.ts` | **exit 1** |
| Add `core-web/tools/codemod-jest-to-vitest.mjs` | **exit 0**, category `migration-tooling` — proves the check does not reject its own PR |
| Edit `core-web/pom.xml` | **exit 0**, category `build-invocation` |
| A realistic migration commit | **exit 0** |
| Revert each failing scenario | **exit 0** |

## Known limitations, stated rather than hidden

- **`package.json` and `project.json` are allowed wholesale.** The check verifies *which files*
  changed, not what changed inside them. A product-affecting edit to `package.json` (a runtime
  dependency bump, say) would pass. Narrowing this to a key-level diff is possible but was not
  required; reviewers should read those specific files, which is a handful of files rather than a
  thousand.
- **`core-web/pom.xml` is allowed wholesale**, with the same caveat — it is a build file, and FR-009
  constrains the edit to removing two Jest-only flags.
- The check proves *no product file changed*. It does not prove the tests are still meaningful —
  that is FR-003a's job, and the two together are what the reviewer relies on.
