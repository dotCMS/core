# ai-evals Architecture

How the harness works internally. Read this when you need to extend, debug, or reason about the system's behavior under edge cases.

## Overview

The harness runs a YAML-defined task N times against a live dotCMS instance, measures how often the model succeeds, and returns structured results. The core insight: models are non-deterministic, so a single run is not meaningful. Pass rate over N runs is.

```
YAML task file
     │
     ▼
  loader.ts       parse + validate → TaskDefinition
     │
     ▼
  harness.ts      runTask() → N serial runs → TaskResult
     │
     ├── executeRun()
     │       ├── generateText()    model loop (up to 10 steps)
     │       │       └── tools.ts  search + execute
     │       ├── assertions        http_get | custom
     │       └── cleanup           best-effort
     │
     └── aggregate → pass_rate, cost, means
```

---

## Module responsibilities

| File | Owns |
|---|---|
| `main.ts` | Env validation, CLI arg parsing, task loop, summary output |
| `loader.ts` | YAML → `TaskDefinition`, basic field validation |
| `harness.ts` | `runTask`, `executeRun`, assertion runners, cleanup, cost estimation |
| `tools.ts` | `makeTools(url, token)` — the two AI SDK tools the model can call |
| `types.ts` | All TypeScript interfaces — single source of truth for data shapes |

---

## Data flow: one run

```
executeRun(index, options)
  │
  ├── makeTools(dotcmsUrl, authToken)
  │     └── returns { search, execute } — fresh tools per run
  │
  ├── generateText({ model, prompt, tools, stopWhen: stepCountIs(10) })
  │     │
  │     └── model loop:
  │           step 1: model calls search({ code })
  │                     └── sandbox Worker: runs code with spec injected
  │                         returns spec data as string
  │           step 2: model calls execute({ code })
  │                     └── sandbox Worker: runs code with api adapter
  │                         main thread executes HTTP → dotCMS
  │                         returns API response as string
  │           step N: model produces final text → finish_reason: stop
  │
  ├── runHttpGetAssertion()  →  fetch dotCMS → AssertionResult
  ├── runCustomAssertion()   →  AsyncFunction(trace, url, token, fetch) → AssertionResult
  │
  ├── runCleanup()           →  best-effort DELETE/POST (errors logged, not fatal)
  │
  └── RunResult { steps, text, finish_reason, tokens, duration, assertions, passed }
```

---

## The two tool implementations

### `search`

Injects the pre-processed OpenAPI spec (committed JSON file, loaded at startup via `getSpec()`) as a `spec` variable into a sandbox Worker. The model writes JavaScript that walks `spec.paths` to find endpoints, parameters, and request shapes.

- No network calls — pure spec exploration
- No auth needed — the spec is static data
- Timeout: 10 seconds

### `execute`

Creates a fresh `Executor` + `createApiAdapter` per call. The adapter holds `dotcmsUrl` and `authToken` in the **main thread**. The sandbox Worker never sees the token — it calls `api.request(...)` which posts a message to the main thread, which executes the actual HTTP call and posts the result back.

- Auth token isolated to main thread by design
- Timeout: 15 seconds (longer because it waits on dotCMS network)

Both tools create a fresh `Executor` per call. The `Executor` itself is cheap (plain object). The cost is the Worker thread, which is created and disposed per `executor.execute()` call. Under sequential runs this is fine. Under concurrency (N tasks in parallel), you'd have N Workers alive simultaneously — acceptable, but worth knowing.

---

## Why runs are serial within a task

Runs within a task are sequential, not parallel. The reason: runs may mutate shared dotCMS state. If run 2 starts before run 1's cleanup completes, run 2 might find leftover state (e.g. a folder that shouldn't exist yet) and produce a false result.

The unit of concurrency is the **task**, not the run. Multiple tasks can run in parallel safely because each task targets independent dotCMS resources — and that's where future parallelism should be added (currently tasks also run sequentially in `main.ts`, but the structure is ready for `Promise.all`).

---

## Assertion layer 1: behavior (`custom`)

Answers: *did the model do the right thing?*

