import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { z } from 'zod';

import { exampleToolHandler } from './handlers';

/**
 * TODO: Define your input schema
 *
 * TIPS:
 * - Keep it to 5 parameters max (Narrow Parameters principle)
 * - Use descriptive names that match dotCMS terminology
 * - Add .optional() for non-required fields
 * - Add validation rules (max length, enums, etc.)
 */
const ExampleToolInputSchema = z.object({
    // Example parameters - replace with your own
    contentType: z.string().describe('Content type variable name'),
    action: z.enum(['publish', 'unpublish', 'archive']).describe('Action to perform'),
    limit: z.number().int().positive().max(100).optional().default(10)
});

/**
 * Registers this tool with the MCP server
 *
 * TODO:
 * 1. Update the tool name (use snake_case, verb-first)
 * 2. Update the title and description
 * 3. Update the annotations
 * 4. Update the inputSchema
 */
export function registerExampleTools(server: McpServer) {
    server.registerTool(
        'example_tool_action', // TODO: Replace with your tool name (e.g., 'bulk_publish_by_tag')
        {
            title: 'Example Tool Action', // TODO: Human-readable title

            // TODO: Write a clear description that explains:
            // - What this tool does
            // - When to use it
            // - What parameters it needs
            // - Any prerequisites (like specific content types existing)
            description: `
                TODO: Describe your tool here.

                This tool does X by using Y service to accomplish Z.

                Prerequisites:
                - Context must be initialized first
                - Content type must exist

                Example usage: "Publish all blog posts tagged with 'featured'"
            `.trim(),

            annotations: {
                title: 'Example Tool Action',

                // TODO: Set to true if this tool only reads data (doesn't modify)
                readOnlyHint: false,

                // TODO: Set to true if calling this tool multiple times with same params is safe
                idempotentHint: false,

                // TODO: Set to true if parameters accept open-ended values
                openWorldHint: true
            },

            inputSchema: ExampleToolInputSchema as any
        },
        exampleToolHandler
    );
}

// TODO: Don't forget to register this in src/main.ts:
// import { registerExampleTools } from './tools/_example-tool';
// registerExampleTools(server);
