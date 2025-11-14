## Adding New Tools

### Quick Start

1. **Copy the example tool**:
```bash
   cp -r src/tools/_example-tool src/tools/your-tool-name
```

2. **Follow the TODOs** in each file

3. **Register in `main.ts`**:
```typescript
   import { registerYourTools } from './tools/your-tool-name';
   registerYourTools(server);
```

4. **Test it**:
```bash
   yarn nx test mcp-server
```

### Do I Need a New Service?

**Use existing services if:**
- You're working with content types → `ContentTypeService`
- You're creating/publishing/archiving content → `WorkflowService`
- You're searching content → `SearchService`
- You need site information → `SiteService`

**Create a new service if:**
- You need a dotCMS API endpoint that doesn't have a service yet
- You're integrating with an external system (not dotCMS)

### Creating a New Service

If you need a new service:

1. **Create service file** in `src/services/your-service.ts`
2. **Extend AgnosticClient**:
```typescript
   export class YourService extends AgnosticClient {
       // Your service gets automatic auth, logging, error handling
   }
```
3. **Add Zod schemas** in `src/types/your-types.ts`
4. **Add tests** in `src/services/your-service.spec.ts`
5. **Then create your tool** using the tool template

See existing services for patterns to follow.

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
