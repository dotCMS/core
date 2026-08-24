# Contributing Skills

How to add and maintain a dotCMS skill. The rules here are **enforced by tooling**, not by memory — you don't have to remember them, but understanding them makes the happy path fast.

- **Fast path:** `just new-skill` → fill in `SKILL.md` → open a PR.
- **Catalog:** [CATALOG.md](CATALOG.md) — the browsable inventory. Auto-generated; never hand-edit it.
- **Enforcement:** `just skills-lint` locally (optional, run it before you push — see §7), and the `Skill Lint` CI check that runs automatically and gates every PR touching `.claude/skills/**`.

---

## 1. Naming — `dot-<domain>-<action>[-target]`

All first-party skills start with **`dot-`**. This isn't for de-duplication — it's for **provenance**: at invocation time the skill list is flat (your skills, plugin skills, and global skills all appear together with no folder context), and `dot-` instantly identifies what's ours. It matches the `dot-` prefix already used for dotCMS Angular component selectors.

Format: `dot-<domain>-<action>` with an optional `-<target>`. The **`<domain>`** must be one of the approved prefixes (single source of truth: [`skills.config.json`](skills.config.json)):

| Domain | For skills about… |
| --- | --- |
| `dot-issue-` | GitHub issues — triage, validation, creation |
| `dot-pr-` | Pull requests — review, checks |
| `dot-release-` | Releases, LTS, rollback, backports |
| `dot-cicd-` | CI/CD, build, merge queue, workflows |
| `dot-content-` | Content, contentlets, indexing |
| `dot-ui-` | Frontend / editor / UI workflows |

Examples: `dot-issue-manage`, `dot-release-rollback`, `dot-cicd-diagnose`.

Need a domain that isn't listed? Add it to `approvedDomains` in `skills.config.json` **in the same PR** (that edit is the review point for "is this a real new domain?").

> The **directory name must equal the `name` frontmatter field.** `skill-lint` enforces this.

---

## 2. Frontmatter

```yaml
---
name: dot-release-verify-notes          # required — must equal the directory name
description: >                           # required — see §5, this drives triggering
  Verify release notes are complete. Use when preparing a dotCMS release
  or when the user mentions release notes, changelog, or version bump.
owner: "@dotcms/platform"                # required — who fixes it / answers questions
status: experimental                     # required — experimental | active | superseded | deprecated
related: [dot-release-rollback]          # optional — adjacent skills (see §4)
supersedes: dot-release-old-notes        # optional — replacement link (see §4)
---
```

### These are dotCMS conventions, **not** Claude fields

Claude's runtime only reads **`name`** and **`description`** (and, in some clients, an optional `compatibility` / `allowed-tools`). Everything else here — **`owner`, `status`, `supersedes`, `related`, `superseded-by`** — is **inert as far as Claude is concerned.** It carries meaning *only* because our tooling gives it meaning:

- the **catalog generator** renders it,
- **`skill-lint`** validates it,
- the **scaffolder** seeds it.

Implication: don't assume Claude enforces these, and **don't reuse the reserved names** (`name`, `description`, `compatibility`, `allowed-tools`) for custom purposes.

---

## 3. Status and its transitions

`status` makes a skill's authority explicit instead of assumed:

| Status | Meaning |
| --- | --- |
| **`experimental`** | Published and usable, but **not yet the canonical one for its job.** May change or be replaced. New skills start here. |
| **`active`** | Vetted and canonical. The one to reach for — and the one to *extend* rather than fork. |
| **`superseded`** | Retired **because a specific better skill replaced it** (carries `superseded-by:`). Kept for reference/back-compat only; not for new work. |
| **`deprecated`** | Retired **with no replacement** — obsolete or no longer needed, and slated for removal. Nothing supersedes it; there's simply nothing to use instead. |

> `superseded` vs `deprecated`: both mean "don't use for new work," but `superseded` points you to a replacement and `deprecated` doesn't. Pick `deprecated` when the capability is going away entirely.

**Lifecycle: `experimental → active → superseded` *or* `deprecated`**