```typescript
const fn = new Function('trace', 'url', 'token', 'fetch',
    `return (async () => { ${script} })()`
);
const result = await Promise.race([fn(...), timeout(10_000)]);
```

Key decisions:

**`new Function` not the agentic-tools sandbox.** The sandbox blocks `fetch` by design — it's the isolation guarantee for model-generated code. Custom assertion scripts are your code, trusted, and they need `fetch` for HTTP checks. Running them in the sandbox would require adding a fetch adapter and defeats the isolation guarantee. `new AsyncFunction` in the main thread is the right choice.

**`fetch` passed explicitly.** Not accessed via closure or global. This makes the available surface explicit and avoids relying on Node's global fetch being available in all environments.

**`Promise.race` with 10s timeout.** A hanging assertion fetch would stall the entire eval run indefinitely without it. The timeout rejects, the catch block records an error in `details`, the assertion fails cleanly.

**Errors are caught, not rethrown.** A syntax error or runtime error in the script fails the assertion with `details.error` set. It does not crash the run or the harness. The rest of the runs continue.

---

## Assertion layer 2: reality (`http_get`)

Answers: *did dotCMS state actually change?*

```typescript
const response = await fetch(url, { headers: { Authorization: `Bearer ${token}` } });
// always capture body regardless of status
const responseBody = await response.json().catch(() => response.text());
const passed = expect === 'ok' ? response.ok : status === expected;
return { passed, details: { status, response_body, expected } };
```

Key decision: **always capture the full response body, pass or fail.** When an assertion fails, the most common question is "what did dotCMS actually return?" Without the body, you can't tell if the model targeted the wrong path, the API rejected the payload, or the resource simply doesn't exist. The body is always in `details.response_body`.

---

## Cleanup: best-effort semantics

Cleanup runs after assertions on every run — including runs that errored before assertions ran. It never affects pass/fail. If a cleanup DELETE returns 404 (e.g. the task already cleaned up after itself), that's logged and ignored.

This is intentional. Consider:
- Run 1 creates a folder, assertion passes, cleanup deletes it ✓
- Run 2 creates a folder, assertion passes, cleanup tries to delete it → 404 because dotCMS already processed a second delete → should not mark run 2 as failed

Cleanup is infrastructure, not correctness. Don't fail on cleanup errors.

---

## Cost tracking

```typescript
estimated_cost_usd =
    (total_input_tokens  / 1_000_000) * input_per_mtok  +
    (total_output_tokens / 1_000_000) * output_per_mtok
```

Pricing constants are stored alongside the result in `cost_model_assumptions`:

```typescript
{ model_id, input_per_mtok, output_per_mtok }
```

This means historical results can be recomputed if pricing changes, and you always know which rates were used at run time. Update `COST_ASSUMPTIONS` in `harness.ts` when you change models or when Anthropic changes pricing.

---

## Failure modes and how they're handled

| What fails | Behavior |
|---|---|
| `generateText` throws (network, rate limit) | Run recorded as failed with `error` field. Cleanup still runs. Other runs continue. |
| Assertion script throws | `AssertionResult.passed = false`, error in `details.error`. Run continues to next assertion. |
| Assertion script hangs >10s | `Promise.race` rejects with timeout error. Same as above. |
| `http_get` fetch fails (network) | `AssertionResult.passed = false`, error in `details.error`. |
| Cleanup step fails | Logged to stderr with `(ignored)`. Run result unaffected. |
| Missing env var | `process.exit(1)` at startup, before any task runs. |
| YAML parse error | `loadTask()` throws. `main.ts` catches and exits. |

---

## Extending the harness

### Adding a new assertion type

1. Add the interface to `types.ts` and extend `TaskAssertion`
2. Add a runner function in `harness.ts` following the `runHttpGetAssertion` pattern — always return `AssertionResult`, never throw
3. Add the dispatch case in `executeRun()`

### Adding task-level parallelism

In `main.ts`, replace the `for` loop with `Promise.all(tasks.map(...))`. Each task is already self-contained. The only shared state is stdout — interleaved output will need buffering or per-task log files.

### Storing results

`runTask()` returns a fully typed `TaskResult`. Pipe it to a JSON file, a database, or a reporting tool — the harness doesn't own that concern.
