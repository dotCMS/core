# Contract: `sbom-merge` CLI and composite action

**Feature**: [spec.md](../spec.md) · **Plan**: [plan.md](../plan.md) · **Date**: 2026-09-21

Two interfaces are exposed. Both are internal to this repository's CI — neither is a public API —
but both are contracts in the sense that matters: something else calls them, and changing their
shape breaks that caller.

The **published SBOM** is the one genuinely external contract in this work; it is specified last.

---

## 1. CLI: `sbom-merge`

A pure transformation. Reads two CycloneDX 1.6 documents, writes one.

### Invocation

```
sbom-merge --image <path> --frontend <path> --output <path> [--fail-on-missing-frontend]
```

| Argument | Required | Meaning |
|---|---|---|
| `--image` | yes | CycloneDX JSON from the Syft image scan |
| `--frontend` | yes | CycloneDX JSON from `pnpm sbom` over `core-web` |
| `--output` | yes | Destination for the merged document |
| `--fail-on-missing-frontend` | no | Exit non-zero if `--frontend` is absent or empty, instead of degrading (see below) |

### Behavior

Applies reconciliation rules 1–8 from [data-model.md](../data-model.md#reconciliation-rules).
The transformation is deterministic: the same two inputs always produce a byte-identical output, so
the tool is safe to re-run and its output is diffable between releases.

### Exit codes

| Code | Condition | Caller's expected response |
|---|---|---|
| `0` | Merge succeeded; output written and schema-valid | Publish the output |
| `2` | An input file is missing or is not valid CycloneDX 1.6 | Fail the step (FR-009) |
| `3` | Merge produced a document that fails schema validation | Fail the step; never publish (SC-005) |
| `4` | The invariant was violated — a component present in an input is missing from the output | Fail the step (SC-009) |

Exit codes are distinct on purpose: FR-009 requires a frontend-inventory failure to be
*distinguishable*, which a single generic `1` would not satisfy.

### Degraded mode

When `--frontend` is absent and `--fail-on-missing-frontend` is **not** set, the tool copies the
image document through unchanged and records in `metadata` that frontend coverage is absent.

This exists for FR-009 and the spec's first edge case: a release from a branch without this change,
or a frontend step that failed, must still publish the Java/OS inventory rather than publishing
nothing — but the result must **not** be indistinguishable from a complete document. A reader must
be able to tell, from the artifact alone and without reading build logs (SC-007).

---

## 2. Invocation from the release action

**There is no composite `action.yml` wrapper.** The established pattern in this repository —
`changelog-publisher`, invoked by `cicd_comp_changelog-site-publish-phase.yml` — is to set up uv
once and call the tool directly with `working-directory`:

```yaml
- name: Setup uv
  uses: astral-sh/setup-uv@v5

- name: Merge SBOMs
  working-directory: .github/actions/core-cicd/sbom-merge
  run: |
    uv run sbom-merge \
      --image "$IMAGE_SBOM" \
      --frontend "$FRONTEND_SBOM" \
      --output "$MERGED_SBOM"
```

Verified against `.github/actions/core-cicd/changelog-publisher/`, which ships `pyproject.toml`,
`src/`, `tests/` and `uv.lock` but **no** `action.yml`. Adding a wrapper here would introduce a
pattern the repository does not use, for no benefit.

The caller learns whether frontend coverage is present from the tool's stdout marker
(`::frontend-covered::true|false`), mirroring how `changelog-publisher` signals a skip with
`::changelog-skip::`. This keeps the degraded-release branch out of JSON parsing in bash.

---

## 3. Upstream contract: what `sbom-generator` must produce

The existing action gains steps before the merge. The contract they must satisfy:

| Step | Requirement | Source |
|---|---|---|
| Set up pnpm | `pnpm/setup` with **no `version` input** — it resolves from `core-web/package.json`. Hardcoding a version is a contract violation here, not a style preference | [R3](../research.md#r3--how-should-the-pnpm-version-be-pinned) |
| Populate the store | `pnpm fetch` — sufficient for license resolution; a full `pnpm install` is not required | [R2](../research.md#r2--where-in-the-pipeline-can-the-inventory-be-generated) |
| Generate | `pnpm sbom --sbom-format cyclonedx --sbom-spec-version 1.6 --prod` | [R7](../research.md#r7--which-cyclonedx-specification-version) |

`--sbom-spec-version 1.6` is not optional: pnpm 12 defaults to 1.7 and the merge tool rejects a
mismatched input with exit code `2`.

---

## 4. External contract: the published SBOM

The only interface outside this repository. Downstream consumers — scanners, customer compliance
pipelines, internal dashboards — parse it. **Every property below is held fixed by this work**
(FR-006, FR-007, SC-005, SC-008):

| Property | Value | Why fixed |
|---|---|---|
| Format | CycloneDX JSON | US3: consumers must not learn a second shape |
| Specification version | `1.6` | What is published today; moving to 1.7 is a separate, consumer-visible decision |
| Artifact name | `sbom-dotcms-<version>` | Existing discoverability; ADR-0019's conventions |
| Artifact count | exactly one per release | FR-012, SC-008 |
| Location | GitHub release asset + workflow artifact, as today | FR-007 |

**The only intended change is additive**: npm components appear where previously there were almost
none. Existing maven, deb and generic components are unchanged in count and content (FR-010,
SC-006) — which is asserted by a test, not merely intended.