- **→ experimental** — set automatically by `just new-skill`. You never choose it.
- **experimental → active** — a deliberate promotion by the `owner`, via a PR that flips the field, once the skill has proven out. *(You may open the **first** PR already `active` if you're confident it works and it's been exercised; otherwise leave it `experimental` and promote later.)*
- **→ superseded** — set on the **old** skill by the author of the **replacing** skill, in the same PR (see §4).
- **→ deprecated** — set by the `owner` when the skill is being retired with no successor. No `superseded-by:` (there's nothing to point at).

---

## 4. `supersedes` / `related` — linking skills

These fields make overlap visible in the catalog instead of tribal knowledge.

- **`supersedes: <name>`** — *directional, replacement.* "This skill takes over from that one." The old skill must then get `status: superseded` **and** a reverse `superseded-by: <this skill>`. `skill-lint` **fails** if the two sides disagree — so retirement can't be left half-done.
- **`related: [<name>, …]`** — *non-directional, adjacency.* "These are complementary, not replacements." Helps authors find the neighborhood and readers pick the right sibling.

**Worked example** — replacing `dot-content-index` with `dot-content-reindex`:

```yaml
# dot-content-reindex/SKILL.md
supersedes: dot-content-index
```
```yaml
# dot-content-index/SKILL.md
status: superseded
superseded-by: dot-content-reindex
```

---

## 5. The description *is* the trigger contract

`description` is the single field that determines whether Claude invokes your skill. Spend your effort here. State **what it does AND when to use it**, and be specific and slightly "pushy" about trigger contexts (Claude tends to under-trigger). List the phrases, file types, and situations that should fire it. All "when to use" information goes in the description, not the body.

---

## 6. Contribution workflow

Deliberately lightweight: four stages, minimal gates.

### Propose *(required for a new domain prefix; optional for a skill in an existing domain)*
- Open a Skill Proposal issue: name (`dot-<domain>-<action>`), domain, the problem, and why existing skills don't cover it.
- Check [CATALOG.md](CATALOG.md) first; link any near-matches and say why you're not extending them. (`just new-skill` also greps for near-matches at creation time.)
- Outcome: a maintainer 👍, or "extend X instead" — *before* you write code.

### Review
- Open a PR with the skill. `CATALOG.md` **regenerates automatically** (`skill-lint` fails if it's stale) — no manual catalog edits.
- **One approval.** Reviewer checks: `dot-` naming + approved domain, no duplication, valid frontmatter, description-as-trigger, and that it actually works.

### Publish
- Merges as `experimental` (or `active` if you're confident — see §3). Announce it in the team channel so people know it exists.

### Maintain
- Each skill has one **`owner`** (frontmatter) — responsible for fixes and questions.
- **Retiring with a replacement:** set the old skill's `status: superseded` + `superseded-by:` (CI enforces the reverse link); keep it one release cycle for reference, then delete the folder (the catalog updates itself).
- **Retiring with no replacement:** set `status: deprecated` (no `superseded-by:`); keep it one release cycle, then delete the folder.
- **Duplicate found in the wild:** merge into the better one, mark the other `superseded`, update references.

---

## 7. Tooling reference

| Command | Does |
| --- | --- |
| `just new-skill` | Scaffold a valid skill (prompts for domain/action, warns on near-matches, seeds frontmatter, regenerates the catalog). Accepts flags for non-interactive use: `--domain --action --target --owner --description`. |
| `just skills-catalog` | Regenerate `CATALOG.md` from frontmatter. |
| `just skills-lint` | Validate naming, frontmatter, supersedes links, and catalog freshness — the exact check CI runs. **When:** run it before you open or push to a PR, and any time you've hand-edited a skill or its frontmatter *without* `just new-skill`. It's optional (CI runs the same check and is the real gate), but it takes ~1s and catches the problem locally instead of via a red build and a round-trip. Most useful after manual edits, where you must also run `just skills-catalog` and commit the result — otherwise CI fails on a stale catalog. |

> **`just new-skill` and the `skill-creator` skill are complementary, not alternatives.** `just new-skill` makes the box compliant (correct name, frontmatter, catalog); `skill-creator` helps you fill the box (draft the body, tune the `description` for triggering, run evals). CI judges the result regardless of how it was made.
>
> **Want authoring help *and* compliance? Scaffold first, author second:**
> 1. `just new-skill` — creates the compliant shell before any content exists.
> 2. Point `skill-creator` at that skill ("improve this existing skill at `.claude/skills/dot-…`") so it edits the *content*, leaving the name and governance frontmatter intact.
> 3. `just skills-catalog` then `just skills-lint` — skill-creator usually rewrites the `description`, which the catalog shows, so regenerate it before pushing.
> 4. Open the PR — CI confirms.
>
> Don't run `skill-creator` *first*: it creates a non-`dot-` folder with no `owner`/`status` and eval scaffolding tied to that name, forcing a rename and rework afterward.

**Grandfathered skills:** skills listed in `skills.config.json` are exempt from naming/`owner`/`status` checks — their lint issues show as **warnings**, not failures. This is reserved for **vendored/generic skills we did not author** (currently `skill-doctor`), which intentionally keep their upstream name and get no `dot-` prefix. First-party skills should never be grandfathered — bring them into convention instead.

**External skills** (symlinked from `.agents/skills/` — `nx-*`, `angular-developer`, etc.) are not governed here; they appear in the catalog's "External" section for visibility only.
