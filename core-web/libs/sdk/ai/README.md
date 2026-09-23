# @dotcms/ai

Every other CMS hands an AI a *fixed menu of tools* — it can only do what the vendor pre-built. `@dotcms/ai` does the opposite: the model writes code, and the runtime runs it in a sandbox against the whole dotCMS API, with auth and policy owned in one place. The ceiling isn't a tool list; it's the API itself.

You bring whatever drives it — a model, an agent framework, an automation tool like n8n. This is the execution layer beneath them: no LLM inside, it only runs the code, safely.

It's also the layer dotCMS's own MCP server and first-party agents run on. We ship on it, not just publish it.

### Governed by construction

Safety isn't a setting you turn on; it's the shape of the runtime:

- **Your token never enters the sandbox.** Auth is injected on the host side; the executing code cannot read it.
- **Adapters are the only way out.** Sandbox code reaches the network/host *only* through an adapter you grant — direct `fetch`/`require`/`process.env` are removed.
- **You decide the surface.** An allow-list (or typed `defineAdapter` operations) bounds what any code — model-written or not — can reach. Expose `scan` and `read`; never expose `delete`. The allow-list judges the path that is actually requested, after dot-segments and encoded dots resolve, so `/allowed/../elsewhere` cannot slip past a prefix.

## Install

```bash
npm install @dotcms/ai
```

## Which SDK Version Should I Use?

dotCMS SDKs are published in lockstep with dotCMS itself: every `@dotcms/*` package ships at the **exact same version number** as the dotCMS release it was built for (e.g. dotCMS `26.7.14-1` → `@dotcms/client@26.7.14-1`, `@dotcms/react@26.7.14-1`, and so on).

**Simple rule of thumb: use the SDK version that matches your dotCMS instance's version.**

You don't have to upgrade the SDK every time dotCMS releases a new version (or vice versa). Most releases don't change anything the SDKs rely on, so an older SDK usually keeps working fine against a newer dotCMS instance. Occasionally, though, a release does include a real breaking change — and if your SDK is older than that point, it will stop working correctly.

You don't need to track this yourself: your dotCMS instance always knows the oldest SDK version it still supports, and the SDK checks itself against it automatically. If you're using an SDK that's too old, you'll see a clear warning in your console telling you to upgrade.

**Recommendation:** pin your SDKs to the same version as your dotCMS instance, and only bump them when you upgrade dotCMS — or when the console tells you to.

