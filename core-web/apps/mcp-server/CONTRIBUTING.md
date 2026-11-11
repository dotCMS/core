## Adding New Tools

### Quick Start

Tools are written once, in [`@dotcms/ai/tools`](../../libs/sdk/ai/README.md#ready-made-tools--dotcmsaitools), so every host — this server, a consumer's own MCP server, an agent — gets the same tool. This app only hosts them.

1. **Build the tool in the SDK.** The operation, its endpoint list, the definition and the export are covered in [`libs/sdk/ai/src/tools/README.md`](../../libs/sdk/ai/src/tools/README.md#adding-a-tool), along with what goes in each of `definitions/`, `operations/` and `toolkit/`.

2. **Host it here** — `src/tools/your_tool.ts`:
```typescript
   const { schema, metadata, handler } = xmcpTool(yourTool);
   export { schema, metadata };
   export default handler;
```
   Then add `'your_tool'` to the expected list in `src/smoke/server-boot.spec.ts`.

3. **Test it**:
```bash
   pnpm nx test sdk-ai      # the operation and the tool
   pnpm nx test mcp-server  # builds, boots the bundle, lists the tools
```

### Where Does the Logic Go?

In the SDK — see the "Where does my code go?" table in [`libs/sdk/ai/src/tools/README.md`](../../libs/sdk/ai/src/tools/README.md#where-does-my-code-go). **Nothing** goes in `apps/mcp-server/src/tools/`: xmcp loads every module there as a tool at boot, so it holds the three-line adapters and nothing else. Errors are thrown, never formatted by hand — a tool's `execute` turns anything thrown into a `ToolFailure` with a `code` and a `retryable` flag.

### CONTEXT Framework Principles

Every tool must follow the CONTEXT framework:

- **C - Context First**: Ensure context is initialized (handled by middleware)
- **O - One Intent Per Tool**: Each tool does ONE thing (verb-based names)
- **N - Narrow Parameters**: Maximum 5 parameters
- **T - Transform Responses**: Natural language, not JSON dumps
- **E - Educate with Errors**: Helpful error messages with next steps
- **X - eXplicit Orchestration**: Clear descriptions guide the LLM

### Tool Naming Conventions

✅ Good names:
- `publish_content_by_tag`
- `search_content_by_category`
- `archive_expired_content`

❌ Bad names:
- `manage_content` (too broad, not one intent)
- `contentAction` (not snake_case, not descriptive)
- `process` (what does it process?)

### Best Practices

1. **One tool = One user intent** (not one API endpoint)
2. **Reuse services** (don't duplicate HTTP calls)
3. **Format responses** (prose > JSON)
4. **Log everything** (use the Logger class)
5. **Validate everything** (use Zod schemas)
6. **Test everything** (follow existing test patterns)
