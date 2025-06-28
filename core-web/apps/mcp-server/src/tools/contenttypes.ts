import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { z } from 'zod';

import {
    ContentTypeService,
    ContentTypeListParamsSchema,
    ContentTypeCreateParamsSchema
} from '../services/contentype';
import { ContentType } from '../types/contentype';
import { formatContentTypesAsText } from '../utils/contenttypes';

// Cache for content type schemas to avoid repeated API calls
let contentTypeSchemasCache: ContentType[] | null = null;
let cacheTimestamp = 0;
const CACHE_DURATION = 30 * 60 * 1000; // 5 minutes in milliseconds

const contentTypeService = new ContentTypeService();

export function registerContentTypeTools(server: McpServer) {
    server.registerTool(
        'content_type_discovery',
        {
            title: 'Content Type Discovery',
            description:
                'IMPORTANT: This tool MUST be called FIRST before any other operations to learn all available content type schemas in the dotCMS instance. This provides the LLM with complete knowledge of all content types, their fields, field types, and structure. The response is cached for 5 minutes to avoid repeated API calls. Use this to understand what content types exist and their complete field definitions before creating or working with content.',
            annotations: {
                title: 'Content Type Discovery',
                readOnlyHint: true
            },
            inputSchema: z.object({}).shape
        },
        async () => {
            const now = Date.now();

            // Return cached data if it's still valid
            if (contentTypeSchemasCache && (now - cacheTimestamp) < CACHE_DURATION) {
                const formattedText = formatContentTypesAsText(contentTypeSchemasCache);

                return {
                    content: [
                        {
                            type: 'text',
                            text: `[CACHED RESPONSE] Content Type Schemas (cached for ${Math.round((now - cacheTimestamp) / 1000)}s):\n\n${formattedText}`
                        }
                    ]
                };
            }

            // Fetch fresh data
            try {
                contentTypeSchemasCache = await contentTypeService.getContentTypesSchema();
                cacheTimestamp = now;
                const formattedText = formatContentTypesAsText(contentTypeSchemasCache);

                return {
                    content: [
                        {
                            type: 'text',
                            text: `Content Type Schemas (${contentTypeSchemasCache.length} content types found):\n\n${formattedText}`
                        }
                    ]
                };
            } catch (error) {
                return {
                    content: [
                        {
                            type: 'text',
                            text: `Error fetching content type schemas: ${error instanceof Error ? error.message : String(error)}`
                        }
                    ]
                };
            }
        }
    );

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
            const contentTypes = await contentTypeService.list(params);
            const formattedText = formatContentTypesAsText(contentTypes);

            return {
                content: [
                    {
                        type: 'text',
                        text: formattedText
                    }
                ]
            };
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
            const contentTypes = await contentTypeService.create(params.contentType);

            return {
                content: [
                    {
                        type: 'text',
                        text: formatContentTypesAsText(contentTypes)
                    }
                ]
            };
        }
    );
}
