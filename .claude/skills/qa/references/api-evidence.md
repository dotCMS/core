# API issues: recorded evidence harness

For an API/backend issue, prefer driving the **UI surface** where the behavior shows up —
that's the strongest video. When no UI surface exists, don't fake one: record an
**evidence-harness page** instead, so API verification still produces watchable,
self-documenting proof.

## Pattern

1. Generate a minimal local HTML page in a temp directory (`mktemp -d` — never in the repo) that:
   - lists each check as a row: **criterion → request → expected → actual → PASS/FAIL**,
   - executes the calls with `fetch` against `https://localhost:8443` — Basic auth on
     local dev (or a short-lived JWT revoked in teardown; see environment.md § API auth);
     inject credentials at runtime, never hardcode them into the generated file,
   - renders status and response excerpts live as each call completes,
   - ends with a summary row: `RESULT: n/n PASSED`.
2. Open it in the recorded Playwright context (`page.goto('file://...')`) — the standard
   banner/pacing rules from browser-video.md apply.
3. Assert the real outcome **in the script** (from the fetch results forwarded via
   `page.evaluate` return values or `page.exposeFunction`), not by reading pixels.

## Rules

- The page runs against the same instance the provenance check validated — never demo/staging.
- Show full request lines (method + path + relevant params) on screen; truncate response
  bodies to the fields the criterion is about.
- Mask anything sensitive (tokens, cookies) — show `Bearer …<last4>` at most.
- Mutating checks follow the same seeding/naming rules as api-seeding.md
  (`qa-<issue#>-<epoch>` prefix, leave in place).
- Verify status codes AND response shape — a 200 with the wrong body is a FAIL.
- Permission-sensitive endpoints: when the criterion is about authorization, run the call
  twice — as admin and as a limited user — and show both rows.