> **On an LTS release?** LTS releases don't currently get their own matching SDK version. Until that's addressed, use the SDK version published for the closest regular release at or before your LTS version.
>
> Want more background on how dotCMS releases and support windows work? See [Release & Support Lifecycle](https://dev.dotcms.com/docs/release-support-lifecycle).

## The front door — one runtime, two verbs

```ts
import { createRuntime } from '@dotcms/ai/runtime';

const dotcms = createRuntime({
    url,          // dotCMS instance URL
    token,        // dotCMS auth token — NEVER enters the sandbox
    allow,        // optional allow-list/policy (string[] of path prefixes, or a predicate)
    sessionId,    // context-cache + isolation key
    includeSpec,  // inject the `spec` global for the search use case
    timeout       // sandbox wall-clock timeout (ms)
});

await dotcms.request(opts);   // DIRECT — you write the call. No worker.
await dotcms.run(code);       // SANDBOXED — a model wrote `code`.
```

**The one rule that keeps the surface small:** `request` is the default. `run` is only for code you did **not** write (a model did). If you write the call yourself, you never need `run`. `run(code)` is implemented *as* "spin a worker whose `api.request` forwards to `dotcms.request`" — the two verbs share one adapter, one auth path, one allow-list, one error model, and cannot drift.

## Package topology — one package, subpaths as seams

| Subpath | Audience | Contains | Generic? |
|---|---|---|---|
| `@dotcms/ai/runtime` | Most callers — the front door | `createRuntime`, `defineAdapter`, errors | dotCMS-wired |
| `@dotcms/ai/sandbox` | Power users / custom adapters | `createSandbox`, `defineAdapter`, `Executor`, types, errors | **fully generic, lint-enforced** |
| `@dotcms/ai/adapter` | Power users | `dotcmsAdapter`, `requestCore`, context loading + cache | dotCMS-specific |
| `@dotcms/ai/spec` | The search use case | the OpenAPI spec (opt-in; keeps the ~400KB off the default path) | dotCMS-specific |
| `@dotcms/ai/tools` | Hosts building their own MCP server or agent | one factory per tool (`searchTool`, `executeTool`, `pageCreateTool`, …), and the operations behind them (`createPage`, `placeContent`, `verifyPage`, `uploadAssets`, `downloadAssets`) | dotCMS-specific, **top layer, lint-enforced** |

`@dotcms/ai` is a pure namespace — there is no bare import; everything is reached through a subpath. It is an **umbrella** for growth: future AI surfaces (RAG, embeddings, custom agents, harness) land as new subpaths under the same package.

The layers only point one way: `tools` builds on `runtime`, which builds on `adapter` and `sandbox`. Lint rules keep `sandbox` free of dotCMS code, and keep everything beneath `tools` from importing it, so a bare `@dotcms/ai/runtime` import never pulls in the tool descriptions or `node:fs`.

## Ready-made tools — `@dotcms/ai/tools`

The same seven tools dotCMS's own MCP server ships, packaged for yours. You bring the host (an MCP server, an agent loop, a workflow node); the tools bring the tuned descriptions, the validated input schemas and the sharp-edge handling behind each one.

| Factory | Tool name | What it does | Read-only | Resolves to |
|---|---|---|---|---|
| `searchTool` | `search` | Model-written JS explores the bundled OpenAPI spec | yes | `{ result: string }` |
| `executeTool` | `execute` | Model-written JS calls the dotCMS API in the sandbox | no | `{ result: string }` |
| `pageCreateTool` | `page_create` | Creates and publishes a page, creating its folder first so the URL cannot collapse | no | `CreatePageManifest` |
| `pagePlaceContentTool` | `page_place_content` | Places contentlets into page slots without wiping the slots it didn't touch | no | `PagePlaceContentManifest` |
| `pageVerifyTool` | `page_verify` | Renders a page and diagnoses empty slots, swallowed VTL errors and stale cache | yes | `VerifyPageManifest` |
| `uploadAssetsTool` | `upload_assets` | Streams a local directory into dotCMS as file assets | no | `UploadAssetsManifest` |
| `downloadAssetsTool` | `download_assets` | Streams dotCMS file assets to a local directory | no | `DownloadAssetsManifest` |

Create one **connection**, the dotCMS instance and identity the tools act as, then call a factory for each tool you want, passing the connection. Most tool packages in the AI SDK ecosystem work this way.

```ts
import { generateText } from 'ai';
import { dotcmsConnection, pageCreateTool, pageVerifyTool, searchTool } from '@dotcms/ai/tools';

const dotcms = dotcmsConnection({ url: 'https://demo.dotcms.com', token });

const result = await generateText({
    model,
    prompt: 'Create a /books page and check that it renders',
    tools: {
        search: searchTool(dotcms),
        page_create: pageCreateTool(dotcms),
        page_verify: pageVerifyTool(dotcms)
    }
});
```

Each tool is a plain object — `name`, `title`, `description`, `inputSchema` (Zod), `annotations` (MCP's read-only/destructive/idempotent/open-world hints), `execute` and `toModelOutput` — shaped the way the Vercel AI SDK and the MCP TypeScript SDK expect. Every result is an object, as Google ADK requires. The package depends on none of them.

**The connection.** You supply the URL and token; the tools never go looking for them. Either value may be a string or a resolver. A resolver is read on every call, which covers rotating tokens, secrets managers, and hosts that read their own configuration lazily:

```ts
const dotcms = dotcmsConnection({
    url: 'https://demo.dotcms.com',
    token: () => secrets.get('dotcms-token'),     // string | () => string | undefined | Promise<…>
    onCall: (event) => tracer.record(event),       // optional: fired around every request
    onContextError: (label, error) => log(label, error),
    onResolveError: (field, error) => log(`dotCMS ${field} resolver failed`, error)
});
```

A connection holds no state and makes no request. A host serving many users, such as a remote MCP server with per-user OAuth or an agent with one session per user, creates one connection per session with that user's token, and builds its tools from it.

**Tool options** describe the tool, not the caller. They go in the factory's second argument:

```ts
const tools = {
    page_create: pageCreateTool(dotcms),
    page_verify: pageVerifyTool(dotcms, { requestTimeout: 10_000 }),
    execute: executeTool(dotcms, { allow: ['/api/v1/content', '/api/content/_search'] })
};
```

You never tell a tool which endpoints to call — each tool owns that, and **enforces** it. Every fixed-purpose tool declares exactly the endpoints it calls, method and path, and any other request is refused with a `POLICY` failure before it reaches the wire. `page_create` knows it needs folders, content types and the workflow fire endpoint, so you don't have to list them and can't break it by leaving one out. A bug in `page_verify`, or a page path shaped to climb elsewhere, still can't get past the render and site endpoints. This covers `search` too: its sandbox gets the same `api` adapter as `execute`, and its allow-list (the context reads, nothing else) is what makes it read-only.

The exception is `execute`, where the *model* chooses the endpoints. `executeTool` takes `allow`, the same allow-list `createRuntime` takes, to bound what the model's code can reach. The context reads behind the `sites` / `contentTypes` / `languages` / `currentUser` globals are always permitted, `GET` only, so a narrow list never silently empties them.

| Option | Tools | Default |
|---|---|---|
| `allow` | `executeTool` | none — the model's code may call any endpoint the token allows (the other tools are always limited to their own endpoints) |
| `timeout` | `executeTool` | 45000 ms sandbox wall-clock |
| `includeStacks` | `executeTool` | false — host stack traces are withheld from the model |
| `requestTimeout` | page and asset tools | 30000 ms per request |
| `root` | asset tools — **required** | none — the local directory the model's `src` / `dest` must stay inside, symlinks resolved |

**The asset tools need a `root`.** `uploadAssetsTool` and `downloadAssetsTool` touch the local disk, with paths the model chooses. Without a boundary, a hosted server's `upload_assets` would read any directory the process can (and serve it back through dotCMS), and `download_assets` would write into any directory it can. Name the workspace the model may use:

```ts
uploadAssetsTool(dotcms, { root: '/srv/agent-workspace' })
downloadAssetsTool(dotcms, { root: '/srv/agent-workspace' })
```

Paths are compared after symlinks resolve: a link inside the root that points out of it is refused, and so is a download that would write through a symlink. `root: '/'` is the explicit whole-disk choice, for a local agent acting as its own user. The dotCMS MCP server uses it.

**`execute` never throws.** It validates the input against `inputSchema` (it comes from the model, so this is the trust boundary), runs the tool, and resolves to the result in the table above — or to a `ToolFailure`: `{ ok: false, code, retryable, error, status? }`. The `retryable` flag is there because a model cannot `instanceof` its way through a failure; it needs to be told whether trying again can help. `isToolFailure(result)` tells the two apart.

**Nothing happens at creation.** Neither `dotcmsConnection` nor a factory does any I/O; the connection is resolved and checked on each call. So a host started before its credentials exist still boots and lists its tools, and every call answers with a `CONFIGURATION` failure instead of the host crashing. That covers an empty value, a resolver returning `undefined`, and a resolver that throws. When a resolver throws, the model is told only that the connection could not be resolved. A secrets manager's error can carry paths, key names or hostnames, so the real error goes to the connection's `onResolveError` hook, and to the failure's `cause`, instead. Each call also builds a fresh runtime, so instance context (sites, content types, languages) is never stale from one call to the next.

**What the model sees.** Each tool decides how its results reach the model. `search`/`execute` declare a text form, and the rest are shown as structured JSON (so is any failure). In the AI SDK, the tool's `toModelOutput` is picked up automatically. A text-only transport such as MCP calls the tool's own `toText(result)`, which applies the same rule: code text unescaped, everything else pretty-printed JSON.

**Register each tool under its `name`.** The descriptions refer to their siblings by these names ("use the `search` tool first", "prefer `page_create`"). A tool registered under another name still works, but the model is pointed at one that does not exist.

### With the MCP TypeScript SDK

The tool object is already a valid `registerTool` config. All that's left is MCP's text envelope:

```ts
import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { dotcmsConnection, pageVerifyTool, searchTool, type DotCMSTool } from '@dotcms/ai/tools';

const dotcms = dotcmsConnection({ url, token });
const server = new McpServer({ name: 'my-server', version: '1.0.0' });
const tools: DotCMSTool[] = [searchTool(dotcms), pageVerifyTool(dotcms)];

for (const tool of tools) {
    server.registerTool(tool.name, tool, async (args) => ({
        content: [{ type: 'text', text: tool.toText(await tool.execute(args)) }]
    }));
}
```

### With Google ADK

ADK's `FunctionTool` takes the Zod schema directly as `parameters`:

```ts
import { FunctionTool, LlmAgent } from '@google/adk';
import { dotcmsConnection, pageVerifyTool, searchTool, type DotCMSTool } from '@dotcms/ai/tools';

const dotcms = dotcmsConnection({ url, token });
const tools: DotCMSTool[] = [searchTool(dotcms), pageVerifyTool(dotcms)];

const agent = new LlmAgent({
    model: 'gemini-flash-latest',
    name: 'dotcms_agent',
    instruction: 'Help authors manage their dotCMS site.',
    tools: tools.map(
        (t) =>
            new FunctionTool({
                name: t.name,
                description: t.description,
                parameters: t.inputSchema,
                execute: (input) => t.execute(input)
            })
    )
});
```

The schemas are Zod 4. Your framework and `@dotcms/ai` must resolve the **same** Zod install, which a normal install does. If you pin a second copy, TypeScript will reject `inputSchema` where the framework expects its own Zod type.

### With a framework that takes JSON Schema

The Anthropic and OpenAI SDKs, n8n and others take JSON Schema rather than Zod. Render it from the **input** side, so fields with a default stay optional. Otherwise the model is told it must send every one of them:

```ts
import { z } from 'zod';
import { dotcmsConnection, pageVerifyTool, searchTool, type DotCMSTool } from '@dotcms/ai/tools';

const dotcms = dotcmsConnection({ url, token });
const tools: DotCMSTool[] = [searchTool(dotcms), pageVerifyTool(dotcms)];
const definitions = tools.map((t) => ({
    name: t.name,
    description: t.description,
    input_schema: z.toJSONSchema(t.inputSchema, { io: 'input' })
}));

// …when the model asks for a tool:
const result = await tools.find((t) => t.name === block.name)?.execute(block.input);
```

### Choosing the surface

The surface is exactly what you import and register. Expose `search` and `page_verify` (the two read-only tools), and the model cannot create or delete anything — both are held to `GET`s by their own allow-lists, not just by their annotations. Each fixed-purpose tool can only reach its own endpoints. `execute` is the open-ended one, so bound it with `allow`, or leave it out.

### The operations behind the tools

The tools are formatting on top of typed operations, which are exported too. They are the direct path, the tool-level counterpart of `request()`: use them when **you** write the call. Typed options go in, a typed manifest comes out, and errors are thrown from the one hierarchy above.

```ts
import { createRuntime } from '@dotcms/ai/runtime';
import { createPage, placeContent, verifyPage } from '@dotcms/ai/tools';

const dotcms = createRuntime({ url, token });

const page = await createPage({ dotcms, site: 'demo.dotcms.com', urlPath: '/books', title: 'Books', template: templateId });
await placeContent({ dotcms, path: page.fullPath, site: 'demo.dotcms.com', slots: [{ slot: 1, contentlets: [bookId] }] });
const report = await verifyPage({ dotcms, path: page.fullPath, site: 'demo.dotcms.com' });
```

A runtime from `createRuntime` has no per-request deadline, unlike the one each tool call builds — pass a `signal` to `request` if you need one. `uploadAssets` and `downloadAssets` read and write the local file system, so they need Node or Bun.

## Custom, typed operations — `defineAdapter`

Instead of permitting paths on a generic `request`, expose **named operations** an LLM can call by name, with Zod-validated input and a declared output contract. This is the governed path in practice — the model sees `scan`, not `/api/**`:

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

- **`input` is mandatory** — it is the *trust* boundary (args come from model code; validate before the handler runs).
- **`output` is required for any model-facing adapter** — it is the *tool-contract* boundary (the result schema the LLM plans against; becomes the auto-generated tool definition). Use **loose/passthrough** output schemas so a new REST field doesn't break the contract. Adapters *without* `output` are typed as not model-exposable and are withheld from the auto-generated tool descriptions (`describeAdapterForLLM`).

## Error model

A single typed hierarchy, surfaced identically from `request()` and `run()` (one `requestCore`): `ValidationError`, `PolicyError`, `HttpError` (carries status + body), `NetworkError` (no response at all: refused, reset, DNS), `TimeoutError`, `AbortError`, `SandboxError`, `RuntimeError` — all subclasses of `DotCMSError`, each with a stable `code` and a serializable `toJSON()`. The model-facing string an MCP tool builds is *formatting on top of* this model — in `@dotcms/ai/tools` that formatting is the `ToolFailure` every tool resolves to on failure, which adds the `retryable` flag a model needs to decide whether to try again.

```ts
import { isDotCMSError, HttpError } from '@dotcms/ai/runtime';
try { await dotcms.request({ path: '/api/v1/site' }); }
catch (e) { if (e instanceof HttpError) console.error(e.status, e.body); }
```

## Threat model — capability confinement, NOT adversarial isolation

The governance above is **capability confinement for trusted code generators** — it stops your own model from doing something it shouldn't, not an attacker from breaking out.

- **Stops accidental egress:** `fetch`/`XMLHttpRequest`/`WebSocket`/`EventSource`/`sendBeacon` throw; `require` removed; dynamic `import()` is blocked at the source level (so `import('node:fs')`/`import('node:net')` can't re-open host access); `process.env` emptied; worker spawned with `env:{}`.
- **Stops runaway cost:** wall-clock timeout, `resourceLimits` memory/stack caps, and an `AbortSignal` threaded to adapter calls so a timeout aborts in-flight host work.
- **Does NOT stop hostile code.** User code runs via `new AsyncFunction(code)` in the same V8 isolate as the worker harness — hostile code can reach shared globals, and the `import()` block is a source-level guard (not hardened against deliberate obfuscation). The intended threat is "our own model hallucinates a `DELETE` or an infinite loop," not "an attacker submits malicious JS."

**If you must run genuinely untrusted code, bring your own process/microVM isolation.**

## Support matrix

- **Node** ≥ 20, **Bun** (native Web Workers). Both worker backends behave identically.
- **OpenAPI spec ↔ server version:** `@dotcms/ai/spec` is generated from a *specific* dotCMS instance (see "Regenerating the spec"). It is a filtered snapshot, not a live contract — regenerate it against your target server if its REST surface differs from the one you built against.
- **Semver:** subpaths are part of the public API; a breaking change to any subpath is a major.

## Regenerating the spec

`src/generated/spec.json` is **build-generated and git-ignored** — it is NOT committed. The `build`/`test`/`serve` targets run `sdk-ai:generate-spec` automatically (via `dependsOn`), so you rarely run it by hand; do so only to refresh the local copy or inspect the output.

```bash
# Defaults to https://demo.dotcms.com/api/openapi.json
pnpm nx run sdk-ai:generate-spec

# Override with a different instance (URL or local file path):
pnpm nx run sdk-ai:generate-spec -- http://localhost:8080/api/openapi.json
```

The script filters the spec to the endpoints in `ALLOWED_PREFIXES` (see `scripts/spec-transform.ts`), keeps request/response `$ref`s, and prunes `components.schemas` to just the schemas those endpoints reference. Keeping `$ref`s (rather than dereferencing) dedupes shared schemas and keeps the file small (~400KB). The output is compact JSON (machine-read only) — use `jq` to inspect it. Because the spec is regenerated at build time, there is nothing to commit.

## Commands

```bash
pnpm nx run sdk-ai:build                       # Build (ESM + CJS, dual)
pnpm nx run sdk-ai:test                        # Run tests
pnpm nx run sdk-ai:lint                        # Lint src + scripts
pnpm nx run sdk-ai:generate-spec -- <url-or-path>  # Refresh spec.json
```
