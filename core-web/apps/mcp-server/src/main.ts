import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import { z } from 'zod';

import { registerContentTypeTools } from './tools/content-types';
import { registerContextTools } from './tools/context';
import { registerSearchTools } from './tools/search';
import { registerWorkflowTools } from './tools/workflow';
import { createContextCheckingServer } from './utils/context-checking-server';
import { registerListFolderTools } from './tools/list-folder';

const originalServer = new McpServer({
    name: 'DotCMS',
    version: '1.0.0'
});

// Create context-checking server proxy to enforce initialization requirements
const server = createContextCheckingServer(originalServer);

const DOTCMS_URL = process.env.DOTCMS_URL;
const AUTH_TOKEN = process.env.AUTH_TOKEN;

const urlSchema = z.string().url();
const tokenSchema = z.string().min(1, 'AUTH_TOKEN cannot be empty');

try {
    urlSchema.parse(DOTCMS_URL);
    tokenSchema.parse(AUTH_TOKEN);
} catch (e) {
    // eslint-disable-next-line no-console
    console.error('Invalid environment variables:', e);
    process.exit(1);
}

// Register context tools first (context_initialization is exempt from checking)
registerContextTools(server);

// Register content type tools (will be protected by context checking)
registerContentTypeTools(server);

// Register search tools (will be protected by context checking)
registerSearchTools(server);

// Register workflow tools (will be protected by context checking)
registerWorkflowTools(server);

// Register custom tools
registerListFolderTools(server);

const transport = new StdioServerTransport();
(async () => {
    await server.connect(transport);
})();
