import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import { z } from 'zod';

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

server.registerTool("hello",
  {
    title: "Hello Tool",
    description: "Say hello to someone",
    inputSchema: { name: z.string() }
  },
  async ({ name }) => ({
    content: [{
      type: "text",
      text: `Hello ${name}\nDOTCMS_URL: ${DOTCMS_URL}\nAUTH_TOKEN: ${AUTH_TOKEN}`
    }]
  })
);

const transport = new StdioServerTransport();
(async () => {
  await server.connect(transport);
})();


