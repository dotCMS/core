# sbom-merge

Merges the frontend npm inventory (`pnpm sbom` over `core-web`) into the Syft image-scan SBOM, so
one CycloneDX document covers every ecosystem in a dotCMS release.

It exists because the image scan cannot see the frontend: `core-web`'s WAR ships bundled, minified
JavaScript with no package manifests, so ~2,000 npm packages were invisible to the published SBOM.
Measured on a real release artifact before this change: 726 components, of which **7** were npm —
and only 2 of those came from `core-web` at all. See [issue #37575](https://github.com/dotCMS/core/issues/37575)
and the [feature spec](../../../../specs/37575-core-web-npm-sbom/spec.md).

## Usage

```bash
uv run sbom-merge --image <syft.json> --frontend <pnpm.json> --output <merged.json>
```

| Exit | Meaning |
|---:|---|
| 0 | Merged (or degraded — see below). Output written |
| 2 | An input is missing, malformed, or not CycloneDX 1.6 |
| 3 | The merged document failed schema validation; nothing was written |
| 4 | A component present in an input is missing from the output |

Prints `::frontend-covered::true|false` so the calling workflow can branch without parsing JSON.

**Degraded mode**: if the frontend document is absent or empty, the image inventory is published
unchanged and the document records `dotcms:sbom:frontend-covered=false`. A release must not lose
its Java/OS inventory because the frontend half broke — but it must not look complete either.

## The rules, and why they are not obvious

Full specification: [data-model.md](../../../../specs/37575-core-web-npm-sbom/data-model.md#reconciliation-rules).
The three that cost real debugging:

**Never deduplicate by name.** dotCMS ships `tinymce` three times: `6.8.3` from `core-web`'s tree,
`6.8.6` copied into `dotAdmin/`, and `7.2.1` vendored under the legacy webapp for the Dojo/JSP
editor. They are different editors on different majors. A name-keyed merge collapses them and
misreports the CVE surface for both. `core-web`'s own tree also carries `tslib` at three versions
simultaneously, so this is not only a cross-source problem.

**Reconcile between sources, never within one.** Syft legitimately emits the same purl twice when
it finds the same jar at two paths in the image — there are 16 such pairs in a real release. An
early version keyed the whole merge on identity and quietly took maven from **571 to 555**. The
trimmed test fixture had no duplicates, so every test passed; only the full document exposed it.
`tests/fixtures/syft-sample.json` now deliberately contains a real duplicate pair.

**The frontend inventory is additive, never authoritative.** Components vendored into the repository
(`ext/tinymcev7`, the Dojo tree) appear in no lockfile, so the image scan is the only source that
can see them. Treating the pnpm document as the authority on npm would erase them.

## A caveat worth knowing before you change the generation step

The inventory is generated with `pnpm fetch` followed by `pnpm sbom --prod`. Both halves matter:

- **`--lockfile-only` looks fine and is wrong twice over.** It yields zero licences, because licence
  text is read from the store. It also *over-reports*: 2,365 components versus 1,981 with a
  populated store, the difference being platform-specific optional binaries
  (`@esbuild/aix-ppc64`, `@emnapi/*`) that the lockfile lists for every platform and that never
  ship. `tests/test_licenses.py` guards against its reintroduction.
- **`--prod` is an approximation of "what ships", not a synonym.** `monaco-editor` is declared under
  `devDependencies` and ships anyway, because the Angular build copies it into `dotAdmin/`. It
  survives today only because it arrives with its manifest intact and the image scan catches it —
  a coincidence, not a design. `tests/test_shipped_assets.py` fails if a third copied asset appears.

## Tests

```bash
uv check --fix  # lint
uv format 
uv audit  # check for known security vulns in dependencies
uv run pytest
```

Fixtures are real captured data, never hand-written: a trimmed release artifact and real
`pnpm sbom` output. Both bugs described above were invisible to invented fixtures.
