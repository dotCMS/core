import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import { z } from 'zod';

const server = new McpServer({
  name: 'DotCMS',
  version: '1.0.0',
});

server.registerTool("hello",
  {
    title: "Hello Tool",
    description: "Say hello to someone",
    inputSchema: { name: z.string() }
  },
  async ({ name }) => ({
    content: [{ type: "text", text: `Hello ${name}` }]
  })
);

const transport = new StdioServerTransport();
(async () => {
  await server.connect(transport);
})();


