# Issue Resolution Specification: dotAI: Stop returning Java stack traces from AI viewtool exceptions

**Feature Branch**: `37154-ai-viewtool-stack-trace`

**Created**: 2026-09-08

**Status**: Draft

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: [dotCMS/core#37154](https://github.com/dotCMS/core/issues/37154) — part of epic [dotCMS/core#37255](https://github.com/dotCMS/core/issues/37255) (dotAI Security Hardening)

**Input**: User description: "https://github.com/dotCMS/core/issues/37154"

<!--
  This is the dotCMS ISSUE-RESOLUTION spec (used by /speckit-specify-fix). Unlike the
  feature spec, it is framed around a defect: what is wrong, how to reproduce it, and how
  we will know it is fixed. It still flows into /speckit-plan, where the Legacy Impact and
  ADR Alignment gates apply. Keep this technology-light — root-cause and fix details are
  refined in the plan.
-->

## Problem Statement *(mandatory)*

The dotAI Velocity viewtool (`$ai`, registered in `toolbox.xml`) exposes sub-tools for
completions and semantic search. When a call to one of these sub-tools fails, the tool does not
throw. It returns a result object that carries the failure, and that object includes the
**full Java stack trace** of the exception alongside the exception message.

Nothing gates this on environment or on a debug flag. A template that prints the result, or
iterates its keys, renders the trace into the page. Frames name internal classes, packages,
source files and line numbers, and the exception message can carry upstream detail such as the
provider's response text or connection target. This is information disclosure to any site
visitor.

**Severity / Impact**: Low severity (P2), per the issue. Affects every site whose templates
render the raw result of `$ai.completions.summarize(...)`, `$ai.completions.raw(...)`,
`$ai.search.query(...)` or `$ai.search.related(...)` when the call fails. Failure is a normal
condition (provider down, model misconfigured, quota exceeded, malformed prompt), so the leak is
reachable in ordinary operation, not only under attack. No data loss or privilege escalation.
The epic-level acceptance this issue owns is: **no dotAI code path returns a stack trace to a
caller or template**.

Two adjacent paths on the same tool are covered by that epic-level wording and are part of this
fix. `$ai.generateImage(...)` does not return a trace, but its handler returns the raw exception
message, which for wrapped exceptions begins with a class name. `$ai.generateText(...)` does not
catch at all; the exception propagates into the rendering layer, and whether that layer prints
the exception into the page has not been verified.

## Reproduction *(mandatory)*

**Environment**: `main` as of 2026-09-08. Any dotCMS instance with the dotAI app configured on
a site. Reproducible from any Velocity page template or `.vtl` widget on that site.

**Steps to Reproduce**:

1. Configure the dotAI app for a site so `$ai.isAiEnabled()` is true.
2. Make the provider call fail deterministically. Any of these work: point the provider URL at a
   closed port, use an invalid API key, or pass a malformed prompt to `raw`
   (for example `$ai.completions.raw("this is not JSON")`).
3. In a template on that site, call the tool and print the whole result:
   `#set($r = $ai.completions.summarize("anything"))` then `$r`.
   Repeat with `$ai.search.query("anything")` and `$ai.generateImage("anything")`.
4. On a second template, call `$ai.generateText("anything")` with the same broken provider and
   render the page. This step establishes whether the propagated exception reaches the page.
5. Load both pages as an anonymous visitor.

**Expected Behavior**: The rendered result contains an `error` entry with a short, generic
message and nothing else. No stack frames, class names, package names, file names or line
numbers appear anywhere in the response. The server log carries the full exception with its
stack trace at error level.

**Actual Behavior**: The rendered result contains a `stackTrace` entry. For completions it is a
multi-line string beginning with the exception class name and followed by `at com.dotcms...`
frames. For search it is a list whose elements render as `com.dotcms.ai.api.EmbeddingsAPIImpl.
searchForContent(EmbeddingsAPIImpl.java:NNN)` and similar. The `error` entry carries the raw
exception message, which for wrapped exceptions starts with the wrapped class name, for example
`java.net.ConnectException: Connection refused`. The `generateImage` result has no `stackTrace`
entry but its `error` entry carries that same raw message. The `generateText` page outcome is
to be recorded during planning: either the rendering layer swallows the exception, or it prints
the exception text or trace into the page.

**Reproducibility**: Always, on every failure path of the affected methods.

## Scope of Investigation *(mandatory)*

<!--
  Keep to WHAT is affected, not the code-level fix (that is the plan's job). But DO name the
  product area, since dotCMS mixes modern and legacy surfaces — this drives Legacy Impact in
  the plan.
-->

- **Affected area**: dotAI, template rendering. Specifically the Velocity viewtool surface under
  the `$ai` key: completions (`summarize`, `raw` overloads), search (`query` overloads,
  `related` overloads), and the top-level `generateImage` and `generateText` overloads. The
  dotAI REST resources are a separate surface and are not affected; they do not catch and
  return traces.
- **Suspected surface**: Modern only, `com.dotcms.ai.viewtool.*`. No `com.dotmarketing.*` code is
  involved beyond the shared `Logger` and `JSONObject` utilities. Legacy impact is expected to
  be nil; the plan confirms.
- **Related known decisions**: None known. The epic's principle applies: fix at the boundary
  (the viewtool exception handlers) so all current and future template consumers inherit it.
  The sibling issue [#37153](https://github.com/dotCMS/core/issues/37153) (escape viewtool output
  by default) will touch the same three classes; see Regression Risk. The plan formally consults
  `dotCMS/platform-adrs`.

## Root-Cause Hypothesis

The exception handlers in the two sub-tools were written as debugging aids and never
production-hardened. The completions tool has one private handler used by all four public
entry points that writes the printed stack trace into the returned map. The search tool has
three inline handlers, one per public entry point, that put the stack-frame array into the
returned map. Neither logs the exception server-side, so the trace in the payload was the only
diagnostic available, which is likely why it was kept.

The top-level `$ai.generateImage` handler returns only the message, so it leaks no frames, but
the message itself is exception-derived and can name classes. `$ai.generateText` has no handler;
it rethrows, and the leak question moves to the rendering layer, which is outside this package
and has not been inspected. The embeddings sub-tool has no exception handling at all. A grep of
the whole `com.dotcms.ai` package for stack-trace access finds only the five sites named above.

## Fix Scope & Non-Goals *(mandatory)*

**In scope**:

- Every failure path in the completions sub-tool (`summarize` and the three `raw` overloads) and
  the search sub-tool (`query` overloads and `related` overloads) returns a payload with an
  `error` entry holding a fixed, generic, safe message and no other diagnostic content. No
  `stackTrace` entry. No exception class names, frames, file names or line numbers anywhere
  in the payload.
- The full exception, including its stack trace, is logged server-side through the platform
  `Logger` at error level, at the point where it is caught.
- The `error` key name is preserved so templates that already test `$result.error` keep working.
- A single shared handling routine is preferred over five copies, so the next viewtool method
  cannot regress by copying an old handler. The plan decides where it lives.
- The top-level `generateImage` overloads use that same shared routine, so their `error` entry
  becomes the same fixed string and no exception-derived text remains anywhere on the tool.
- The `generateText` path is verified during planning: force a provider failure and render a
  page that calls it. If the rendering layer prints the exception message or trace into the
  page, `generateText` is brought under the shared handler in this fix. If the rendering layer
  swallows it and renders nothing, `generateText` is left as is and the finding is recorded in
  the plan.
- Tests, per the Acceptance section, added to the existing viewtool integration test classes,
  which are already registered in a CI suite.

**Explicitly out of scope / non-goals**:

- `AppConfig.debugLogger` prompt/response logging behind `DEBUG_LOGGING` / `AI_DEBUG_LOGGING`.
  Explicit default-off flag, working as intended, per issue and epic.
- Escaping or sanitizing successful model output. That is #37153.
- Changing `$ai.generateText` for consistency alone. It is changed only if the planning
  verification above shows the propagated exception reaches the page.
- Fixing how the rendering layer itself handles propagated exceptions. If that layer prints
  traces for any viewtool, that is a platform-wide issue to be filed separately; this fix only
  stops dotAI from being a source.
- The dotAI REST resources. They do not return traces today.
- Adding a dev/prod or debug gate that keeps traces in the payload under some flag. The issue
  asks for removal, not gating.
- Any behaviour change on the success path. Successful calls return exactly what they return
  today.
- Prompt-injection mitigation (#37152), rate limiting (#37155), per-user retrieval filtering
  (#37151).

## Regression Risk *(mandatory)*

- **Blast radius**: Only the failure path of the affected viewtool methods: the six completions
  and search methods, the two `generateImage` overloads, and conditionally the two
  `generateText` overloads. Any template that reads `$result.stackTrace` will now get nothing. A search of the repository's shipped
  templates and of the dotAI tests finds no consumer of that key. Templates that read
  `$result.error` keep working, but the text changes from the raw exception message to a fixed
  string, so a template that branched on message content would stop matching. This is accepted
  as the point of the change.
- **Backward compatibility**: No API, schema, index mapping, or persisted state changes.
  Rollback-safe. The two viewtool sub-tools are constructed per call by the parent tool and hold
  no state. #37153 will edit the same three classes; merge order is #37154 first, then #37153
  rebases. The fixed error string carries no user or model data, so #37153 has nothing to escape
  in it.
- **Data considerations**: None. No stored data is involved.

## Acceptance & Verification *(mandatory)*

<!-- Measurable, so the fix is provably done. -->

- **AC-001**: The reproduction steps above no longer produce the actual behavior; they produce
  the expected behavior. For each of `summarize(String)`, `summarize(String,String)`,
  `raw(String)`, `raw(JSONObject)`, `raw(Map)`, `query(String)`, `query(String,String)`,
  `query(Map)`, `related(Contentlet,String)`, `related(ContentMap,String)`,
  `generateImage(String)`, `generateImage(Map)`: when the
  underlying call throws, the returned payload has an `error` entry, has no `stackTrace` entry,
  and no string value anywhere in the payload contains `Exception`, the frame marker `at ` followed by a fully qualified method,
  `.java:`, or the package prefix `com.dotcms`.
- **AC-002**: On the same forced failures, the platform log receives one error-level entry
  from the viewtool class that includes the original exception (message and stack trace).
- **AC-003**: Regression check for the blast radius. The existing success-path tests in
  `CompletionsToolTest` and `SearchToolTest` continue to pass unchanged. No successful call's
  payload changes shape or content.
- **AC-004**: A grep of `dotCMS/src/main/java/com/dotcms/ai` for `printStackTrace`,
  `getStackTrace` and `stackTrace` returns no hits in code that builds a caller-facing payload,
  and no viewtool handler places `getMessage()` output or any other exception-derived text into
  a caller-facing payload.
- **AC-005**: The `generateText(String)` and `generateText(Map)` outcome under a forced provider
  failure is verified and recorded in the plan. If the rendered page contains any exception
  text or frames, both overloads are brought under the shared handler and meet AC-001 and
  AC-002. If the page contains none, the verification evidence is recorded and no change is
  made.
- **Verification method**:
  - Integration tests added to the existing classes, which are already in `MainSuite2b`:
    `-Dit.test=CompletionsToolTest#<new failure-path methods>`,
    `-Dit.test=SearchToolTest#<new failure-path methods>` and
    `-Dit.test=AIViewToolTest#<new failure-path methods>` for `generateImage`. Failure is
    forced with a WireMock
    stub returning a 5xx, or a malformed `raw` prompt, so the exception originates below the
    viewtool. For the search `query`/`related` paths, forcing may use an unreachable provider
    port or a bad index configuration; the plan chooses the most deterministic option.
  - If the plan extracts a shared handler, a plain unit test under `dotCMS/src/test` covers it
    directly with a synthetic exception. This satisfies the issue's "unit test" wording without
    requiring container init.
  - Log assertion: capture the `Logger` output for the viewtool class during the test and assert
    one error entry containing the exception message. If capturing proves impractical in the
    integration harness, the plan must say so and fall back to asserting the log call through
    the shared handler's unit test.
  - TDD gate (Constitution Principle V): the new tests are written first, approved, and shown
    failing on `main` (they will fail because the payload contains `stackTrace`) before any
    handler is changed.
  - `generateText` verification (AC-005): on a local instance with the provider pointed at a
    closed port, render a page that calls `$ai.generateText("anything")` and capture the
    rendered HTML. Record the result in the plan. If exception text appears, add
    `generateText` failure-path tests to `AIViewToolTest` alongside the `generateImage` ones.
  - Manual: run the reproduction steps on a local instance and confirm the page shows only the
    generic error and the server log shows the trace.

## Assumptions

- **"Generic, safe error message" means a fixed string, not the sanitized exception message.**
  The acceptance criterion forbids class names in the payload. Wrapped exceptions produce
  messages such as `java.net.ConnectException: Connection refused`, and provider client
  exceptions embed upstream text (`Streaming failed: ...`). Keeping any part of the message
  cannot meet the criterion reliably. The exact wording is the plan's choice; it must not
  include any exception-derived content. Template authors who need the cause read the server
  log. The epic author can override this at spec review.
- The `error` key stays a plain string, as today, so existing `$result.error` checks are
  unaffected.
- The `related(ContentMap, String)` overload delegates to `related(Contentlet, String)` and needs
  no separate handler, only test coverage.
- `generateImage` is in scope because its handler returns exception-derived text, which the
  first acceptance criterion forbids, and because routing it through the shared handler costs
  one line once that handler exists. `generateText` is conditional because whether it leaks
  depends on the rendering layer, which this spec has not inspected.
- The `$ai` key in `toolbox.xml` is the real template name. The issue's `dotAI` wording refers
  to the product, not the key.
