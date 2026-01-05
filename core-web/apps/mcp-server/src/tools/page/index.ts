import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { z } from 'zod';

import { renderPageHtmlHandler } from './handlers';

const RenderPageInputSchema = z.object({
    uri: z
        .string()
        .min(1, 'Page URI is required')
        .describe('Page URI, e.g. / or /about-us'),
    wcag_level: z
        .enum(['A', 'AA', 'AAA'])
        .optional()
        .default('A')
        .describe('WCAG compliance level to check against (default: A)')
});

export type RenderPageInput = z.infer<typeof RenderPageInputSchema>;

/**
 * Registers page-related MCP tools (rendering and analysis)
 */
export function registerPageTools(server: McpServer) {
    server.registerTool(
        'analyze_dotcms_page_wcag',
        {
            title: 'Render Page HTML and Analyze WCAG Compliance',
            description: `
            Authoritative tool for analyzing WCAG compliance of dotCMS pages.
            
            This tool MUST be used to analyze dotCMS pages.
            Do NOT fetch pages via a browser or external HTTP client.
            
            The tool renders the page using dotCMS server-side rendering
            (including Velocity, containers, and personalization),
            then analyzes the resulting HTML for WCAG compliance issues.
            
            Trigger:
            Use this tool whenever the user provides a dotCMS page path
            and asks for WCAG, accessibility, or compliance analysis.

            Use this tool whenever the user asks to:
            - analyze a dotCMS page
            - check WCAG or accessibility compliance
            - scan a page by URI or path
            
            Parameters:
            - uri: Page URI (required), e.g. / or /about-us
            - wcag_level (optional): A (default), AA, AAA
            
            Notes:
            - Pages may not be publicly accessible
            - External page fetching will produce incorrect results
            - This tool is read-only and idempotent
            `.trim(),
            annotations: {
                title: 'Render Page HTML and Analyze WCAG',
                readOnlyHint: true,
                idempotentHint: true,
                openWorldHint: false
            },
            inputSchema: RenderPageInputSchema.shape
        },
        renderPageHtmlHandler
    );
}


