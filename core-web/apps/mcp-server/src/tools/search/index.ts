import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';

import { searchDescription } from './description';
import { contentSearchHandler } from './handlers';

import { SearchFormSchema } from '../../services/search';
import { asMcpSchema } from '../../utils/schema-helpers';

/**
 * Registers content search tool with the MCP server
 */
export function registerSearchTools(server: McpServer) {
    server.registerTool(
        'content_search',
        {
            title: 'Search Content',
            description: searchDescription,
            annotations: {
                title: 'Search Content',
                readOnlyHint: true
            },
            inputSchema: asMcpSchema(SearchFormSchema)
        },
        contentSearchHandler
    );
}
