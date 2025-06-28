import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import { z } from 'zod';

import {
    ContentTypeService,
    ContentTypeListParamsSchema,
    ContentTypeCreateParamsSchema
} from './services/contentype';
import { ContentType } from './types/contentype';

const server = new McpServer({
    name: 'DotCMS',
    version: '1.0.0'
});

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

const contentTypeService = new ContentTypeService();

// Cache for content type schemas to avoid repeated API calls
let contentTypeSchemasCache: ContentType[] | null = null;
let cacheTimestamp = 0;
const CACHE_DURATION = 30 * 60 * 1000; // 5 minutes in milliseconds

server.registerTool(
    'learn_content_type_schemas',
    {
        title: 'Learn Content Type Schemas',
        description:
            'IMPORTANT: This tool MUST be called FIRST before any other operations to learn all available content type schemas in the dotCMS instance. This provides the LLM with complete knowledge of all content types, their fields, field types, and structure. The response is cached for 5 minutes to avoid repeated API calls. Use this to understand what content types exist and their complete field definitions before creating or working with content.',
        annotations: {
            title: 'Learn Content Type Schemas',
            readOnlyHint: true
        },
        inputSchema: z.object({}).shape
    },
    async () => {
        const now = Date.now();

        // Return cached data if it's still valid
        if (contentTypeSchemasCache && (now - cacheTimestamp) < CACHE_DURATION) {
            return {
                content: [
                    {
                        type: 'text',
                        text: `[CACHED RESPONSE] Content Type Schemas (cached for ${Math.round((now - cacheTimestamp) / 1000)}s):\n\n${JSON.stringify(contentTypeSchemasCache, null, 2)}`
                    }
                ]
            };
        }

        // Fetch fresh data
        try {
            contentTypeSchemasCache = await contentTypeService.getContentTypesSchema();
            cacheTimestamp = now;

            return {
                content: [
                    {
                        type: 'text',
                        text: `Content Type Schemas (${contentTypeSchemasCache.length} content types found):\n\n${JSON.stringify(contentTypeSchemasCache, null, 2)}`
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

        return {
            content: [
                {
                    type: 'text',
                    text: JSON.stringify(contentTypes, null, 2)
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
                    text: JSON.stringify(contentTypes, null, 2)
                }
            ]
        };
    }
);

const transport = new StdioServerTransport();
(async () => {
    await server.connect(transport);
})();
