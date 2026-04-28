# @dotcms/agentic-tools

Portable runtime primitives for building AI agents that interact with dotCMS. Used by `apps/mcp-server` and `apps/ai-evals` — any future consumer that needs sandbox execution or authenticated dotCMS API calls.

## What's in here

| Module | Export | Purpose |
|---|---|---|
| `executor` | `createExecutor`, `Executor` | Orchestrates sandbox + adapters |
| `http-client` | `createApiAdapter` | Authenticated HTTP adapter for dotCMS |
| `sandbox` | `createSandbox` | Dual-runtime worker (Node.js / Bun) |
| `spec` | `getSpec` | Loads the pre-processed OpenAPI spec |
| `types` | various interfaces | Shared TypeScript types |

## Architecture

```
Consumer (mcp-server, ai-evals, ...)
    └── createExecutor()
            ├── createSandbox()          ← worker thread (Node.js or Bun)
            └── createApiAdapter(config) ← auth injected here, never in sandbox
```

**Sandbox isolation**: user code runs in a Worker. It can call `api.request(...)` which posts a message to the main thread. The main thread executes the actual HTTP call with the injected auth token and posts the result back. Auth tokens are never sent into the sandbox.

**Dual-runtime**: `createSandbox()` auto-detects the runtime via `typeof globalThis.Bun` and picks the right worker implementation.

## Usage

```typescript
import { createExecutor, createApiAdapter, getSpec } from '@dotcms/agentic-tools';

const apiAdapter = createApiAdapter({
    dotcmsUrl: 'https://demo.dotcms.com',
    authToken: 'your-api-token'
});

const executor = createExecutor();
executor.registerAdapter(apiAdapter);

// Run sandboxed code with access to the api adapter
const result = await executor.execute(`
    const data = await api.request({ path: '/api/v1/contenttype' });
    return data;
`, {
    adapters: ['api'],
    sandbox: { timeout: 10000 }
});
```

## OpenAPI Spec

`getSpec()` returns the pre-processed dotCMS OpenAPI spec committed at `src/generated/spec.json`.

**To refresh the spec** (requires a running dotCMS instance):

```bash
# Defaults to https://demo.dotcms.com/api/openapi.json when no arg is passed
yarn nx run agentic-tools:generate-spec

# Override with a different instance:
yarn nx run agentic-tools:generate-spec -- http://localhost:8080/api/openapi.json
```

Then **commit the updated `src/generated/spec.json`**. The spec is static build-time data — consumers do not need a live dotCMS instance to build or run.

The script filters the full OpenAPI spec down to the endpoints in `ALLOWED_PREFIXES` (see `scripts/generate-spec.ts`), dereferences all `$ref` pointers, and strips response schemas to keep the file small.

## Commands

```bash
yarn nx run agentic-tools:build          # Compile TypeScript
yarn nx run agentic-tools:test           # Run tests
yarn nx run agentic-tools:lint           # Lint src + scripts
yarn nx run agentic-tools:generate-spec -- <url-or-path>  # Refresh spec.json
```
