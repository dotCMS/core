import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { z } from 'zod';

import {
    ContentTypeService,
    ContentTypeListParamsSchema,
    ContentTypeCreateParamsSchema
} from '../services/contentype';
import { formatContentTypesAsText } from '../utils/contenttypes';
import { Logger } from '../utils/logger';
import { executeWithErrorHandling, createSuccessResponse } from '../utils/response';

const contentTypeService = new ContentTypeService();
const logger = new Logger('CONTENT_TYPES_TOOL');

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
        async (params) => {
            return executeWithErrorHandling(
                async () => {
                    logger.log('Starting content type list tool execution', params);

                    const contentTypes = await contentTypeService.list(params);
                    const formattedText = formatContentTypesAsText(contentTypes);

                    logger.log('Content types listed successfully', { count: contentTypes.length });

                    return createSuccessResponse(formattedText);
                },
                'Error fetching content type schemas'
            );
        }
    );

    server.registerTool(
        'content_type_create',
        {
            title: 'Create Content Type',
            description:
                'Creates a content type in dotCMS. Content types are used to define the structure of content in dotCMS, you can think of them as schemas for content.',
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
        async (params) => {
            return executeWithErrorHandling(
                async () => {
                    logger.log('Starting content type creation tool execution', params);

                    const contentTypes = await contentTypeService.create(params.contentType);

                    logger.log('Content type created successfully', { count: contentTypes.length });

                    return createSuccessResponse(formatContentTypesAsText(contentTypes));
                },
                'Error creating content type'
            );
        }
    );
}
