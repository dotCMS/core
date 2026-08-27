# Coverage matrix

Nine product-surface axes. For **each**, decide **In scope** (≥1 manual case) or **Out of scope**.
The plan publishes no out-of-scope list, so this walk is invisible in the output — which is exactly
why it must be deliberate. A forgotten axis and a consciously excluded one produce an identical
plan, and only one of them is right.

Walk the matrix **once** over the union of everything the PR changed, not once per issue.

| Axis | What to vary | In scope when |
|---|---|---|
| **Permissions** | anonymous, back-end user, front-end user, CMS Anonymous role, CMS Owner, individual vs. inherited, role revocation | the diff touches `Permission*`, `Role*`, a REST endpoint, or any `checkPermission` path |
| **Sites / hosts** | `SYSTEM_HOST`, default host, secondary host, cross-host references | the diff touches `Host*`, `Identifier*`, multi-tenant content, or host-scoped queries |
| **Languages** | default language, non-default language, `DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE` on/off, missing translation | the diff touches `Contentlet`, `Language*`, search, rendering, or anything taking a `languageId` |
| **Content version state** | working only, live, archived, multiple versions, working ≠ live | the diff touches `Versionable`, publish/unpublish, or workflow |
| **Cache layers** | cold cache, warm cache, **invalidation after mutation**, **cross-node invalidation** | any mutation of cached state |
| **Workflow** | default scheme, non-default scheme, required action, mandatory step | the diff touches `Workflow*` or content state transitions |
| **Push Publish** | bundle generation, receiver replay, integrity checks | the diff touches publishable entities (content, templates, containers, content types, workflows, categories) |
| **Persistence** | **PostgreSQL only** — CI does not run H2 | any DB-touching code |
| **UI / UX** | see the checklist below | any of the triggers below |

### UI / UX triggers

The axis is in scope if **any one** of these holds:

- the diff touches `core-web/`
- the diff includes `*.html`, `*.scss`, `*.component.ts`
- an issue or acceptance criterion names a user-visible surface (screen, dialog, button, form,
  selector, drawer — "Content Drive", "Site Browser", "Host Folder Field", "Page Editor", …)
- the diff changes a REST endpoint that `core-web/` consumes
- an issue carries a UI-area label
- screenshots, Figma links, or video are attached

**A backend-only diff does not exempt this axis** when an acceptance criterion names a user-visible
surface. If the change is reachable from a screen, a human has to look at that screen.

### UI / UX checklist

When the axis is in scope, cover each sub-aspect that genuinely applies. These are all things a
person checks by hand:

- **Visual rendering** — correct content on the happy path and in each non-default state (empty,
  loading, error, disabled)
- **Keyboard navigation** — logical tab order, every interactive element reachable, `Escape` closes
  modals and menus, `Enter` / `Space` activate buttons
- **Focus management** — opening a dialog moves focus into it; closing returns focus to the trigger;
  route changes move focus to the main heading
- **Screen reader** — interactive elements have accessible names; dynamic changes are announced
- **Error / loading / empty states** — correct copy, and recoverable (the retry actually works, an
  error doesn't strand the user)
- **Responsive layout** — usable at mobile, tablet, and desktop widths; no overflow, overlap, or
  unreachable actions
- **i18n** — copy resolves for the default language and one non-default language; long translations
  don't break the layout
- **Theme / contrast** — light and dark both render correctly; text stays legible
- **Copy** — labels, buttons, errors, and tooltips match the spec or the acceptance criteria

---

## Mandatory cases

Include these whenever the trigger applies. All are `Manual`, like every other case.

**One per issue, always** — `Verify: #<issue>`: the original bug no longer reproduces on the
post-merge build. Run the issue's own reproduction steps against the build under test and confirm
the fixed behavior. Spell out environment, user, site, screen, and click sequence.

| Trigger in the diff | Case |
|---|---|
| Mutates cached state | **Cache invalidation** — mutate, re-read, confirm the new value. `High`. |
| Touches a `*Cache*` layer or the cache transport | **Cross-node invalidation** — mutate on node A, read from node B, confirm B returns the new value. dotCMS runs clustered; an entry that evicts in-process can stay stale on a peer. `High`. |
| Touches `com.dotcms.rest.*` | **401 unauthenticated** — give the exact `curl`. `High`. |
| Touches `com.dotcms.rest.*` | **403 authenticated but unauthorized**. `High`. |
| Touches `com.dotcms.rest.*` | **Response contract** — response JSON matches the declared `@Schema`. `Medium`. |
| Adds or changes a startup/upgrade task, or any DDL | **Upgrade on a populated DB** — restore a pre-fix snapshot, deploy, confirm the task runs once cleanly and the schema matches, then restart and confirm it does not re-run. Cross-reference `docs/core/ROLLBACK_UNSAFE_CATEGORIES.md`. `High`. |

An axis or mandatory case already covered by a test the PR itself added gets **no manual case** —
CI checks it on every build. Drop it silently; the plan does not enumerate what was skipped.
