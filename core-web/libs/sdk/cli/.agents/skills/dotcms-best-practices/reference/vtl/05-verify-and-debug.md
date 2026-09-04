# 05 · Verify & debug (created ≠ renders)

Content that saved with HTTP 200 can still render blank. Verification has **two layers** — they catch different failures, and passing one does NOT imply the other:

- **Layer 1 — validate the VTL** (`/api/vtl/dynamic`): does the code parse and do its toolbox/`$dotcontent` calls execute? This runs in a **request context, not a render context** — so it cannot see anything the page pipeline injects.
- **Layer 2 — verify the render** (page verification): does the assembled page actually produce HTML in each slot? This is the only layer that exercises the full pipeline (`$URLMapContent`, `$CONTENTLETS`, `$dotTheme`, per-container vars).

A container/detail VTL that is syntactically perfect in Layer 1 can still render blank in Layer 2. Do both.

VTL syntax: [velocity.md](velocity.md). What a page needs to render: [core/00](../core/00-what-must-exist.md).

---

## Layer 1 — validate VTL with `/api/vtl/dynamic`

POST your VTL to `/api/vtl/dynamic` with body **`{ "velocity": "<your vtl>" }`** (that exact key — a wrong shape throws `ArrayIndexOutOfBoundsException`, not a helpful message). It evaluates against a plain web-request context and throws with line/column on a parse/eval error (instead of `#dotParse` swallowing it into a blank HTTP 200). Use it on every non-trivial VTL **before** you upload it.

**What this context HAS** (so these validate for real):
- `$dotcontent` (`.pull` / `.find` / `.search`), `$dotJSON` (`$dotJSON.put(...)` → returns JSON), `$host`, `$user`, `$visitor`, `$request`/`$response`.
- The whole toolbox: `$date`, `$math`, `$number`, `$text`, `$esc`, `$navtool`, `$categories`, `$sql`, `$json`, … (~65 viewtools), plus `$UtilMethods`, `$language(s)`.

**What it does NOT have** (these are injected only during a page/container/detail/theme render — they'll report `UNDEFINED_REFERENCE` here, which is expected, not a bug in your code):
- `$URLMapContent` — detail-page only (needs the URL-map pipeline).
- `$CONTENTLETS`, `$dotContentMap`, and per-container vars (`$contentletList<uuid>`, `$totalSize<uuid>`) — container-scoped, `#set` mid-render.
- `$dotPageContent`, `$dotTheme` / `$dotThemeLayout` — page/theme render only. For the theme in the playground, use `$templatetool.theme(inode, host)` instead of `$dotTheme`.
- `$urlContentMap` — **never a VTL var at all**; it's only a field in the Page REST API JSON. The detail-page var is `$URLMapContent`.
- `#dotParse` works only with an **explicit path**, not the bare page-shell form.

So Layer 1 fully validates a **listing/query VTL or a `$dotJSON` script**. For a **container or detail VTL** it validates only the parts that don't touch render-injected vars — the rest must go to Layer 2.

---

## Layer 2 — verify the render

Call the page-verification tool. It renders the page, absorbs the raw `page/render` footguns (host resolution, the `200 ≠ rendered` trap, the two rendered layers that disagree), and returns a per-slot `verdict` — its description is the source of truth for the mechanics. Each verdict tells you where the fix lives:

- **`empty-vtl-error`** — content is placed but the slot rendered empty → the VTL failed. Back to Layer 1: run that container's VTL through `/api/vtl/dynamic` for the line/column.
- **`empty-no-content`** — slot resolved but nothing placed → a placement gap, not a code bug. Fill it via content placement ([09-placement.md](../core/09-placement.md)).
- **`cache-stale`** — slot rendered but `page.rendered` is empty → page cache. Set `cachettl:"0"` and re-publish (publish rules: [core/00](../core/00-what-must-exist.md)).
- **Renders, but wrong/empty field** (verdict `ok`, output looks off) → a correctness bug, not a pipeline break — see the rules below.

### Slot-level and page-level render are different things
A slot renders **in isolation**; the theme then has to assemble those slots into the page. Those can disagree: containers that render perfectly on their own still produce a page that is only the shell if the theme's layout loop drops them. When slot output looks right but the page doesn't contain it, the fault is theme-side — debug the layout loop ([02-themes.md](02-themes.md)), not the container or the placement.

---

## Renders, but blank or wrong — triage

When the render is empty or shows the wrong value (not a hard error), the cause is almost always an author-time mistake. The rules themselves live where you write the code — this is just the symptom → where-to-look map:

- **Wrong render path** (widget code in a `<Var>.vtl`, or vice versa) → decision table in [01-choose-mechanism.md](01-choose-mechanism.md).
- **Reading the wrong context var** (`$URLMapContent` vs `$dotContentMap` vs a `$dotcontent` query) → [velocity.md](velocity.md) §2 (context objects) + [01-choose-mechanism.md](01-choose-mechanism.md). Note Layer 1 can't catch this — those vars are absent in the request context.
- **A Select/Checkbox field treated as a string, a method called on a literal, a reassigned context object, a typo'd method printing as text** → the VTL gotchas in [velocity.md](velocity.md) §5.
- **A broken image** (`${field}` instead of the `/dA/` URL) → the images rule in [velocity.md](velocity.md) §4.

### The code-execution sandbox (it's JavaScript, not VTL)
- **Velocity vars don't exist here.** `$dotcontent`, `$date`, `#foreach` → `ReferenceError`. Query content via `POST /api/content/_search` (`{query, limit, sort}` — **not** `/api/v1/content/...`, which 404s misleadingly). Run VTL only through `POST /api/vtl/dynamic`.
- **`await` everything; return only JSON-serializable values.** An un-awaited Promise (or a function/class) → `DataCloneError`. Return plain objects/arrays/strings.
- **Watch string literals.** A raw apostrophe in a single-quoted JS string (`'grandchild's'`) → `SyntaxError`. Use double quotes or escape.
- **`.rendered` on a container is an object keyed by `uuid-N`, not a flat string.** Index into `rendered["uuid-1"]` before string ops like `.slice`.
