# dotCMS MCP Server

The dotCMS MCP (Model Context Protocol) Server enables AI assistants to interact directly with dotCMS content management capabilities. Built with [xmcp](https://xmcp.dev), this server provides sandbox-isolated code execution for exploring the dotCMS REST API specification and executing authenticated API calls.

### When to Use It:

-   Building AI-powered content management workflows
-   Automating content creation and publishing processes
-   Creating intelligent content discovery and search experiences
-   Developing AI assistants that need to understand your content structure
-   Implementing automated content operations and bulk updates

### Key Benefits:

-   **Conversational Content Management**: Ask AI to create, edit, and publish content using natural language instead of navigating through admin interfaces
-   **Intelligent Content Discovery**: Let AI understand your content structure and help you find, organize, and manage content across your site
-   **Automated Publishing Workflows**: Have AI handle content approval, publishing, and archiving based on your business rules
-   **Smart Content Creation**: Generate content that follows your existing content types and field requirements automatically
-   **Bulk Content Operations**: Process multiple pieces of content at once through simple AI conversations
-   **Developer Productivity**: Generate code components, forms, and integrations based on your actual dotCMS content structure

## Table of Contents

-   [Prerequisites & Setup](#prerequisites--setup)
    -   [Get a dotCMS Environment](#get-a-dotcms-environment)
    -   [Create a dotCMS API Token](#create-a-dotcms-api-token)
-   [Quickstart](#quickstart)
-   [Available Tools](#available-tools)
    -   [Search](#search)
    -   [Execute](#execute)
-   [Development](#development)
    -   [Local Development Setup](#local-development-setup)
    -   [Project Structure](#project-structure)
    -   [Development Commands](#development-commands)
    -   [Contributing Guidelines](#contributing-guidelines)
-   [Security Best Practices](#security-best-practices)
-   [dotCMS Support](#dotcms-support)
-   [How To Contribute](#how-to-contribute)
-   [Licensing Information](#licensing-information)

## Prerequisites & Setup

### Get a dotCMS Environment

#### Version Compatibility

-   **Recommended**: dotCMS Evergreen
-   **Minimum**: dotCMS v24.4
-   **Best Experience**: Latest Evergreen release

#### Environment Setup

**For Production Use:**

-   ☁️ [Cloud hosting options](https://www.dotcms.com/pricing) - managed solutions with SLA
-   🛠️ [Self-hosted options](https://dev.dotcms.com/docs/current-releases) - deploy on your infrastructure

**For Testing & Development:**

-   🧑🏻‍💻 [dotCMS demo site](https://demo.dotcms.com/dotAdmin/#/public/login) - perfect for trying out the MCP server
-   📘 [Learn how to use the demo site](https://dev.dotcms.com/docs/demo-site)

**For Local Development:**

-   🐳 [Docker setup guide](https://github.com/dotCMS/core/tree/main/docker/docker-compose-examples/single-node-demo-site)
-   💻 [Local installation guide](https://dev.dotcms.com/docs/quick-start-guide)

### Create a dotCMS API Token

> [!WARNING]
> This MCP server requires an API token with **write permissions** for Content Types, Content, and Workflows. Only use tokens with the minimum required permissions and secure them properly.

This integration requires an API Token with content management permissions:

1. Go to the **dotCMS admin panel**
2. Click on **System** > **Users**
3. Select the user (with proper permissions) you want to create the API Token for
4. Go to **API Access Key** and generate a new key

For detailed instructions, please refer to the [dotCMS API Documentation](https://dev.dotcms.com/docs/rest-api-authentication).

## Configuration

Before setting up the MCP server, you need these environment variables to connect to your dotCMS instance:

### Environment Variables

| Variable           | Required | Description                        | Example |
| ------------------ | -------- | ---------------------------------- | ------- |
| `DOTCMS_URL`  | ✅       | Your dotCMS instance URL           | `https://demo.dotcms.com` |
| `AUTH_TOKEN` | ✅       | API authentication token (created in [setup step](#create-a-dotcms-api-token)) | `your-api-token-here` |
| `SANDBOX_TIMEOUT`  | ❌       | Sandbox execution timeout in ms (default: 15000) | `15000` |


## Quickstart

Get up and running with the dotCMS MCP Server in minutes.

The server runs on both **Node.js** (≥20) and **Bun** — the correct sandbox implementation is selected automatically at runtime.

> [!NOTE]
> This version is currently in **beta**. Once stable, replace `@dotcms/mcp-server@beta` with `@dotcms/mcp-server` in the examples below.

### Claude Desktop Setup

Add the MCP server to your Claude Desktop configuration file. The configuration file location varies by operating system:

- **macOS**: `~/Library/Application Support/Claude/claude_desktop_config.json`
- **Windows**: `%APPDATA%\Claude\claude_desktop_config.json`

**Using npx (Node.js):**

```json
{
    "mcpServers": {
        "dotcms": {
            "command": "npx",
            "args": ["-y", "@dotcms/mcp-server@beta"],
            "env": {
                "DOTCMS_URL": "https://your-dotcms-instance.com",
                "AUTH_TOKEN": "your-api-token"
            }
        }
    }
}
```

**Using bunx (Bun):**

```json
{
    "mcpServers": {
        "dotcms": {
            "command": "bunx",
            "args": ["@dotcms/mcp-server@beta"],
            "env": {
                "DOTCMS_URL": "https://your-dotcms-instance.com",
                "AUTH_TOKEN": "your-api-token"
            }
        }
    }
}
```

### Cursor IDE Setup

Add the MCP server to your Cursor configuration. Open Cursor Settings and navigate to "Features" > "Model Context Protocol" or create/edit the configuration file:

**Using npx (Node.js):**

```json
{
    "mcpServers": {
        "dotcms": {
            "command": "npx",
            "args": ["-y", "@dotcms/mcp-server@beta"],
            "env": {
                "DOTCMS_URL": "https://your-dotcms-instance.com",
                "AUTH_TOKEN": "your-api-token"
            }
        }
    }
}
```

**Using bunx (Bun):**

```json
{
    "mcpServers": {
        "dotcms": {
            "command": "bunx",
            "args": ["@dotcms/mcp-server@beta"],
            "env": {
                "DOTCMS_URL": "https://your-dotcms-instance.com",
                "AUTH_TOKEN": "your-api-token"
            }
        }
    }
}
```

### Start Using

1. **Restart** your AI assistant (Claude Desktop or Cursor)
2. **Start creating**: Ask the AI to create content, manage workflows, or generate code

Example interactions:
```
You: "Create a new blog post about AI in content management"
AI: [Searches the API spec, then executes the appropriate API calls]

You: "Show me all my content types"
AI: [Discovers and displays your content schemas]

You: "Generate a React component for my Product content type"
AI: [Analyzes your Product fields and generates a complete component]
```

## Available Tools

The dotCMS MCP Server provides two core tools that enable comprehensive content management through AI:

### Search

**Tool**: `search`

**Purpose**: Explore the dotCMS REST API specification using JavaScript code that runs in an isolated sandbox.

The `spec` global contains the full dereferenced OpenAPI spec with `paths` object.

```javascript
// List all available endpoint paths
return Object.keys(spec.paths)

// Find endpoints related to content types
return Object.entries(spec.paths)
  .filter(([path]) => path.includes('contenttype'))
  .map(([path, methods]) => ({ path, methods: Object.keys(methods) }))
```

### Execute

**Tool**: `execute`

**Purpose**: Execute authenticated API calls against your dotCMS instance using JavaScript code in an isolated sandbox.

```javascript
// List content types
const result = await api.request({ path: '/api/v1/contenttype' })
return result

// Search content with Elasticsearch
const result = await api.request({
  method: 'POST',
  path: '/api/v1/es/search',
  body: { query: 'contentType:webPageContent +languageId:1' }
})
return pick(result.contentlets, ['identifier', 'title', 'modDate'])
```

**Helper utilities available**: `pick(arr, fields)`, `table(arr)`, `count(arr, field)`, `sum(arr, field)`, `first(arr, n)`


## Development

### Local Development Setup

For developers who want to contribute or modify the MCP server:

#### 1. Clone and Setup

```bash
# Clone the dotCMS repository
git clone https://github.com/dotCMS/core.git
cd core/core-web

# Install dependencies
yarn install

# Build the server (pass the OpenAPI spec URL or local file path)
yarn nx build mcp-server --specUrl=https://demo.dotcms.com/api/openapi.json
```

> [!NOTE]
> Files are located in `core-web/apps/mcp-server` and we use [Nx monorepo](https://nx.dev/)

#### 2. Use MCP Inspector for debug

After a successful build:

```bash
npx @modelcontextprotocol/inspector -e DOTCMS_URL=https://demo.dotcms.com -e AUTH_TOKEN=the-api-token node dist/apps/mcp-server/stdio.js
```

#### 3. Use Local Build in AI Assistants

The built server works with both `node` and `bun` — the correct sandbox is selected automatically.

**Using Node.js:**
```json
{
    "mcpServers": {
        "dotcms": {
            "command": "node",
            "args": ["/path/to/dotcms/core/core-web/dist/apps/mcp-server/stdio.js"],
            "env": {
                "DOTCMS_URL": "your-dotcms-url",
                "AUTH_TOKEN": "your-api-token"
            }
        }
    }
}
```

**Using Bun:**
```json
{
    "mcpServers": {
        "dotcms": {
            "command": "bun",
            "args": ["/path/to/dotcms/core/core-web/dist/apps/mcp-server/stdio.js"],
            "env": {
                "DOTCMS_URL": "your-dotcms-url",
                "AUTH_TOKEN": "your-api-token"
            }
        }
    }
}
```

### Project Structure

```
mcp-server/
├── src/
│   ├── tools/              # MCP tool implementations
│   │   ├── search.ts       # API spec exploration tool
│   │   └── execute.ts      # API execution tool
│   ├── lib/                # Core library code
│   │   ├── executor.ts     # Sandbox executor orchestration
│   │   ├── http-client.ts  # Authenticated HTTP adapter
│   │   ├── spec.ts         # OpenAPI spec loader
│   │   ├── types.ts        # TypeScript type definitions
│   │   └── sandbox/        # Sandbox isolation (dual-runtime)
│   │       ├── index.ts        # Runtime detection factory
│   │       ├── interface.ts    # Sandbox interface
│   │       ├── bun-worker.ts   # Bun Web Worker sandbox
│   │       └── node-worker.ts  # Node.js worker_threads sandbox
│   ├── prompts/            # Prompt templates (xmcp convention)
│   └── generated/          # Build-time generated files
│       └── spec.json       # Processed OpenAPI spec
├── scripts/
│   └── generate-spec.ts    # OpenAPI spec processor
├── openapi.json            # Full dotCMS OpenAPI specification
├── xmcp.config.ts          # xmcp framework configuration
├── jest.config.ts          # Test configuration
└── project.json            # Nx project configuration
```

### Key Architecture Patterns

**xmcp Framework**: The server uses [xmcp](https://xmcp.dev) for MCP protocol handling:
-   Tools are auto-discovered from `src/tools/`
-   Each tool exports `schema`, `metadata`, and a default handler function
-   Built with rspack for optimized bundling

**Sandbox Isolation**: Code execution is sandboxed using Workers with dual-runtime support:
-   **Bun**: Uses native Web Workers (`Blob` + `URL.createObjectURL`)
-   **Node.js**: Uses `worker_threads` (`new Worker(code, { eval: true })`)
-   Runtime is detected automatically via `typeof globalThis.Bun`
-   API tokens are never exposed to sandbox code
-   Configurable timeout prevents runaway execution
-   Adapter pattern bridges sandbox ↔ main thread for API calls

**Build-time Spec Processing**: The OpenAPI spec is pre-processed at build time:
-   `generate-spec` target dereferences `$ref` pointers and filters to relevant endpoints
-   Output is a compact JSON embedded in the bundle
-   Reduces runtime overhead and MCP response size

### Development Commands

```bash
# Build for production (pass the OpenAPI spec URL or local file path)
yarn nx build mcp-server --specUrl=https://demo.dotcms.com/api/openapi.json

# Development mode (with hot reload)
yarn nx serve mcp-server

# Lint the code
yarn nx lint mcp-server

# Serve the built server
yarn nx serve mcp-server

# Run all tests
yarn nx test mcp-server

# Run tests in watch mode
yarn nx test mcp-server --watch

# Regenerate the spec only (URL or local file path)
yarn nx generate-spec mcp-server -- https://demo.dotcms.com/api/openapi.json
```

### Contributing Guidelines

When adding new MCP tools:

1. Create a new `.ts` file in `src/tools/`
2. Export `schema` (Zod), `metadata` (ToolMetadata), and a default handler
3. xmcp auto-discovers the tool — no registration needed
4. Add tests and documentation

## Security Best Practices

### API Token Security

-   **Principle of Least Privilege**: Only grant permissions required for your use case
-   **Environment Variables**: Never hardcode tokens in source code
-   **Token Rotation**: Regularly rotate API tokens
-   **Monitoring**: Monitor API usage for unusual patterns
-   **HTTPS Only**: Always use HTTPS for dotCMS connections
-   **Sandbox Isolation**: API tokens are injected by the main thread and never exposed to sandbox code

## dotCMS Support

We offer multiple channels to get help with the dotCMS MCP Server:

-   **GitHub Issues**: For bug reports and feature requests, please [open an issue](https://github.com/dotCMS/core/issues/new/choose) in the GitHub repository
-   **Community Forum**: Join our [community discussions](https://community.dotcms.com/) to ask questions and share solutions
-   **Documentation**: Visit our [developer documentation](https://dev.dotcms.com/) for detailed guides

When reporting issues, please include:

-   MCP server version and build information
-   dotCMS version and environment details
-   AI assistant being used (Claude, Cursor, etc.)
-   Minimal reproduction steps
-   Expected vs. actual behavior
-   Relevant log output

## How To Contribute

GitHub pull requests are the preferred method to contribute code to dotCMS. We welcome contributions to the dotCMS MCP Server! If you'd like to contribute, please follow these steps:

1. Fork the repository [dotCMS/core](https://github.com/dotCMS/core)
2. Create a feature branch (`git checkout -b feature/amazing-mcp-feature`)
3. Make your changes in the `apps/mcp-server` directory
4. Add tests for new functionality
5. Run the test suite (`yarn nx test mcp-server`)
6. Commit your changes (`git commit -m 'Add amazing MCP feature'`)
7. Push to the branch (`git push origin feature/amazing-mcp-feature`)
8. Open a Pull Request

## Licensing Information

dotCMS comes in multiple editions and as such is dual-licensed. The dotCMS Community Edition is licensed under the GPL 3.0 and is freely available for download, customization, and deployment for use within organizations of all stripes. dotCMS Enterprise Editions (EE) adds several enterprise features and is available via a supported, indemnified commercial license from dotCMS. For the differences between the editions, see [the feature page](http://www.dotcms.com/cms-platform/features).

This MCP Server is part of dotCMS's dual-licensed platform (GPL 3.0 for Community, commercial license for Enterprise).

[Learn more](https://www.dotcms.com) at [dotcms.com](https://www.dotcms.com).
