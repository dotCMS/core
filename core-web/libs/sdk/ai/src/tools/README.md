# `@dotcms/ai/tools` — what goes where

This folder has three layers. Each has one job, and imports only point down. Lint enforces the direction.

```
tools/
├── index.ts          public surface — the only file consumers import from
├── definitions/      THE TOOLS — what the model sees
├── operations/       the dotCMS work behind them — what actually happens
│   └── shared/       helpers only operations use
└── toolkit/          how any tool is built and run — no dotCMS operation knowledge
```

```
definitions  ──►  operations  ──►  toolkit
     └───────────────────────────────►┘
```

## `definitions/` — the tools

One file per model-facing tool, named after it (`page-create.ts` is the `page_create` tool). A definition holds everything the model reads, plus the smallest possible handler:

- `name`, `title`, `description` — the description is tuned prompt text, not documentation
- `inputSchema` — the Zod schema, with a `.describe()` on every field (the model reads those too)
- `annotations` — MCP's read-only / destructive / idempotent / open-world hints
- `endpoints` — imported from the operation (see below), except for the two code tools, which have none
- `handler` — maps the validated input onto one operation call and returns its result
- the exported factory (`pageCreateTool(connection, options)`): the connection first, then the tool's own options

No logic lives here. If a handler starts branching, parsing responses or calling `request` itself, that code belongs in an operation.

`tools.spec.ts` tests the tools the way a host uses them: factories, credentials, validation failures, policies, results.

## `operations/` — the dotCMS work

The direct layer, exported for consumers who write their own calls: `createPage`, `placeContent`, `verifyPage`, `uploadAssets`, `downloadAssets`. An operation takes typed options (including a `dotcms: DotCMSRuntime`), returns a typed manifest, and **throws** typed errors. Formatting a failure for a model is the toolkit's job, not the operation's. Input the caller got wrong (a relative path, an unknown site, a slot that isn't on the page) throws `ValidationError`, so the model is told `VALIDATION` and not to retry. A plain `Error` is for a response dotCMS should never have sent, and reaches the model as `UNKNOWN`. When you add context to an error you caught, keep its class: that class is what tells the model whether a retry can help.

Each operation also exports its **endpoint list** (`PAGE_CREATE_ENDPOINTS`, …). That's every method + path it calls, declared right next to the requests. The tool enforces that list at runtime: anything else is refused before it reaches the network. The operation's spec records every request its fake runtime sees and fails if one isn't on the list. So a new request means a new entry, or the build goes red.

`shared/` holds what several operations need and no tool calls directly: site/language resolution (`resolve.ts`, with its own `RESOLVE_ENDPOINTS`), page-path normalization, page response shapes, the asset operations' glob filter (`glob.ts`) and manifest shapes (`asset-common.ts`), and the local-filesystem boundary they enforce (`local-root.ts`: paths compared after symlinks resolve, checked before anything is created).

## `toolkit/` — how a tool is built and run

Generic machinery with no knowledge of any specific dotCMS operation:

| File | What it owns |
|---|---|
| `types.ts` | The public `DotCMSTool` shape (input typed from its schema), `AnyDotCMSTool` for lists of mixed tools, and the factory option types |
| `connection.ts` | `dotcmsConnection`: the URL, token and observability hooks a consumer hands every tool, with values resolved on each call. The tools never read the environment (lint-enforced) |
| `create-tool.ts` | `defineTool` / `createTool`: input validation, resolving the connection, turning throws into failures, building the per-tool policy |
| `results.ts` | The `{ result }` shape of the code tools, and the one rendering rule behind every tool's `toModelOutput` (AI SDK) and `toText` (MCP and other text transports): text for a tool whose definition declares `toText`, JSON otherwise |
| `tool-runtime.ts` | The runtime one call runs on (fresh per call, request deadline), and the `ToolFailure` envelope |
| `endpoints.ts` | The endpoint pattern language, the policies built from it, and `CONTEXT_ENDPOINTS` (the one list the toolkit owns, because the runtime makes those reads for every tool) |
| `lenient-boolean.ts` | Zod helpers for input schemas |

## Where does my code go?

| You're writing… | Put it in |
|---|---|
| Text the model reads (a description, a field hint) | `definitions/<tool>.ts` |
| A request to dotCMS, or parsing its response | `operations/<operation>.ts` |
| Something two operations both need | `operations/shared/` |
| Something every tool needs, unrelated to what the tool does | `toolkit/` |
| A new public export | `index.ts` |

## Adding a tool

1. **Operation.** Add `operations/<name>.ts`: the typed function plus its `<NAME>_ENDPOINTS`. Add `operations/<name>.spec.ts` with a fake runtime, and copy the `seen` + `afterEach(unlistedCalls(...))` guard from `page-verify.spec.ts`.
2. **Definition.** Add `definitions/<name>.ts`: `defineTool({ name, title, description, inputSchema, annotations, endpoints, handler })` and the factory that calls `createTool(definition, connection, options)`. Add the factory to `FACTORIES` in `definitions/tools.spec.ts`.
3. **Export** the factory and the operation from `index.ts`.
4. **Host it** in the MCP server (`apps/mcp-server/src/tools/<tool_name>.ts`). See `apps/mcp-server/CONTRIBUTING.md`.

## The two code tools: `search` and `execute`

These two live only in `definitions/`, and have no operation, on purpose. The model writes the code, and running it is already a public verb, `createRuntime(...).run(code)` in `@dotcms/ai/runtime`, so the runtime is their direct layer. An operation here would only wrap `run()` a second time. Their handlers do tool work: pick the runtime settings (a short timeout and the `spec` global for `search`, the configured timeout for `execute`) and cap the result for the model's context.

With no operation to hold it, their endpoint list sits in the definition:

- **`search`** allows the context reads only (`CONTEXT_ENDPOINTS`). Its sandbox gets the same `api` adapter as `execute`, so this list is what makes it read-only.
- **`execute`** declares `endpoints: 'model-chosen'`, because the model picks the requests. The consumer bounds them with `allow`.

Every other tool owns its endpoints, and consumers never configure them.
