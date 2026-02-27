# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Essential Commands

### Development Commands
```bash
# Build the MCP server for prod
yarn nx build mcp-server

# Run tests
yarn nx test mcp-server

# Run linting
yarn nx lint mcp-server
```

### Environment Setup
The MCP server requires two environment variables:
```bash
export DOTCMS_URL="https://your-dotcms-instance.com"
export AUTH_TOKEN="your-auth-token"
```

#### Configuration example:

``` JSON
{
  "mcpServers": {
    "dotcms": {
      "command": "node",
      "args": [
        "/Users/fmontes/Developer/dotcms/core/core-web/dist/apps/mcp-server/main.js"
      ],
      "env": {
        "DOTCMS_URL": "http://localhost:8080",
        "AUTH_TOKEN": "YOUR_DOTCMS_AUTH_TOKEN"
      }
    }
  }
}
```

## Architecture Overview

This is a **Model Context Protocol (MCP) server** for dotCMS, built with TypeScript and using the `@modelcontextprotocol/sdk` library. The server provides AI assistants with tools to interact with dotCMS content management functionality.

### Core Architecture

**Entry Point**: `src/main.ts` - Initializes the MCP server, validates environment variables, and registers all tool modules.

**Service Layer**:
- `AgnosticClient` (`src/services/client.ts`) - Base HTTP client with authentication and error handling
- `ContentTypeService` (`src/services/contentype.ts`) - Content type CRUD operations
- `WorkflowService` (`src/services/workflow.ts`) - Content workflow actions (save, publish, delete, etc.)
- `SiteService` (`src/services/site.ts`) - Site management operations

**Tool Registration**:
- `registerContentTypeTools()` - Content type listing and creation tools
- `registerWorkflowTools()` - Content save and workflow action tools
- `registerContextTools()` - Context initialization for discovering available schemas

**Type System**: All services use Zod schemas for runtime validation of inputs and outputs, with TypeScript types generated from the schemas.

### Key Patterns

**Service Inheritance**: All services extend `AgnosticClient` which provides:
- Automatic environment variable loading (`DOTCMS_URL`, `AUTH_TOKEN`)
- Authenticated HTTP requests with Bearer token
- Comprehensive error logging with request/response details
- Automatic Content-Type header management

**Tool Registration Pattern**: Each tool module exports a registration function that:
- Defines the tool schema with Zod validation
- Implements the tool handler with proper error handling
- Returns structured MCP responses with text content

**Caching Strategy**: The context tool implements a 30-minute cache for content type schemas and site information to avoid repeated API calls.

**Logging**: Uses a custom `Logger` class (`src/utils/logger.ts`) with structured logging throughout all services and tools.

## MCP Tools Available

### Context Initialization Tool
**Purpose**: Must be called first to discover all available content types, sites, and workflow schemes
**Usage**: Provides complete schema information for AI to understand what content types exist
**Caching**: Results cached for 30 minutes

### Content Type Tools
- `content_type_list` - List/filter existing content types
- `content_type_create` - Create new content types with fields

### Workflow Tools
- `content_save` - Create/update content using workflow actions
- `content_action` - Perform publish/unpublish/archive/delete actions on content

## Development Guidelines

### Adding New Services
1. Extend `AgnosticClient` for automatic authentication and error handling
2. Define Zod schemas for all input/output types in `src/types/`
3. Add comprehensive logging using the `Logger` class
4. Follow the existing service pattern with validation and error handling

### Adding New Tools
1. Create tool registration function following existing patterns
2. Use Zod schemas for input validation
3. Return proper MCP response format with text content
4. Handle errors gracefully with `isError: true` responses

### Type Definitions
- Content types: `src/types/contentype.ts`
- Workflow types: `src/types/workflow.ts`
- Site types: `src/types/site.ts`

All types use Zod schemas with TypeScript inference (`z.infer<typeof Schema>`)

### Testing
- Tests use Jest with Node.js environment
- Test files should follow `*.spec.ts` naming convention
- Focus on service layer validation and error handling

## Configuration

The server is configured as an Nx application with:
- **Build target**: Uses esbuild for fast compilation
- **Platform**: Node.js with CommonJS output
- **Bundle**: Individual file compilation (bundle: false)
- **Source maps**: Enabled in development, disabled in production

## Integration Notes

This MCP server is designed to be used with AI assistants that support the Model Context Protocol. The server provides tools for:
- Content discovery (what content types exist)
- Content creation (both content types and content instances)
- Content lifecycle management (publish, unpublish, archive, delete)

The context initialization tool is crucial - it should be called first to provide the AI with complete knowledge of the dotCMS instance's schema before attempting any content operations.
