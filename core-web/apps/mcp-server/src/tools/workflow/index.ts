import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { z } from 'zod';

import { contentSaveHandler, contentActionHandler } from './handlers';

import { ContentCreateParamsSchema, ContentActionParamsSchema } from '../../types/workflow';

/**
 * Registers workflow tools with the MCP server
 */
export function registerWorkflowTools(server: McpServer) {
    server.registerTool(
        'content_save',
        {
            title: 'Save Content',
            description:
                'Saves content by firing a workflow action. This method creates new content or updates existing content based on the provided parameters. Only contentType and languageId are required fields. All other fields depend on the content type schema. Fields checkbox, radio, select and multiselect are a string of comma separated values.',
            annotations: {
                title: 'Save Content',
                readOnlyHint: false,
                idempotentHint: false,
                openWorldHint: true
            },
            inputSchema: z.object({
                content: ContentCreateParamsSchema,
                comments: z.string().optional()
            }) as any // eslint-disable-line @typescript-eslint/no-explicit-any -- Zod .shape incompatible with SDK InputArgs; runtime works
        },
        contentSaveHandler
    );

    server.registerTool(
        'content_action',
        {
            title: 'Perform Content Action',
            description:
                'Performs content actions (publish/unpublish/archive/unarchive/delete) by firing a workflow action. This method can publish, unpublish, archive, unarchive, or delete existing content based on the provided parameters.',
            annotations: {
                title: 'Perform Content Action',
                readOnlyHint: false,
                idempotentHint: false,
                openWorldHint: true
            },
            // eslint-disable-next-line @typescript-eslint/no-explicit-any -- Zod .shape incompatible with SDK InputArgs (ZodRawShapeCompat); runtime works
            inputSchema: ContentActionParamsSchema as any
        },
        contentActionHandler
    );
}

// Re-export types and handlers for external use if needed
export type { ContentSaveParams, ContentActionParams } from './handlers';
