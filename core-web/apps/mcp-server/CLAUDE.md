# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Essential Commands

### Development Commands
```bash
# Build the MCP server for prod
yarn nx build mcp-server

# Development mode with hot reload
yarn nx serve mcp-server

# Regenerate spec (requires a URL or local file path)
yarn nx generate-spec mcp-server -- https://demo.dotcms.com/api/openapi.json

# Run tests
yarn nx test mcp-server

# Run linting
yarn nx lint mcp-server
```

### Environment Setup
The MCP server requires two environment variables:
```bash
export DOTCMS_BASE_URL="https://your-dotcms-instance.com"
export DOTCMS_API_TOKEN="your-api-token"
```

Optional:
```bash
export SANDBOX_TIMEOUT=15000  # Sandbox execution timeout in ms (default: 15000)
```

#### Configuration example:

```json
{
  "mcpServers": {
    "dotcms": {
      "command": "node",
      "args": [
        "/Users/fmontes/Developer/dotcms/core/core-web/dist/apps/mcp-server/stdio.js"
      ],
      "env": {
        "DOTCMS_BASE_URL": "http://localhost:8080",
        "DOTCMS_API_TOKEN": "YOUR_DOTCMS_API_TOKEN"
      }
    }
  }
}
```

## Architecture Overview

This is a **Model Context Protocol (MCP) server** for dotCMS, built with [xmcp](https://xmcp.dev) (rspack-based framework). The server provides AI assistants with two tools to interact with dotCMS: **search** (explore API spec) and **execute** (make API calls).

### Core Architecture

**Framework**: xmcp auto-discovers tools from `src/tools/`. Each tool exports `schema`, `metadata`, and a default handler function.

**Entry Point**: xmcp generates the entry point at build time (`dist/stdio.js`).

**Build Pipeline**:
1. `generate-spec` — fetches the OpenAPI spec from a dotCMS instance, processes it into `src/generated/spec.json` (dereferences $refs, filters to relevant endpoints)
2. `xmcp build` — bundles everything with rspack into `dist/`

**Tool Layer** (`src/tools/`):
- `search.ts` — Explore the OpenAPI spec via sandbox-executed JavaScript
- `execute.ts` — Make authenticated API calls via sandbox-executed JavaScript

**Library Layer** (`src/lib/`):
- `executor.ts` — Orchestrates sandbox creation and adapter injection
- `http-client.ts` — Authenticated HTTP adapter (tokens injected by main thread, never in sandbox)
- `spec.ts` — Loads the pre-processed OpenAPI spec
- `types.ts` — TypeScript interfaces for adapters, sandbox config, execution context
- `sandbox/index.ts` — Runtime detection factory (`createSandbox`)
- `sandbox/interface.ts` — Sandbox interface definition
- `sandbox/bun-worker.ts` — Bun Web Worker sandbox
- `sandbox/node-worker.ts` — Node.js `worker_threads` sandbox

### Key Patterns

**Dual-Runtime Sandbox**: User code runs in Workers with automatic runtime detection. On Bun, uses native Web Workers (`Blob` + `URL.createObjectURL`). On Node.js, uses `worker_threads` (`new Worker(code, { eval: true })`). Runtime is detected via `typeof globalThis.Bun`. API tokens are never exposed to the sandbox — the main thread handles authentication via the adapter pattern.

**Adapter Pattern**: The executor bridges sandbox code to the main thread:
- Sandbox calls `api.request(...)` which posts a message to the main thread
- Main thread executes the actual HTTP call with injected auth
- Result is posted back to the sandbox

**Build-time Spec Processing**: `scripts/generate-spec.ts` fetches the OpenAPI spec from a URL (or reads a local file), dereferences it, filters to allowed endpoint prefixes, strips response schemas, and handles circular references. The developer must provide the spec URL or file path when running `generate-spec`.

### Type System

All interfaces are in `src/lib/types.ts`:
- `Adapter` / `AdapterMethod` — for extending sandbox capabilities
- `SandboxConfig` / `SandboxResult` — sandbox execution parameters and results
- `ExecutionContext` — adapters + variables passed to sandbox

## MCP Tools Available

### Search Tool
**Purpose**: Explore the dotCMS REST API specification
**Sandbox globals**: `spec` (the dereferenced OpenAPI spec object)
**Read-only**: Yes — no side effects

### Execute Tool
**Purpose**: Make authenticated API calls to dotCMS
**Sandbox globals**: `api` (HTTP adapter), `pick`, `table`, `count`, `sum`, `first` (helpers)
**Side effects**: Yes — can create/modify/delete content

## Development Guidelines

### Adding New Tools
1. Create a new `.ts` file in `src/tools/`
2. Export `schema` (Zod object), `metadata` (ToolMetadata), and a default handler
3. xmcp auto-discovers it — no registration needed

### Adding New Adapters
1. Implement the `Adapter` interface from `src/lib/types.ts`
2. Register it on the executor in the tool handler
3. Specify adapter names in `execute()` options

### Testing
- Tests use Jest with Node.js environment
- Test files should follow `*.spec.ts` naming convention
- `generate-spec` target runs before tests (spec.json must exist)

## Configuration

The server is configured as an Nx application with:
- **Build**: `nx:run-commands` executor running `xmcp build`
- **Dev**: `nx:run-commands` running `xmcp dev` (hot reload)
- **Test**: `@nx/jest:jest` executor
- **Lint**: `@nx/eslint:lint` executor
- **Output**: `dist/apps/mcp-server/stdio.js`
