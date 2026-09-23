# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Essential Commands

### Development Commands
```bash
# Build the MCP server (regenerates the OpenAPI spec first, from demo.dotcms.com by default)
pnpm nx build mcp-server

# Development mode with hot reload
pnpm nx serve mcp-server

# Regenerate the spec only (URL or local file path; defaults to demo.dotcms.com)
pnpm nx run sdk-ai:generate-spec -- https://demo.dotcms.com/api/openapi.json

# Run tests (builds first, then boots the bundle — see Testing)
pnpm nx test mcp-server

# Run linting
pnpm nx lint mcp-server
```

### Environment Setup

Configure the MCP server via the `env` block in your MCP client config:

```json
{
  "mcpServers": {
    "dotcms": {
      "command": "node",
      "args": [
        "/Users/fmontes/Developer/dotcms/core/core-web/dist/apps/mcp-server/stdio.js"
      ],
      "env": {
        "DOTCMS_URL": "http://localhost:8080",
        "AUTH_TOKEN": "eyJ0eXAiOiJKV1Qi..."
      }
    }
  }
}
```

| Variable | Required | Description |
|---|---|---|
| `DOTCMS_URL` | Yes | Base URL of the dotCMS instance |
| `AUTH_TOKEN` | Yes | JWT Bearer token (generate in dotCMS → User Tools → API Tokens) |
| `SANDBOX_TIMEOUT` | No | Sandbox execution timeout in ms for `execute` (default: `45000`) |

## Architecture Overview

This is a **Model Context Protocol (MCP) server** for dotCMS, built with [xmcp](https://xmcp.dev) (rspack-based framework). It is a thin host: every tool — its description, input schema, annotations and behavior — comes from **`@dotcms/ai/tools`** (`libs/sdk/ai/src/tools/`), the same tool set any consumer can register in their own MCP server or agent. This app only reads the environment and adapts each tool to xmcp's file convention.

### Core Architecture

**Framework**: xmcp auto-discovers tools from `src/tools/`. Each tool exports `schema`, `metadata`, and a default handler function.

**Entry Point**: xmcp generates the entry point at build time (`dist/stdio.js`).

**Build Pipeline**:
1. `sdk-ai:generate-spec` (via `dependsOn`) — fetches the OpenAPI spec from a dotCMS instance and processes it into `libs/sdk/ai/src/generated/spec.json`
2. `xmcp build` — bundles everything with rspack into `dist/apps/mcp-server/`

**Tool Layer** (`src/tools/`) — one three-line file per tool, all shaped the same:

```ts
const { schema, metadata, handler } = xmcpTool(pageVerifyTool);
export { schema, metadata };
export default handler;
```

**Wiring** (`src/lib/tools.ts`) — `xmcpTool(factory)` calls a `@dotcms/ai/tools` factory with this server's options (`SANDBOX_TIMEOUT`, context-error logging) and maps the tool onto xmcp's exports, turning a manifest or failure into the JSON text the server has always returned. It passes no `url` / `token`: the tools read `DOTCMS_URL` / `AUTH_TOKEN` themselves, on each call, so a server started without credentials still boots and answers every call with a `CONFIGURATION` failure.

**Tool logic** — lives in `@dotcms/ai`, not here:
- `libs/sdk/ai/src/tools/definitions/` — each tool's name, description, Zod input and handler
- `libs/sdk/ai/src/tools/*.ts` — the operations behind them (`page-create`, `page-place-content`, `page-verify`, `assets-transfer`, `resolve`, …) and `tool-runtime.ts` (per-call runtime, request deadline, the `ToolFailure` envelope)
- `libs/sdk/ai/src/runtime.ts`, `sandbox/`, `adapter/` — the runtime, sandbox and dotCMS adapter everything runs on

See the [`@dotcms/ai` README](../../libs/sdk/ai/README.md) for the layering rules.

## MCP Tools Available

`search`, `execute`, `page_create`, `page_place_content`, `page_verify`, `upload_assets`, `download_assets` — see the table in the [`@dotcms/ai` README](../../libs/sdk/ai/README.md#ready-made-tools--dotcmsaitools).

## Development Guidelines

### Adding New Tools
1. Add the definition to `libs/sdk/ai/src/tools/definitions/` with `defineTool({ name, title, description, inputSchema, annotations, endpoints, handler })` plus a factory that calls `createTool(definition, options)`, and put any logic worth testing on its own in a sibling operation module under `libs/sdk/ai/src/tools/`. `endpoints` is enforced — list every method + path the operation calls, and guard the operation's spec with `unlistedCalls` (see `page-verify.spec.ts`)
2. Export the factory (and the operation, if it has one) from `libs/sdk/ai/src/tools/index.ts`, and add it to `FACTORIES` in `create-tools.spec.ts`
3. Add `src/tools/<tool_name>.ts` here — the three lines above, with your factory
4. Add the name to the expected list in `src/smoke/server-boot.spec.ts`

### Adding New Adapters
Sandbox adapters (extra globals for `execute` code) are built with `defineAdapter` from `@dotcms/ai/sandbox` — see the [`@dotcms/ai` README](../../libs/sdk/ai/README.md#custom-typed-operations--defineadapter).

### Testing
- Tests use Vitest with the Node environment, `*.spec.ts` naming
- Tests for tool logic live next to it in `libs/sdk/ai/src/tools/` (`pnpm nx test sdk-ai`)
- This app keeps only `src/smoke/server-boot.spec.ts`, which boots the BUILT bundle over stdio — the `test` target depends on `build` for that reason
- Never put a spec file under `src/tools/`: xmcp loads every module there as a tool, and the build fails on one (`scripts/check-tools-dir.mjs`)

## Configuration

The server is configured as an Nx application with:
- **Build**: `nx:run-commands` executor running `xmcp build`
- **Dev**: `nx:run-commands` running `xmcp dev` (hot reload)
- **Test**: Vitest (`vite.config.mts`)
- **Lint**: `@nx/eslint:lint` executor
- **Output**: `dist/apps/mcp-server/stdio.js`
