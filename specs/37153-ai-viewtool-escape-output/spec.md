# Issue Resolution Specification: dotAI: escape viewtool output by default

**Feature Branch**: `37153-ai-viewtool-escape-output`

**Created**: 2026-09-11

**Status**: Draft

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: [dotCMS/core#37153](https://github.com/dotCMS/core/issues/37153) (public stub) with full detail in `dotCMS/private-issues#675` — part of epic [dotCMS/core#37255](https://github.com/dotCMS/core/issues/37255) (dotAI Security Hardening)

**Input**: User description: "37153"

<!--
  This is the dotCMS ISSUE-RESOLUTION spec (used by /speckit-specify-fix). Unlike the
  feature spec, it is framed around a defect: what is wrong, how to reproduce it, and how
  we will know it is fixed. It still flows into /speckit-plan, where the Legacy Impact and
  ADR Alignment gates apply. Keep this technology-light — root-cause and fix details are
  refined in the plan.
-->

## Problem Statement *(mandatory)*

The dotAI Velocity viewtool (`$ai`, registered in `toolbox.xml`) hands the AI provider's reply
to templates exactly as the provider returned it. Completions (`summarize`, `raw`), text
generation (`generateText`) and image generation (`generateImage`) all return a nested JSON
structure whose string values are unescaped. dotCMS Velocity does not auto-escape `$reference`
output (no reference-insertion event handler is configured), so a template that prints any of
those strings into a page emits them as live HTML. Printing the whole object is no safer: the
JSON serializer escapes only `"`, `\` and `</`, so `<img src=x onerror=...>` survives intact.

Model output is not trustworthy markup. It can be steered by content the model was fed
(the prompt-injection surface tracked in #37152), by a crafted prompt, or by hallucination.
When the model emits `<script>` or an `onerror=` attribute and the template renders it, the
payload executes in the browser of **every visitor** of that page. The unsafe path is the
default: an author who writes the documented
`$summary.openAiResponse.choices.get(0).message.content` gets the unsafe behavior with no
signal that a safer choice exists.

**Severity / Impact**: High (P1), per the private issue and epic. Affected: every site whose
templates render `$ai` output to HTML, which is the primary purpose of the tool. Victims are
anonymous site visitors, not just editors. No data loss or privilege escalation by itself, but
arbitrary script in the visitor's origin (cookie theft, session riding, defacement). Rendered
fragments cached with `#dotcache` persist one poisoned reply for every visitor until the cache
expires. The epic-level acceptance this issue owns is: **"Default viewtool accessor escapes
`<script>` / `onerror=` payloads; only the explicit opt-in returns raw."**

### Payload shapes (reference for the rest of this spec)

Values marked `*` are escaped by default after this fix. Everything else is returned as today.

```
summarize(prompt[, index])            search.query(...) / search.related(...)
{                                     {
  "query":  *"...",                     "query":  *"...",
  "dotCMSResults": [{                   "dotCMSResults": [{
      "title": "...",                       "title": "...",
      "<contentlet fields>": ...,           "<contentlet fields>": ...,
      "matches": [{                         "matches": [{
          "distance": 0.31,                     "distance": 0.31,
          "extractedText": *"..."               "extractedText": *"..."
      }]                                    }]
  }],                                   }],
  "openAiResponse": *{ provider JSON:   "total": 3, "count": 3, "limit": 50, ...
      "choices":[{"message":{         }
          "content": *"model text"}}]
  }
}
summarize with no index hits: { "error": "no matching content found ..." }

raw(...) / generateText(...)          generateImage(...)
*{ provider response as rebuilt      *{ provider data[0] as rebuilt by the client:
   by the dotAI client:                  "url": "...",
   "choices":[{"message":{               + added by dotCMS: "originalPrompt": "<echoed prompt>",
       "content": "model text"}}],       "tempFileName": "...", "response": "...", "tempFile": "..." }
   "model": "...", "id": "..." }
```

## Reproduction *(mandatory)*

**Environment**: `main` as of 2026-09-11 (`1ce2510b67`). Any dotCMS instance with the dotAI
app configured on a site so `$ai.isAiEnabled()` is true. Reproducible from any Velocity page
template or `.vtl` widget on that site. No special data required beyond a chat provider, which
will echo markup when asked.

**Steps to Reproduce**:

1. Configure the dotAI app for a site with a working chat provider.
2. Create a page template on that site containing:
   ```
   #set($r = $ai.completions.raw({"model":"<configured model>","messages":[{"role":"user","content":"Reply with exactly this text and nothing else: <img src=x onerror=alert(document.cookie)>"}]}))
   <div>$r.choices[0].message.content</div>
   ```
   Equivalent forms for the other accessors: `$ai.completions.summarize("...")` rendering
   `$r.openAiResponse.choices[0].message.content`; `$ai.generateText("...")` rendering
   `$r.choices[0].message.content`; `$ai.generateImage($request.getParameter("q"))` rendering
   `$r.originalPrompt` (the echoed prompt; reflected XSS when the prompt comes from the request).
3. Publish the page and load it as an anonymous visitor.
4. View source and observe the `<img ...>` tag inside the `<div>`; observe the alert dialog.

**Expected Behavior**: The string reaching the page is HTML-escaped by default:
`&lt;img src=x onerror=alert(document.cookie)&gt;`. It renders as visible text, no image tag is
created, no script runs. A template author who genuinely wants the model's markup rendered
must go through a differently named path whose name makes the unsafe choice explicit.

**Actual Behavior**: The provider's string is inserted verbatim. The browser parses the `<img>`
tag, the image fails to load, `onerror` fires, and the alert (or any attacker script) runs in
the visitor's session. Every accessor listed above behaves this way; none escapes anything.

**Reproducibility**: Always, on every success path of the affected accessors, whenever the
provider reply contains active markup.

## Scope of Investigation *(mandatory)*

<!--
  Keep to WHAT is affected, not the code-level fix (that is the plan's job). But DO name the
  product area, since dotCMS mixes modern and legacy surfaces — this drives Legacy Impact in
  the plan.
-->

- **Affected area**: dotAI, template rendering. Specifically the Velocity viewtool surface
  under the `$ai` key. The payloads (see Payload shapes above) carry three kinds of data:
  - **Provider output** (text the AI wrote, or the provider's response object): the whole
    payload of `completions.raw(String|JSONObject|Map)`, `generateText(String|Map)` and
    `generateImage(String|Map)`; the `openAiResponse` subtree of
    `completions.summarize(String|String,String)`. The dotAI client (langchain4j) rebuilds every
    provider response before dotCMS sees it: chat as `choices[].message.content` plus `model`/`id`,
    image as `data[].url` only. The image payload therefore carries no model-written text; it is in
    scope for its echoed `originalPrompt` and for one uniform rule.
  - **Echoed caller input**: the `query` value and the image `originalPrompt` value, copied
    from the caller's arguments into the payload. A template that prints "Results for
    $r.query" with a query taken from a request parameter is reflected XSS through the tool.
    (For `related`, `query` is the contentlet's own text; it is escaped anyway for one rule.)
  - **Stored search text**, `dotCMSResults[].matches[].extractedText`. Normally this is a
    plain-text excerpt of a contentlet (HTML is stripped at index time), so escaping it changes
    nothing visible. It is escaped because it does not always come from a content author.
    Before every search, the embeddings API stores the query text itself as a row in an index
    named `cache` (`EmbeddingsAPIImpl.saveEmbeddingsForCache`), with the raw query as
    `extractedText`. `search.query(String,String)` and `search.query(Map)` let the caller pick
    the index name. A template that searches the `cache` index therefore renders other
    visitors' unescaped query text as `extractedText`.
  - **Contentlet fields** in `dotCMSResults`, including `title`: the same data `$dotcontent`
    hands to every other template unescaped, authored by permissioned users. **Not escaped by
    this fix**; access control for it is #37151.

  The embeddings sub-tool returns counts and index names and is not affected.
  `completions.getConfig()` returns admin-configured prompt templates and is not affected.
  The dotAI REST resources (`/api/v1/ai/*`) call the same API methods and return the result
  as JSON to API clients who own their own rendering; they are **not** in scope and nothing
  there is escaped.
- **Suspected surface**: Modern only, `com.dotcms.ai.viewtool.*` (`AIViewTool`,
  `CompletionsTool`, `SearchTool`). The payloads are built in `com.dotcms.ai.api.*`, which is
  shared with the REST layer, so the escaping must sit at the viewtool boundary. Shared
  utilities involved: `com.dotmarketing.util.json.JSONObject` / `JSONArray` (the result
  carrier; `JSONObject implements Map`, `JSONArray implements List`) and the OWASP Java
  Encoder (`org.owasp.encoder.Encode`), already a dependency and already used by
  `OwaspEncoderTool` (`$owasp`) and `com.liferay.util.Xss`. Legacy impact is expected to be
  read-only use of those utilities; the plan confirms.
- **Related known decisions**: None known. The epic's principle applies: fix at the boundary
  so every current and future template consumer inherits it. Sibling
  [#37154](https://github.com/dotCMS/core/issues/37154) (stop returning stack traces; spec PR
  #37455 open) edits the same three classes; merge order is not fixed (see Regression Risk)
  and this spec does not depend on its code. The plan formally consults
  `dotCMS/platform-adrs`.

## Root-Cause Hypothesis

Each viewtool method calls the corresponding API method and returns its result unchanged. No
output encoding exists anywhere between the provider and the page.

Three shape facts drive the fix. First, the payloads are **nested objects, not strings**: the
model text sits at `openAiResponse.choices[n].message.content` (summarize) and at
`choices[n].message.content` (raw, generateText); the image payload has no model text, only the
provider `url` and the echoed `originalPrompt`.
Second, the provider output is attached to the dotCMS-built result at exactly one seam per
method, so the escaped set can be stated exactly: the whole provider subtree plus three named
fields (`query`, `originalPrompt`, `dotCMSResults[].matches[].extractedText`). Nothing else.
Third, `CompletionsTool` already has a method named `raw(...)` meaning "send this raw provider
request"; that name is taken and cannot double as the unescaped-output opt-in.

## Fix Scope & Non-Goals *(mandatory)*

**In scope**:

- **What is escaped by default.** Exactly this set, and nothing else:
  1. every string value at any depth inside the provider output (the whole payload of `raw`,
     `generateText`, `generateImage`; the `openAiResponse` subtree of `summarize`);
  2. `query`;
  3. `originalPrompt`;
  4. `dotCMSResults[].matches[].extractedText`.

  Keys, numbers, booleans, nulls (including the JSON library's `NULL` sentinel), `title` and
  every other contentlet field are unchanged. The payload keeps its current shape and type
  (`JSONObject`, still a `Map`), so `$r.openAiResponse.choices[0].message.content` keeps
  resolving; only the text differs. The no-hits `summarize` shape, a single-key
  `{"error": "..."}` object, is escaped like any other payload and has no subtree to look for.
- **The encoder is the OWASP Java Encoder's HTML encoder** (`Encode.forHtml` or an equivalent
  that covers `& < > " '`), so the result is safe in HTML body and quoted-attribute context.
  `UtilMethods.escapeHTMLSpecialChars` is not acceptable: it leaves quotes unescaped.
- **Copy, never mutate.** Escaping produces a new structure and leaves the API's object
  untouched. The routine must accept `JSONObject`, `JSONArray`, plain `Map` and plain `List`,
  because the tools' error paths return immutable `Map.of(...)` payloads, not `JSONObject`.
- **One shared routine, one gate.** Escaping is applied at the viewtool boundary through a
  single routine, and whether it runs is decided by a single flag on the tool, so a new
  viewtool method added later cannot forget it by copying an old one.
- **One opt-in switch: `$ai.unsafe`.** The parent tool exposes an accessor named `unsafe`
  that returns the same tool with escaping turned off. `$ai.unsafe` behaves exactly as `$ai`
  does today, for every method: `$ai.unsafe.completions.summarize(...)`,
  `$ai.unsafe.completions.raw(...)`, `$ai.unsafe.generateText(...)`,
  `$ai.unsafe.generateImage(...)`, `$ai.unsafe.search.query(...)`,
  `$ai.unsafe.search.related(...)`, and also `$ai.unsafe.embeddings.*`,
  `$ai.unsafe.isAiEnabled()` and `$ai.unsafe.completions.config`, which were never escaped and
  are identical on both. There are no per-method `*Unescaped` siblings and the opt-in does not
  reuse `raw`.
- **Error payloads are escaped in full** in default mode for the methods that return one
  (`summarize`, `raw`, `search.*`, `generateImage`), so safety does not depend on the merge
  order with #37154. `generateText` has no error payload today: it rethrows, and the Velocity
  method-exception handler swallows it in live mode. Whether it gets a handler is #37154's
  decision; this fix only escapes whatever it returns.
- **A back-compat note for template authors**, delivered as `release-note.md` in this spec
  directory (precedent: `specs/37276-silent-index-delete-loss/release-note.md`). It uses the
  `$ai` key, lists the affected accessors, states which `dotCMSResults` fields change, shows a
  before/after of a template that renders model text, names `$ai.unsafe`, and warns about
  the three regressions named under Blast radius (self-escaping templates, Markdown passed to
  a client-side renderer, values used outside HTML context).
- Tests, per Acceptance & Verification, added to the existing viewtool integration test
  classes (already registered in `MainSuite2b`) plus a plain unit test for the shared routine.

**Explicitly out of scope / non-goals**:

- Escaping contentlet fields in `dotCMSResults`. They are stored dotCMS content rendered
  unescaped by every other template path. Access control over them is #37151.
- Allow-list HTML sanitizing as the default. The private issue accepts either; this spec
  chooses escaping (see Assumptions).
- Escaping in the dotAI REST resources or in `com.dotcms.ai.api.*`.
- A global configuration flag that restores unescaped output for every template. The
  acceptance criterion says raw output is available **only** through the explicit opt-in.
- Context-specific encoding beyond HTML (JavaScript string, URL, CSS). Templates that place
  values in those contexts use `$ai.unsafe` plus the existing `$owasp` tool.
- Changing what the model is asked or what content is retrieved (#37152, #37151), rate
  limiting (#37155), stack-trace removal (#37154), or the `tempFile` absolute path present in
  the image payload.
- Any change to `EmbeddingsTool`, `completions.getConfig()`, or `$ai.isAiEnabled()`.
- Rewriting or restructuring the viewtool classes beyond the shared routine, the flag and the
  `unsafe` accessor.

## Regression Risk *(mandatory)*

- **Blast radius**: Every `$ai` call on every customer template, on the **success path**, for
  the four escaped items only. Specific consequences:
  - Model output that legitimately contains markup (models emit `<p>`, `<b>`, `<ul>` when
    asked for HTML) renders as literal text until the author switches to `$ai.unsafe`. This is
    the intended behavior change and the reason the release note is a deliverable.
  - Model output in Markdown, the provider default, displays identically when printed as
    text, but Markdown carries `>` (blockquotes), `<` (inline code) and quotes. A template
    that passes model Markdown to a client-side renderer sees literal entities and must use
    `$ai.unsafe`.
  - Templates that already escape the output themselves (`$owasp.forHtml(...)`,
    `$esc.html(...)`) now double-encode and display `&amp;lt;` literally. They must drop their
    own encoder or switch to `$ai.unsafe` plus `$owasp`.
  - Templates that render search results see no visible change: `extractedText` is plain text
    already, and `title` and the contentlet fields are untouched. The `query` echo changes
    only if it contained markup.
  - Non-HTML consumers of provider values (a value written into a `<script>` block, a URL
    fetched server-side, or compared with `==`) see entity-encoded text. `$ai.unsafe` covers
    these.
  - Strings already containing entities (`&amp;`) are double-encoded to `&amp;amp;` by a
    correct encoder and display as `&amp;`. Provider output is not pre-escaped today, so this
    is an edge, not the norm.
  - A repository search finds no shipped template, starter or `core-web` code calling `$ai.`,
    so the blast radius is entirely in customer templates.
  - The existing integration tests (`CompletionsToolTest`, `SearchToolTest`, `AIViewToolTest`)
    cast the result to `JSONObject` and assert on structure or on plain text without
    HTML-significant characters (the one query containing apostrophes asserts only non-null).
    They are expected to pass unchanged; any assertion that turns out to depend on `& < > " '`
    must be updated deliberately, not loosened.
- **Backward compatibility**: No API, schema, index mapping or persisted state changes. The
  change is a **template contract change**: any template relying on unescaped provider output
  must be edited to go through `$ai.unsafe`. Return type and shape are preserved, so templates
  keep resolving properties; only rendered text changes. **Rollback note (category H-8, VTL
  viewtool contract change)**: templates edited to use `$ai.unsafe` are stored data and survive
  a rollback, but on the previous release `$ai.unsafe` resolves to null and those templates
  fail to render. The release note must say so, and the implementation PR carries the
  rollback-unsafe label. The safer "ship the accessor first, flip the default later" sequence
  was considered and rejected because the default is the security fix. #37154
  edits the same classes; its spec is open and not merged. Whichever lands first, the other
  rebases; neither depends on the other's code.
- **Data considerations**: None. No stored data is read differently or repaired. The
  `EMBEDDING_CACHE`, the embeddings table and `#dotcache` entries are untouched; cached
  fragments rendered before the fix expire on their own TTL.

## Acceptance & Verification *(mandatory)*

<!-- Measurable, so the fix is provably done. -->

- **AC-001**: The reproduction steps above no longer produce the actual behavior; they produce
  the expected behavior. For `summarize(String)`, `summarize(String,String)`, `raw(String)`,
  `raw(JSONObject)`, `raw(Map)`, `generateText(String)`, `generateText(Map)`,
  when the provider reply's model-text field is exactly
  `<script>alert(1)</script><img src=x onerror=alert(1)>`, the default accessor's model-text
  field is exactly `&lt;script&gt;alert(1)&lt;/script&gt;&lt;img src=x onerror=alert(1)&gt;`
  (the OWASP `Encode.forHtml` output of the stub string), and no string value anywhere in the
  provider-output part contains a literal `<`, `>`, `"` or `'`. For `generateImage(String)`,
  `generateImage(Map)`: when the prompt carries the same markup, `originalPrompt` is returned as
  its `Encode.forHtml` output, `url` is unchanged, and no string value contains raw markup.
- **AC-002**: For `search.query(String,String)`, `search.query(Map)` and `summarize`: when the
  indexed `extractedText` and the result `title` both contain the same markup, the default
  accessor returns `extractedText` escaped, returns `title` and every other `dotCMSResults`
  value identical to today's payload (markup intact), and returns the `query` value escaped.
  The same holds when the query targets the `cache` index. `search.query(String)` and both
  `search.related` overloads share the same wrapped return and are covered by the code path,
  not by a separate test.
- **AC-003**: For every accessor in AC-001 and AC-002, the same call through `$ai.unsafe`
  returns a payload byte-for-byte identical to what the API method returns, including the
  literal `<script>alert(1)</script>` and `<img src=x onerror=alert(1)>`.
- **AC-004**: Default and unsafe payloads are otherwise identical: same keys at every depth,
  same array lengths, same non-string values. Only string values differ, and only inside the
  provider-output part and the echoed-input values, and only where they contained characters
  the encoder changes.
- **AC-005**: The escaped payload still resolves through property access
  (`get("openAiResponse")` → `getJSONArray("choices")` → index 0 → `getJSONObject("message")`
  → `getString("content")`), and the existing success-path tests in `CompletionsToolTest`,
  `SearchToolTest` and `AIViewToolTest` pass unchanged.
- **AC-006**: The API method's own return value is not mutated by the default accessor: after
  a default call, the object the API returned still contains the literal markup. Error
  payloads are escaped in full and returned without throwing: for `completions.raw` a
  WireMock mapping answering the dedicated prompt with HTTP 500 and a non-JSON prompt, for
  `summarize` the no-hits `{error}` shape, for `generateImage` a prompt no stub answers. The
  `search.*` error path uses the same routine and is covered by the unit test (including
  non-string leaves such as stack-trace elements, which are stringified and encoded).
  `generateText` rethrows today; that behaviour is unchanged and not asserted.
- **AC-007**: `release-note.md` exists in this spec directory and covers everything listed for
  it under In scope.
- **AC-008**: `CompletionsAPIImpl.summarize` and `EmbeddingsAPIImpl.searchForContent` still
  return the literal markup, so the REST resources, which return those objects unmodified,
  are unaffected.
- **Verification method**:
  - Integration tests in the existing classes, all already in `MainSuite2b`:
    `-Dit.test=CompletionsToolTest#<new escape methods>`,
    `-Dit.test=SearchToolTest#<new escape methods>`,
    `-Dit.test=AIViewToolTest#<new escape methods>`. Markup is injected through new WireMock
    mappings under `dotcms-integration/src/test/resources/mappings/` that match a dedicated
    prompt string and return `<script>alert(1)</script><img src=x onerror=alert(1)>` in
    `choices[0].message.content` (chat), following the existing `light-speed-stub.json` pattern;
    the image stub answers a prompt that itself carries the markup (the payload's only text is the
    echoed `originalPrompt`), following `ganymede-moon-image-stub.json`; plus one
    mapping answering HTTP 500 for AC-006. For search and summarize, the markup is seeded as
    `extractedText` and `title` through the `EmbeddingsDTODataGen` builder into a dedicated
    `escape-probe` index (so probe rows can never feed another test's prompt); one case seeds
    and queries the `cache` index. Each test runs the same call through `$ai` and `$ai.unsafe`
    and asserts on the pair; AC-006 asserts against the API's object after the default call.
  - A plain unit test under `dotCMS/src/test` for the shared escaping routine: nested
    `JSONObject`, `JSONArray` of strings, arrays of objects, plain `Map` and `List` including
    an immutable `Map.of(...)`, mixed non-string leaves, `null`, empty structures, strings
    already containing entities, and a string containing every HTML-significant character.
  - AC-008: one targeted assertion through `CompletionsAPIImpl.summarize` /
    `EmbeddingsAPIImpl.searchForContent` confirming the API layer still returns unescaped text,
    so the REST layer is provably untouched.
  - TDD gate (Constitution Principle V): the new tests are written first, approved, and shown
    failing on `main` (they fail because the default accessor returns the literal `<script>`
    and because `$ai.unsafe` does not exist) before any viewtool code changes.
  - Manual: run the reproduction on a local instance; confirm the page shows the markup as
    text and no alert fires; switch the template to `$ai.unsafe` and confirm the original
    behavior returns. Record both renderings for the QA brief.

## Assumptions

- **Escaping, not sanitizing, is the default.** The private issue allows either. Escaping is
  chosen because it has no policy surface to maintain, no sanitizing precedent exists in the
  codebase, it is safe in both body and attribute context, and it is what the epic-level
  acceptance literally describes.
- **"AI output" means what the provider returned, what the tool echoes back from the caller,
  and the stored search excerpt (`extractedText`), which can carry other visitors' input.**
  Contentlet fields travelling in the same payload, `title` included, are not AI output. This
  keeps the fix inside the issue's wording, keeps search-result templates working, and closes
  the `cache`-index case.
- **Escaping identifiers, model names, URLs and timestamps inside the provider object is
  accepted** as the price of not maintaining a field list. It is harmless in HTML context.
- **The encoding target is HTML body and quoted-attribute context.** Other contexts are the
  author's responsibility via `$ai.unsafe` and `$owasp`.
- **`$ai.unsafe` satisfies "explicit, clearly named opt-in method."** It is a method on the
  parent tool, the word `unsafe` appears in every raw call, and it keeps the public surface at
  today's method count plus one accessor.
- The `$ai` key in `toolbox.xml` is the real template name; the issues say `$dotAI`.
