# dotCMS MCP Server

The dotCMS MCP (Model Context Protocol) Server enables AI assistants to interact directly with dotCMS content management capabilities. This powerful integration allows AI tools like Claude, GPT, and others to discover content schemas, create content types, manage content workflows, and perform complex content operations—all through natural language interactions.

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
    -   [Context Initialization](#context-initialization)
    -   [Content Type Management](#content-type-management)
    -   [Content Operations](#content-operations)
    -   [Content Search](#content-search)
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

| Variable     | Required | Description                        | Example |
| ------------ | -------- | ---------------------------------- | ------- |
| `DOTCMS_URL` | ✅       | Your dotCMS instance URL           | `https://demo.dotcms.com` |
| `AUTH_TOKEN` | ✅       | API authentication token (created in [setup step](#create-a-dotcms-api-token)) | `your-api-token-here` |
| `VERBOSE`    | ❌       | Enable detailed logging for troubleshooting | `true` |
| `RESPONSE_MAX_LENGTH` | ❌       | Maximum character limit for response truncation (no truncation if not set) | `5000` |

### Response Truncation

The MCP server includes intelligent response truncation to manage large data responses and optimize performance. This feature is controlled by the `RESPONSE_MAX_LENGTH` environment variable:

**Behavior:**
- **Not set or 0**: No truncation occurs (unlimited response length)
- **Positive number**: Responses are truncated to the specified character limit
- **Invalid value**: No truncation occurs (falls back to unlimited)

**When truncation occurs:**
- Large content search results
- Extensive content type schemas
- Bulk content operations
- Any response exceeding the specified limit

**Truncation format:**
```
Original response content...
[truncated]
```


## Quickstart

Get up and running with the dotCMS MCP Server in minutes:

### Claude Desktop Setup

Add the MCP server to your Claude Desktop configuration file. The configuration file location varies by operating system:

- **macOS**: `~/Library/Application Support/Claude/claude_desktop_config.json`
- **Windows**: `%APPDATA%\Claude\claude_desktop_config.json`

```json
{
    "mcpServers": {
        "dotcms": {
            "command": "npx",
            "args": ["-y", "@dotcms/mcp-server"],
            "env": {
                "DOTCMS_URL": "https://your-dotcms-instance.com",
                "AUTH_TOKEN": "your-auth-token"
            }
        }
    }
}
```

### Cursor IDE Setup

Add the MCP server to your Cursor configuration. Open Cursor Settings and navigate to "Features" > "Model Context Protocol" or create/edit the configuration file:

```json
{
    "mcpServers": {
        "dotcms": {
            "command": "npx",
            "args": ["-y", "@dotcms/mcp-server"],
            "env": {
                "DOTCMS_URL": "https://your-dotcms-instance.com",
                "AUTH_TOKEN": "your-auth-token"
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
AI: [Automatically learns your content structure and creates the blog post]

You: "Show me all my content types"
AI: [Discovers and displays your content schemas]

You: "Generate a React component for my Product content type"
AI: [Analyzes your Product fields and generates a complete component]
```

## Available Tools

The dotCMS MCP Server provides four core tools that enable comprehensive content management through AI:

**Tool**: `context_initialization`

**Purpose**: Must be called first to discover all available content types, sites, and workflow schemes

```
You: "Learn about my dotCMS setup"
AI: [Calls context_initialization and learns your complete content schema]
```

**What it provides:**

-   Complete list of content types with field definitions
-   Current site information
-   Available workflow schemes
-   Caches results for 30 minutes to optimize performance

### Content Type Management

**Tools**: `content_type_list`, `content_type_create`

**Purpose**: Discover and create content type schemas

```
You: "Show me all my content types"
AI: [Calls content_type_list to display your content schemas]

You: "Create a new Product content type with name, price, and description fields"
AI: [Calls content_type_create with the appropriate schema]
```

**Capabilities:**

-   List and filter existing content types
-   Create new content types with custom fields
-   Support for all dotCMS field types (Text, Image, Date, etc.)

### Content Operations

**Tools**: `content_save`, `content_action`

**Purpose**: Create, update, and manage content through workflow actions

```
You: "Create a new blog post about dotCMS MCP integration"
AI: [Calls content_save to create the content]

You: "Publish the blog post we just created"
AI: [Calls content_action with PUBLISH action]
```

**Supported Actions:**

-   **Save**: Create or update content
-   **Publish**: Make content live
-   **Unpublish**: Remove from live site
-   **Archive**: Move to archive state
-   **Delete**: Permanently remove content

### Content Search

**Tool**: `content_search`

**Purpose**: Query content using Lucene syntax

```
You: "Find all blog posts published this year that mention 'AI'"
AI: [Calls content_search with appropriate Lucene query]
```

**Search Capabilities:**

-   Full Lucene query syntax support
-   Filter by content type, date ranges, field values
-   Wildcard and fuzzy search
-   Boolean operators (AND, OR, NOT)


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

# Build the server
yarn nx build mcp-server
```

> [!NOTE]
> Files are located in `core-web/apps/mcp-server` and we use [Nx monorepo](https://nx.dev/)

#### 2. Use MCP Inspector for debug

After a succesful build

```bash
npx @modelcontextprotocol/inspector -e DOTCMS_URL=https://demo.dotcms.com -e AUTH_TOKEN=the-auth-token node dist/apps/mcp-server
```

#### 3. Use Local Build in AI Assistants

**Claude Desktop Configuration:**
```json
{
    "mcpServers": {
        "dotcms": {
            "command": "node",
            "args": ["/path/to/dotcms/core/core-web/dist/apps/mcp-server/main.js"],
            "env": {
                "DOTCMS_URL": "your-dotcms-url",
                "AUTH_TOKEN": "your-auth-token"
            }
        }
    }
}
```

**Cursor IDE Configuration:**
```json
{
    "mcpServers": {
        "dotcms": {
            "command": "node",
            "args": ["/path/to/dotcms/core/core-web/dist/apps/mcp-server/main.js"],
            "env": {
                "DOTCMS_URL": "your-dotcms-url",
                "AUTH_TOKEN": "your-auth-token"
            }
        }
    }
}
```

### Project Structure

```
mcp-server/
├── src/
│   ├── main.ts              # Entry point and server initialization
│   ├── services/            # HTTP clients for dotCMS APIs
│   │   ├── client.ts        # Base authenticated HTTP client
│   │   ├── contentype.ts    # Content type operations
│   │   ├── workflow.ts      # Content workflow actions
│   │   ├── search.ts        # Content search functionality
│   │   └── site.ts          # Site information
│   ├── tools/               # MCP tool implementations
│   │   ├── context/         # Context initialization
│   │   ├── content-types/   # Content type management
│   │   ├── workflow/        # Content operations
│   │   └── search/          # Search functionality
│   ├── types/               # TypeScript type definitions
│   └── utils/               # Shared utilities
├── jest.config.ts           # Test configuration
└── project.json             # Nx project configuration
```

### Key Architecture Patterns

**Service Layer**: All services extend `AgnosticClient` which provides:

-   Automatic authentication with Bearer tokens
-   Environment variable validation
-   Comprehensive error logging
-   Structured request/response handling

**Tool Registration**: Each tool module exports a registration function that:

-   Defines tool schema with Zod validation
-   Implements handlers with proper error handling
-   Returns structured MCP responses

**Type Safety**: Extensive use of Zod schemas for:

-   Runtime input validation
-   TypeScript type generation
-   API response validation

### Development Commands

```bash
# Build for development
yarn nx build mcp-server

# Lint the code
yarn nx lint mcp-server

# Serve in development mode
yarn nx serve mcp-server

# Run all tests
yarn nx test mcp-server

# Run tests in watch mode
yarn nx test mcp-server --watch

# Run with coverage
yarn nx test mcp-server --coverage
```

### Contributing Guidelines

When adding new MCP tools:

1. Create the tool in the appropriate `src/tools/` subdirectory
2. Define Zod schemas for input validation
3. Implement proper error handling
4. Add comprehensive logging
5. Register the tool in `src/main.ts`
6. Add tests and documentation

**Development Guidelines:**

-   **Follow TypeScript best practices**: Use strict typing and proper interfaces
-   **Add comprehensive tests**: Include unit tests for new functionality
-   **Document your changes**: Update documentation for new features
-   **Use Zod validation**: All inputs and outputs should be validated
-   **Follow logging patterns**: Use the Logger class for consistent logging

## Security Best Practices

### API Token Security

-   **Principle of Least Privilege**: Only grant permissions required for your use case
-   **Environment Variables**: Never hardcode tokens in source code
-   **Token Rotation**: Regularly rotate API tokens
-   **Monitoring**: Monitor API usage for unusual patterns
-   **HTTPS Only**: Always use HTTPS for dotCMS connections

### Logging and Monitoring

The MCP server includes comprehensive logging:

-   **Structured Logging**: All operations logged with context
-   **Error Tracking**: Detailed error information with stack traces
-   **Request/Response Logging**: Full API interaction logging in verbose mode
-   **Performance Monitoring**: Request timing and performance metrics

## dotCMS Support

We offer multiple channels to get help with the dotCMS MCP Server:

-   **GitHub Issues**: For bug reports and feature requests, please [open an issue](https://github.com/dotCMS/core/issues/new/choose) in the GitHub repository
-   **Community Forum**: Join our [community discussions](https://community.dotcms.com/) to ask questions and share solutions
-   **Documentation**: Join our [community discussions](https://dev.dotcms.com/) to ask questions and share solutions

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

### Development Guidelines

-   **Follow TypeScript best practices**: Use strict typing and proper interfaces
-   **Add comprehensive tests**: Include unit tests for new functionality
-   **Document your changes**: Update documentation for new features
-   **Use Zod validation**: All inputs and outputs should be validated
-   **Follow logging patterns**: Use the Logger class for consistent logging

### Adding New Tools

When adding new MCP tools:

1. Create the tool in the appropriate `src/tools/` subdirectory
2. Define Zod schemas for input validation
3. Implement proper error handling
4. Add comprehensive logging
5. Register the tool in `src/main.ts`
6. Add tests and documentation

## Licensing Information

dotCMS comes in multiple editions and as such is dual-licensed. The dotCMS Community Edition is licensed under the GPL 3.0 and is freely available for download, customization, and deployment for use within organizations of all stripes. dotCMS Enterprise Editions (EE) adds several enterprise features and is available via a supported, indemnified commercial license from dotCMS. For the differences between the editions, see [the feature page](http://www.dotcms.com/cms-platform/features).

This MCP Server is part of dotCMS's dual-licensed platform (GPL 3.0 for Community, commercial license for Enterprise).

[Learn more](https://www.dotcms.com) at [dotcms.com](https://www.dotcms.com).
