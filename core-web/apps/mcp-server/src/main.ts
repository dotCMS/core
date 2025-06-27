import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import { z } from 'zod';

import {
    ContentTypeService,
    ContentTypeListParamsSchema,
    ContentTypeCreateParamsSchema
} from './services/contentype';

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

server.registerTool(
    'listContentTypes',
    {
        title: 'List Content Types',
        description: 'Fetches a list of content types from dotCMS. Content types are used to define the structure of content in dotCMS, you can think of them as schemas for content. Can share content types by name.',
        annotations: {
            title: 'List Content Types',
            readOnlyHint: true,
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
    'createContentType',
    {
        title: 'Create Content Type',
        description: 'Creates a content type in dotCMS. Content types are used to define the structure of content in dotCMS, you can think of them as schemas for content.',
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
