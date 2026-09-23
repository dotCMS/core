## Adding New Tools

### Quick Start

Tools are written once, in [`@dotcms/ai/tools`](../../libs/sdk/ai/README.md#ready-made-tools--dotcmsaitools), so every host — this server, a consumer's own MCP server, an agent — gets the same tool. This app only hosts them.

1. **Write the operation** in `libs/sdk/ai/src/tools/your-operation.ts`: typed options in (including `dotcms: DotCMSRuntime`), a typed manifest out, typed errors thrown. Test it next to it in `your-operation.spec.ts` with a fake `DotCMSRuntime`. Copy the `seen` + `afterEach(unlistedCalls(...))` guard from `page-verify.spec.ts`, so every request your tests exercise is checked against the tool's `endpoints`.

2. **Define the tool and its factory** in `libs/sdk/ai/src/tools/definitions/your-tool.ts`:
```typescript
   const definition = defineTool({
       name: 'your_tool',
       title: 'Do The Thing',
       description: `What it does, when to use it, what it returns.`,
       inputSchema: z.object({ path: z.string().min(1).describe('…') }),
       annotations: { readOnlyHint: true, destructiveHint: false, idempotentHint: true, openWorldHint: true },
       // Enforced: any request not listed here is refused before it reaches the wire.
       endpoints: [...RESOLVE_ENDPOINTS, 'GET /api/v1/your/{id}'],
       async handler(args, ctx: ToolContext<RequestToolOptions>): Promise<YourManifest> {
           return yourOperation({ dotcms: ctx.runtime(), ...args });
       }
   });

   export function yourTool(options: RequestToolOptions = {}): DotCMSTool<typeof definition.inputSchema, YourManifest> {
       return createTool(definition, options);
   }
```

3. **Export it** — the factory and the operation — from `libs/sdk/ai/src/tools/index.ts`, and add the factory to `FACTORIES` in `create-tools.spec.ts`.

4. **Host it here** — `src/tools/your_tool.ts`:
```typescript
   const { schema, metadata, handler } = xmcpTool(yourTool);
   export { schema, metadata };
   export default handler;
```
   Then add `'your_tool'` to the expected list in `src/smoke/server-boot.spec.ts`.

5. **Test it**:
```bash
   pnpm nx test sdk-ai      # the operation and the tool
   pnpm nx test mcp-server  # builds, boots the bundle, lists the tools
```

### Where Does the Logic Go?

- **Talking to dotCMS** → the operation, through `dotcms.request(...)`. Auth, the request deadline and the tool's own allow-list are already applied; never build a `fetch` yourself. A new request means a new entry in the tool's `endpoints` — the spec guard fails until it's there.
- **Anything the model must be told** → the tool's `description` and the `.describe()` on each input field.
- **Errors** → throw. The tool's `execute` turns anything thrown into a `ToolFailure` with a `code` and a `retryable` flag, so a handler never formats its own error string.
- **Nothing** → in `apps/mcp-server/src/tools/`. It is loaded as tools at boot, so it holds the three-line adapters and nothing else.

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
