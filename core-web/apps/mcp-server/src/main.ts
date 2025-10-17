import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { StreamableHTTPServerTransport } from '@modelcontextprotocol/sdk/server/streamableHttp.js';
import { isInitializeRequest } from '@modelcontextprotocol/sdk/types.js';
import express from 'express';
import { z } from 'zod';

import { randomUUID } from 'node:crypto';

import { registerContentTypeTools } from './tools/content-types';
import { registerContextTools } from './tools/context';
import { registerSearchTools } from './tools/search';
import { registerWorkflowTools } from './tools/workflow';
import { createContextCheckingServer } from './utils/context-checking-server';

const DOTCMS_URL = process.env.DOTCMS_URL;
const AUTH_TOKEN = process.env.AUTH_TOKEN;
const PORT = process.env.PORT ? parseInt(process.env.PORT, 10) : 3000;

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

const app = express();
app.use(express.json());

// Map to store transports by session ID
const transports: { [sessionId: string]: StreamableHTTPServerTransport } = {};

// Create and configure MCP server
function createMcpServer() {
    const originalServer = new McpServer({
        name: 'DotCMS',
        version: '1.0.0'
    });

    // Create context-checking server proxy to enforce initialization requirements
    const server = createContextCheckingServer(originalServer);

    // Register context tools first (context_initialization is exempt from checking)
    registerContextTools(server);

    // Register content type tools (will be protected by context checking)
    registerContentTypeTools(server);

    // Register search tools (will be protected by context checking)
    registerSearchTools(server);

    // Register workflow tools (will be protected by context checking)
    registerWorkflowTools(server);

    return server;
}

// Handle POST requests for client-to-server communication
app.post('/mcp', async (req, res) => {
    // Check for existing session ID
    const sessionId = req.headers['mcp-session-id'] as string | undefined;
    let transport: StreamableHTTPServerTransport;

    if (sessionId && transports[sessionId]) {
        // Reuse existing transport
        transport = transports[sessionId];
    } else if (!sessionId && isInitializeRequest(req.body)) {
        // New initialization request
        transport = new StreamableHTTPServerTransport({
            sessionIdGenerator: () => randomUUID(),
            onsessioninitialized: (sessionId) => {
                // Store the transport by session ID
                transports[sessionId] = transport;
            }
            // DNS rebinding protection is disabled for local development
            // For production deployments, enable it with:
            // enableDnsRebindingProtection: true,
            // allowedHosts: ['your-domain.com'],
            // allowedOrigins: ['https://your-domain.com']
        });

        // Clean up transport when closed
        transport.onclose = () => {
            if (transport.sessionId) {
                delete transports[transport.sessionId];
            }
        };

        const server = createMcpServer();

        // Connect to the MCP server
        await server.connect(transport);
    } else {
        // Invalid request
        res.status(400).json({
            jsonrpc: '2.0',
            error: {
                code: -32000,
                message: 'Bad Request: No valid session ID provided'
            },
            id: null
        });
        return;
    }

    // Handle the request
    await transport.handleRequest(req, res, req.body);
});

// Reusable handler for GET and DELETE requests
const handleSessionRequest = async (
    req: express.Request,
    res: express.Response
) => {
    const sessionId = req.headers['mcp-session-id'] as string | undefined;
    if (!sessionId || !transports[sessionId]) {
        res.status(400).send('Invalid or missing session ID');
        return;
    }

    const transport = transports[sessionId];
    await transport.handleRequest(req, res);
};

// Handle GET requests for server-to-client notifications via SSE
app.get('/mcp', handleSessionRequest);

// Handle DELETE requests for session termination
app.delete('/mcp', handleSessionRequest);

app.listen(PORT, () => {
    // eslint-disable-next-line no-console
    console.log(`DotCMS MCP Server listening on http://localhost:${PORT}/mcp`);
});
