# @dotcms/ai

The dotCMS **agentic runtime** — run model-written or human-written code safely against a
dotCMS instance, with auth and policy owned in one place.

## Install

```bash
npm install @dotcms/ai
```

## The front door — one runtime, two verbs

```ts
import { createRuntime } from '@dotcms/ai/runtime';

const dotcms = createRuntime({
    url,          // dotCMS instance URL
    token,        // server-side token — NEVER enters the sandbox
    allow,        // optional allow-list/policy (string[] of path prefixes, or a predicate)
    sessionId,    // context-cache + isolation key
    includeSpec,  // inject the `spec` global for the search use case
    timeout       // sandbox wall-clock timeout (ms)
});

await dotcms.request(opts);   // DIRECT — you write the call. No worker.
await dotcms.run(code);       // SANDBOXED — a model wrote `code`.
```

**The one rule that keeps the surface small:** `request` is the default. `run` is only for
code you did **not** write (a model did). If you write the call yourself, you never need
`run`. `run(code)` is implemented *as* "spin a worker whose `api.request` forwards to
`dotcms.request`" — the two verbs share one adapter, one auth path, one allow-list, one
error model, and cannot drift.

## Package topology — one package, subpaths as seams

| Subpath | Audience | Contains | Generic? |
|---|---|---|---|
| `@dotcms/ai/runtime` | Most callers — the front door | `createRuntime`, `defineAdapter`, errors | dotCMS-wired |
| `@dotcms/ai/sandbox` | Power users / custom adapters | `createSandbox`, `defineAdapter`, `Executor`, types, errors | **fully generic, lint-enforced** |
| `@dotcms/ai/adapter` | Power users | `dotcmsAdapter`, `requestCore`, context loading + cache | dotCMS-specific |
| `@dotcms/ai/spec` | The search use case | the OpenAPI spec (opt-in; keeps the ~550KB off the default path) | dotCMS-specific |

`@dotcms/ai` is a pure namespace — there is no bare import; everything is reached through a
subpath. It is an **umbrella** for growth: future AI surfaces (RAG, embeddings, …) land as
new subpaths under the same package.

## Custom, typed operations — `defineAdapter`

Instead of permitting paths on a generic `request`, expose **named operations** an LLM can
call by name, with Zod-validated input and a declared output contract:

```ts
import { defineAdapter, createSandbox } from '@dotcms/ai/sandbox';
import { z } from 'zod';

const a11y = defineAdapter({
    name: 'a11y',
    methods: {
        scan: {
            description: 'Scan a page URL; returns axe findings',
            input:  z.object({ url: z.string().url() }),
            output: z.object({ findings: z.object({ violations: z.array(z.any()) }).loose() }),
            handler: ({ url }, { request }) =>
                request({ method: 'POST', path: '/api/v1/page-scanner/a11y/check', body: { url } })
        }
    }
});

const sandbox = createSandbox({
    adapters: [a11y],
    timeout: 120_000,
    request: (opts) => dotcms.request(opts) // host capability; the runtime provides one
});
await sandbox.run(`return (await a11y.scan({ url: 'https://demo.dotcms.com/' })).findings.violations;`);
```

- **`input` is mandatory** — it is the *trust* boundary (args come from model code; validate
  before the handler runs).
- **`output` is required for any model-facing adapter** — it is the *tool-contract* boundary
  (the result schema the LLM plans against; becomes the auto-generated tool definition). Use
  **loose/passthrough** output schemas so a new REST field doesn't break the contract.
  Adapters *without* `output` are typed as not model-exposable and are withheld from the
  auto-generated tool descriptions (`describeAdapterForLLM`).

## Error model

A single typed hierarchy, surfaced identically from `request()` and `run()` (one
`requestCore`): `ValidationError`, `PolicyError`, `HttpError` (carries status + body),
`TimeoutError`, `AbortError`, `SandboxError`, `RuntimeError` — all subclasses of
`DotCMSError`, each with a stable `code` and a serializable `toJSON()`. The model-facing
string an MCP tool builds is *formatting on top of* this model.

```ts
import { isDotCMSError, HttpError } from '@dotcms/ai/runtime';
try { await dotcms.request({ path: '/api/v1/site' }); }
catch (e) { if (e instanceof HttpError) console.error(e.status, e.body); }
```

## Threat model — capability confinement, NOT adversarial isolation

The sandbox blocks **accidental egress** (`fetch`/`XMLHttpRequest`/`WebSocket`/`EventSource`/
`sendBeacon` throw; `require` removed; `process.env` emptied; worker spawned with `env:{}`)
and **runaway cost** (wall-clock timeout, `resourceLimits` memory/stack caps, an
`AbortSignal` threaded to adapter calls so a timeout aborts in-flight host work).

It is **capability confinement for trusted code generators, NOT a defense against
adversarial code.** User code runs via `new AsyncFunction(code)` in the same V8 isolate as
the worker harness — hostile code can reach shared globals. The intended threat is "our own
model hallucinates a `DELETE` or an infinite loop," not "an attacker submits malicious JS."
**If you must run genuinely untrusted code, bring your own process/microVM isolation.**

## Support matrix

- **Node** ≥ 20, **Bun** (native Web Workers). Both worker backends behave identically.
- **OpenAPI spec ↔ server version:** `@dotcms/ai/spec` is generated from a *specific* dotCMS
  instance (see "Regenerating the spec"). It is a filtered snapshot, not a live contract —
  regenerate it against your target server if its REST surface differs from the committed one.
- **Semver:** subpaths are part of the public API; a breaking change to any subpath is a major.

## Regenerating the spec

The committed `src/generated/spec.json` is build-time data — consumers do not need a live
instance to build or run.

```bash
# Defaults to https://demo.dotcms.com/api/openapi.json
pnpm nx run sdk-ai:generate-spec

# Override with a different instance:
pnpm nx run sdk-ai:generate-spec -- http://localhost:8080/api/openapi.json
```

Then **commit the updated `src/generated/spec.json`**. The script filters the spec to the
endpoints in `ALLOWED_PREFIXES` (see `scripts/generate-spec.ts`), dereferences `$ref`s, and
strips response schemas to keep the file small.

## Commands

```bash
pnpm nx run sdk-ai:build                       # Build (ESM + CJS, dual)
pnpm nx run sdk-ai:test                        # Run tests
pnpm nx run sdk-ai:lint                        # Lint src + scripts
pnpm nx run sdk-ai:generate-spec -- <url-or-path>  # Refresh spec.json
```
