# ai-evals

Evaluation harness for the dotCMS agentic tools. Runs AI tasks against a live dotCMS instance and measures how reliably the model completes them.

## Setup

### 1. Environment variables

Copy `.env.example` to `.env` in `core-web/` and fill in the values:

```bash
cp core-web/.env.example core-web/.env
```

| Variable | Required | Description |
|---|---|---|
| `DOTCMS_URL` | Yes | Base URL of your dotCMS instance (e.g. `https://demo.dotcms.com`) |
| `AUTH_TOKEN` | Yes | dotCMS API token (generate in dotCMS → User Tools → API Tokens) |
| `ANTHROPIC_API_KEY` | Yes | Anthropic API key |

All three are validated at startup. Missing any one exits immediately with a clear error.

### 2. Run

```bash
# Run all tasks in apps/ai-evals/tasks/
yarn nx run ai-evals:smoke

# Run a single task file
yarn nx run ai-evals:smoke -- apps/ai-evals/tasks/count-workflow-schemes.yaml

# Verbose — print tool input/output for each step
VERBOSE=1 yarn nx run ai-evals:smoke

# Verbose + single task
VERBOSE=1 yarn nx run ai-evals:smoke -- apps/ai-evals/tasks/count-workflow-schemes.yaml
```

> `--verbose` is not supported directly because Yarn 1.x strips `--` before forwarding args to Nx. Use `VERBOSE=1` instead.

---

## Reading the output

```
── count-workflow-schemes (5 runs) ──
   Count workflow schemes - tests multi-step (discover endpoint, call it)
  run 1/5...
    step 1: [search]
    step 2: [execute]
    step 3: [—]
    [PASS] response-has-number
  run 2/5...
  ...

   Result: 5/5 (100%) | $0.0021

=== Summary ===
[PASS] count-workflow-schemes: 5/5 (100%)
[FAIL] create-folder-at-root: 3/5 (60%)
```

- **Steps** — what the model did on each step (`[search]`, `[execute]`, or `[—]` for the final text step)
- **Assertions** — `[PASS]` / `[FAIL]` per assertion, with error details on failure
- **Result line** — pass count, pass rate, estimated cost for all runs of that task
- **Summary** — one line per task; exit code 1 if any task fails

---

## Writing tasks

Tasks live in `apps/ai-evals/tasks/`. One `.yaml` file per task.

### Full schema

```yaml
id: my-task-id              # unique, kebab-case
category: discovery         # discovery | content | workflow
description: One line describing what this tests
difficulty: easy            # easy | medium | hard
runs: 5                     # how many times to run (recommend 5-7)

prompt: |
  The prompt sent to the model. Be specific about what you want.

assertions:
  - type: http_get           # check dotCMS state via GET
    path: /api/v1/some/path
    expect: ok               # 'ok' = any 2xx, or an exact status code like 404

  - type: custom             # arbitrary JS check
    name: my-assertion-name  # human-readable label shown in output
    script: |
      // Has access to: trace, url, token, fetch
      // Return true to pass, false to fail
      return trace.text.includes('something');

cleanup:                     # optional — runs after assertions, best-effort
  - method: DELETE
    path: /api/v1/some/path
  - method: POST
    path: /api/v1/some/other
    body: { key: value }

tags: [discovery, read-only]           # optional, for filtering later
requires_fixture: Starter site data    # optional, documents prerequisites
```

### Assertion types

#### `http_get` — verify dotCMS state changed

Makes a real authenticated GET to dotCMS after the run completes. Use this to confirm the model actually created/modified what it was asked to.

```yaml
- type: http_get
  path: /api/v1/folder/byPath/my-folder
  expect: ok
```

`expect: ok` passes for any 2xx status. Use a number (e.g. `expect: 404`) to assert a specific status.

On failure, the full response body is captured in the trace so you can see exactly what dotCMS returned.

#### `custom` — check behavior or complex state

Inline JavaScript evaluated after the run. Gets four globals:

| Variable | Type | Description |
|---|---|---|
| `trace` | `RunResult` | The completed run: `trace.text`, `trace.steps`, `trace.finish_reason` |
| `url` | `string` | `DOTCMS_URL` from env |
| `token` | `string` | `AUTH_TOKEN` from env |
| `fetch` | `function` | Node.js native fetch |

Return `true` to pass, `false` to fail. Errors are caught and recorded as failures.

```yaml
- type: custom
  name: response-mentions-a-number
  script: |
    return /\b\d+\b/.test(trace.text);
```

```yaml
- type: custom
  name: content-type-has-title-field
  script: |
    const r = await fetch(new URL('/api/v1/contenttype/id/evalTest', url), {
      headers: { Authorization: `Bearer ${token}` }
    });
    if (!r.ok) return false;
    const data = await r.json();
    const fields = data?.entity?.fields ?? [];
    return fields.some((f) => f.variable?.toLowerCase() === 'title');
```

Custom scripts have a **10-second timeout**. Hanging fetches will be killed and recorded as failures.

### Cleanup

Cleanup steps run after assertions on every run, regardless of pass/fail. They're best-effort — if a cleanup call fails (e.g. the folder was already deleted), the error is logged but the run result is not affected.

```yaml
cleanup:
  - method: DELETE
    path: /api/v1/folder/byPath/eval-test-folder
```

### Choosing `runs`

| Difficulty | Recommended runs | Why |
|---|---|---|
| `easy` | 5 | Stable tasks — 5 is enough to catch flakiness |
| `medium` | 5–7 | More complex prompts benefit from a larger sample |
| `hard` | 7 | Multi-step tasks with more failure modes |

Pass rate is the result, not pass/fail. A task passing 4/5 is not the same as 5/5 — that's a flaky prompt worth improving.

---

## Example tasks

### Read-only discovery

```yaml
id: count-workflow-schemes
category: discovery
difficulty: easy
runs: 5
prompt: |
  How many workflow schemes exist in this dotCMS instance?
assertions:
  - type: custom
    name: response-has-number
    script: |
      return /\b\d+\b/.test(trace.text);
tags: [discovery, read-only]
```

### Write + verify

```yaml
id: create-folder-at-root
category: content
difficulty: easy
runs: 5
prompt: |
  Create a folder named "eval-test-folder" at the root of the default site.
assertions:
  - type: http_get
    path: /api/v1/folder/byPath/eval-test-folder
    expect: ok
cleanup:
  - method: DELETE
    path: /api/v1/folder/byPath/eval-test-folder
tags: [content, create]
```

---

## Project structure

```
apps/ai-evals/
├── src/
│   ├── main.ts       # Entry point: env validation, task loop, summary
│   ├── harness.ts    # runTask(), assertion runners, cleanup
│   ├── loader.ts     # YAML → TaskDefinition parser
│   ├── tools.ts      # makeTools(url, token) — search + execute AI tools
│   └── types.ts      # All TypeScript interfaces
├── tasks/            # Task YAML files — add yours here
│   ├── count-workflow-schemes.yaml
│   └── create-folder-at-root.yaml
└── README.md
```
