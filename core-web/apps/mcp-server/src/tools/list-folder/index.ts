import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { z } from 'zod';

import { listFolderContentsHandler } from './handlers';

const ListFolderInputSchema = z.object({
    folder: z
        .string()
        .min(1, 'Folder path is required')
        .describe('Folder path on the site (e.g., /, /images, /docs)'),
    store_raw: z
        .boolean()
        .optional()
        .default(true)
        .describe('Whether to store raw search JSON in context for later filtering'),
    context_key: z
        .string()
        .optional()
        .describe('Optional explicit context key under which to store raw results'),
    limit: z.number().int().positive().max(1000).optional().default(100),
    offset: z.number().int().min(0).optional().default(0)
});

export type ListFolderInput = z.infer<typeof ListFolderInputSchema>;

export function registerListFolderTools(server: McpServer) {
    server.registerTool(
        'list_folder_contents',
        {
            title: 'List Folder Contents',
            description: `
Lists the content items contained in a specific folder path on the current site.

Parameters:
- folder: Folder path (e.g., /, /images, /docs)
- limit (optional): Max number of items to return (default 100, max 1000)
- offset (optional): Pagination offset (default 0)
- store_raw (optional, default true): Store the full raw JSON response in context for LLM-side filtering
- context_key (optional): Key name to store/retrieve the raw results for subsequent filtering

Notes:
- Results include contentlets found at the exact folder path (no recursion).
- Advanced filtering should be performed by the LLM using the raw JSON stored in context (when store_raw is true), not by issuing additional searches.
            `.trim(),
            annotations: {
                title: 'List Folder Contents',
                readOnlyHint: true,
                idempotentHint: true,
                openWorldHint: false
            },
            inputSchema: ListFolderInputSchema.shape
        },
        listFolderContentsHandler
    );
}