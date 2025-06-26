import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import { z } from 'zod';

import { ContentTypeService, ContentTypeListParamsSchema, ContentTypeCreateParamsSchema } from './services/contentype';

const server = new McpServer({
  name: 'DotCMS',
  version: '1.0.0',
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

server.registerTool("listContentTypes",
  {
    title: "List Content Types",
    description: "Fetches a list of content types from dotCMS.",
    inputSchema: ContentTypeListParamsSchema.shape
  },
  async (params) => {
    const contentTypes = await contentTypeService.list(params);

    return {
      content: [{
        type: "text",
        text: JSON.stringify(contentTypes, null, 2)
      }]
    };
  }
);

server.registerTool("createContentType",
  {
    title: "Create Content Type",
    description: "Creates one or more content types in dotCMS. Can accept either a single content type object or an array of content types.",
    inputSchema: z.object({
      contentType: z.union([
        ContentTypeCreateParamsSchema,
        z.array(ContentTypeCreateParamsSchema)
      ])
    }).shape
  },
  async (params) => {
    const contentTypes = await contentTypeService.create(params.contentType);

    return {
      content: [{
        type: "text",
        text: JSON.stringify(contentTypes, null, 2)
      }]
    };
  }
);

const transport = new StdioServerTransport();
(async () => {
  await server.connect(transport);
})();


