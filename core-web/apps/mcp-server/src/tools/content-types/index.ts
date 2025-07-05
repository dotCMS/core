import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { z } from 'zod';

import { contentTypeListHandler, contentTypeCreateHandler } from './handlers';

import {
    ContentTypeListParamsSchema,
    ContentTypeCreateParamsSchema
} from '../../services/contentype';


/**
 * Registers content type tools with the MCP server
 */
export function registerContentTypeTools(server: McpServer) {
    server.registerTool(
        'content_type_list',
        {
            title: 'List Content Types',
            description:
                'Fetches a list of content types from dotCMS. Content types are used to define the structure of content in dotCMS, you can think of them as schemas for content. Can share content types by name.',
            annotations: {
                title: 'List Content Types',
                readOnlyHint: true
            },
            inputSchema: ContentTypeListParamsSchema.shape
        },
        contentTypeListHandler
    );

    server.registerTool(
        'content_type_create',
        {
            title: 'Create Content Type',
            description:
                'Creates a content type in dotCMS. Content types are used to define the structure of content in dotCMS, you can think of them as schemas for content. NOTE: For field types Checkbox, Multi-Select, Radio, and Select, the "value" property is required. The value should be a string with one option per line, each formatted as "Label|value". Example: Pizza|pizza\nChicken|chicken This will create two options for the field.',
            annotations: {
                title: 'Create Content Type',
                readOnlyHint: false,
                idempotentHint: false,
                openWorldHint: true
            },
            inputSchema: z.object({
                contentType: ContentTypeCreateParamsSchema
            }).shape
        },
        contentTypeCreateHandler
    );
}

// Re-export types and handlers for external use if needed
export type { ContentTypeListParams, ContentTypeCreateParams } from './handlers';
