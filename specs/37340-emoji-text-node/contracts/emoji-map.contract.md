# Contract: generated emoji shortcode map

## Artifact

```json
{ "copyright": "©", "registered": "®", "tm": "™", "rocket": "🚀", "…": "…" }
```

Flat `name` → character. ~1949 entries. Committed to the repo; **never hand-edited**.

## Generation

- **Source of truth**: the `emojis` export of `@tiptap/extension-emoji` — the same list the editor
  uses, so the map cannot describe a character the editor could not have produced.
- **When**: during the `core-web` Maven module build, via the `exec-maven-plugin` invocation
  `core-web` already runs. `frontend-maven-plugin` in `nodejs-parent` supplies node + pnpm.
- **Reaches Java how**: `core-web` builds before `dotCMS` in the reactor and `dotCMS/pom.xml:77`
  already depends on `dotcms-core-web`. The artifact ships inside that dependency and is read from
  the classpath. **No new cross-build mechanism** — see [research.md R1](../research.md).

## Integrity (AC-012)

CI regenerates the map and **fails on any diff** against the committed file — the same guarantee
pattern the repo applies to `openapi.yaml`, achieved here inside the same Maven reactor.

A unit test additionally asserts that the JS renderers and the Java helper resolve a sample of
names to the **same** character.

## Versioning

The map is **not** a published API. It is an internal artifact of the SDK packages and the core
WAR, and it carries no version of its own — the SDKs are versioned in date-lockstep with the CMS
release per [ADR-0019](https://github.com/dotCMS/platform-adrs/blob/main/decisions/0019-sdk-cms-date-lockstep-versioning.md).
