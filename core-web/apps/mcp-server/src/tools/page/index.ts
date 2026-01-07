import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { z } from 'zod';

import {
    renderPageHtmlAndAnalyzeWcagHandler,
    renderPageHtmlAndAnalyzeSeoHandler,
    renderPageHtmlAndAnalyzeGeoHandler
} from './handlers';

// Base schema used by all page tools
const BasePageInputSchema = z.object({
    uri: z.string().min(1, 'Page URI is required').describe('Page URI, e.g. / or /about-us')
});

// WCAG-specific schema extends the base with optional WCAG level
const WcagAnalyzeInputSchema = BasePageInputSchema.extend({
    wcag_level: z
        .enum(['A', 'AA', 'AAA'])
        .optional()
        .default('A')
        .describe('WCAG compliance level to check against (default: A)')
});

export type PageUriInput = z.infer<typeof BasePageInputSchema>;
export type WcagAnalyzeInput = z.infer<typeof WcagAnalyzeInputSchema>;
// Handler convenience type: accepts uri and optional wcag_level
export type AnalyzePageParams = PageUriInput & { wcag_level?: 'A' | 'AA' | 'AAA' };

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
            inputSchema: WcagAnalyzeInputSchema.shape
        },
        renderPageHtmlAndAnalyzeWcagHandler
    );

    server.registerTool(
        'analyze_dotcms_page_seo',
        {
            title: 'Render Page HTML and Analyze SEO',
            description: `
            Authoritative tool for analyzing SEO of dotCMS pages.
    
            This tool MUST be used to analyze SEO for dotCMS pages.
            Do NOT fetch pages via a browser or external HTTP client.
    
            The tool renders the page using dotCMS server-side rendering
            (including Velocity, containers, and personalization),
            then analyzes the resulting HTML for SEO best practices
            and search engine optimization issues.
    
            Trigger:
            Use this tool whenever the user provides a dotCMS page path
            and asks for SEO, search ranking, metadata, or indexability analysis.
    
            Use this tool whenever the user asks to:
            - analyze SEO for a dotCMS page
            - check metadata, titles, headings, or links
            - evaluate search engine readiness
            - scan a page by URI or path for SEO issues
    
            Parameters:
            - uri: Page URI (required), e.g. / or /about-us
    
            Notes:
            - Pages may not be publicly accessible
            - External page fetching will produce incorrect results
            - This tool is read-only and idempotent
            `.trim(),
            annotations: {
                title: 'Render Page HTML and Analyze SEO',
                readOnlyHint: true,
                idempotentHint: true,
                openWorldHint: false
            },
            inputSchema: BasePageInputSchema.shape
        },
        renderPageHtmlAndAnalyzeSeoHandler
    );

    server.registerTool(
        'analyze_dotcms_page_geo',
        {
            title: 'Render Page HTML and Analyze GEO',
            description: `
            Authoritative tool for analyzing Generative Engine Optimization (GEO)
            of dotCMS pages.
    
            This tool MUST be used to analyze GEO for dotCMS pages.
            Do NOT fetch pages via a browser or external HTTP client.
    
            The tool renders the page using dotCMS server-side rendering
            (including Velocity, containers, and personalization),
            then analyzes the resulting HTML for suitability as a
            generative AI knowledge source.
    
            GEO analysis evaluates:
            - Semantic clarity and structure
            - Entity definition and reinforcement
            - Answerability and extractability
            - Content chunking and hierarchy
            - Signals that improve LLM understanding and citation
    
            Trigger:
            Use this tool whenever the user provides a dotCMS page path
            and asks about:
            - GEO or Generative Engine Optimization
            - AI search readiness
            - LLM visibility or citation likelihood
            - optimization for AI answers or summaries
    
            Parameters:
            - uri: Page URI (required), e.g. / or /about-us
    
            Notes:
            - Pages may not be publicly accessible
            - External page fetching will produce incorrect results
            - This tool is read-only and idempotent
            `.trim(),
            annotations: {
                title: 'Render Page HTML and Analyze GEO',
                readOnlyHint: true,
                idempotentHint: true,
                openWorldHint: false
            },
            inputSchema: BasePageInputSchema.shape
        },
        renderPageHtmlAndAnalyzeGeoHandler
    );
}


