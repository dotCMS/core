import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { z } from 'zod';

import { contextInitializationHandler } from './handlers';

/**
 * Registers context tools with the MCP server
 */
export function registerContextTools(server: McpServer) {
    server.registerTool(
        'context_initialization',
        {
            title: 'Context Initialization',
            description:
                'IMPORTANT: This tool MUST be called FIRST before any other operations to learn all available content type schemas and current site information in the dotCMS instance. This provides the LLM with complete knowledge of all content types, their fields, field types, structure, and the current site details. The response is cached for 30 minutes to avoid repeated API calls. Use this to understand what content types exist and their complete field definitions before creating or working with content.',
            annotations: {
                title: 'Context Initialization',
                readOnlyHint: true
            },
            inputSchema: z.object({}).shape
        },
        contextInitializationHandler
    );
}
