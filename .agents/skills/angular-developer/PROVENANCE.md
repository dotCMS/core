# Provenance — `angular-developer`

This skill is **vendored from upstream Angular**. It is not authored by dotCMS.

| | |
| --- | --- |
| **Upstream** | https://github.com/angular/angular/tree/main/skills/dev-skills/angular-developer |
| **License** | MIT — `Copyright 2026 Google LLC` (see `SKILL.md` frontmatter) |
| **Last synced** | 2026-08-11 |
| **Synced from commit** | [`840f071`](https://github.com/angular/angular/commit/840f071566a0c5e44048051135da329e4e3ecaef) — _"docs: Adds HTTP communication guidance to Angular Skills"_ (2026-07-29) |
| **Files** | `SKILL.md` + 39 files under `references/` |

## Do not hand-edit this directory

Every file here except this one is a byte-for-byte copy of upstream. Editing them in place creates a
silent fork that nobody can distinguish from upstream later, and the next re-sync would revert the
change without warning.

**If upstream guidance is wrong for this repo, do not patch it here.** Put the dotCMS rule in the
governed overlay skill instead:

> [`.claude/skills/dot-ui-angular-standards/`](../../../.claude/skills/dot-ui-angular-standards/SKILL.md)

That skill declares that it **overrides** this one whenever the work is inside this repository. It
exists because upstream is written for generic Angular CLI projects and this workspace is an Nx
monorepo — upstream mandates `ng build` / `ng new` and documents `angular.json` and Karma, none of
which apply to `core-web/` (Nx + Jest 30, no `angular.json`).

## How to re-sync

```bash
UP="repos/angular/angular/contents/skills/dev-skills/angular-developer"
DEST=".agents/skills/angular-developer"

gh api "$UP/SKILL.md" --jq '.content' | base64 -d > "$DEST/SKILL.md"
gh api "$UP/references" --jq '.[] | select(.type=="file") | .name' | while read -r f; do
  gh api "$UP/references/$f" --jq '.content' | base64 -d > "$DEST/references/$f"
done
```

Then update the **Last synced** and **Synced from commit** rows above:

```bash
gh api "repos/angular/angular/commits?path=skills/dev-skills/angular-developer&per_page=1" \
  --jq '.[0] | "\(.sha)  \(.commit.author.date)"'
```

After re-syncing, re-check that the overlay skill still covers whatever upstream got wrong for this
repo — new upstream content may introduce new `ng`-CLI or non-Nx assumptions that need overriding.

## Sync history

- **2026-08-11** — synced to `840f071`. Brought in 4 new references (`http-client.md`, `pipes.md`,
  `migrations.md`, `environment-configuration.md`) and fixed two upstream contradictions that had been
  flagged in [#37009](https://github.com/dotCMS/core/issues/37009): `references/signal-forms.md`
  previously set `standalone: true` and an explicit `changeDetection: ChangeDetectionStrategy.OnPush`
  in its example component. Both are gone upstream. Prior vendored state was undated and had no
  recorded provenance — this file was added in the same change to make future drift detectable.
