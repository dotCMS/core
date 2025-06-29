import { McpServer } from "@modelcontextprotocol/sdk/server/mcp";
import { z } from "zod";

import { ContentTypeService } from "../services/contentype";
import { SiteService } from "../services/site";
import { ContentType } from "../types/contentype";
import { Site } from "../types/site";
import { formatContentTypesAsText } from "../utils/contenttypes";

// Cache for content type schemas to avoid repeated API calls
let contentTypeSchemasCache: ContentType[] | null = null;
let currentSiteCache: Site | null = null;
let cacheTimestamp = 0;
const CACHE_DURATION = 30 * 60 * 1000; // 5 minutes in milliseconds

const contentTypeService = new ContentTypeService();
const siteService = new SiteService();

export function registerContextTools(server: McpServer) {
    server.registerTool(
        'context_initialization',
        {
            title: 'Context Initialization',
            description:
                'IMPORTANT: This tool MUST be called FIRST before any other operations to learn all available content type schemas and current site information in the dotCMS instance. This provides the LLM with complete knowledge of all content types, their fields, field types, structure, and the current site details. The response is cached for 5 minutes to avoid repeated API calls. Use this to understand what content types exist and their complete field definitions before creating or working with content.',
            annotations: {
                title: 'Context Initialization',
                readOnlyHint: true
            },
            inputSchema: z.object({}).shape
        },
        async () => {
            const now = Date.now();

            // Return cached data if it's still valid
            if (contentTypeSchemasCache && currentSiteCache && (now - cacheTimestamp) < CACHE_DURATION) {
                const formattedText = formatContentTypesAsText(contentTypeSchemasCache);
                const siteInfo = `Current Site: ${currentSiteCache.name} (${currentSiteCache.hostname})

IMPORTANT: When creating or updating content, use this current site's identifier (${currentSiteCache.identifier}) for any host or site fields. This ensures content is properly associated with the current site.`;

                return {
                    content: [
                        {
                            type: 'text',
                            text: `[CACHED RESPONSE] ${siteInfo} (cached for ${Math.round((now - cacheTimestamp) / 1000)}s):\n\n${formattedText}`
                        }
                    ]
                };
            }

            // Fetch fresh data
            try {
                const [contentTypes, currentSite] = await Promise.all([
                    contentTypeService.getContentTypesSchema(),
                    siteService.getCurrentSite()
                ]);

                contentTypeSchemasCache = contentTypes;
                currentSiteCache = currentSite;
                cacheTimestamp = now;

                const formattedText = formatContentTypesAsText(contentTypes);
                const siteInfo = `Current Site: ${currentSite.name} (${currentSite.hostname})

IMPORTANT: When creating or updating content, use this current site's identifier (${currentSite.identifier}) for any host or site fields. This ensures content is properly associated with the current site.`;

                return {
                    content: [
                        {
                            type: 'text',
                            text: `[FRESH RESPONSE] ${siteInfo} (cached for ${Math.round((now - cacheTimestamp) / 1000)}s):\n\n${formattedText}`
                        }
                    ]
                };
            } catch (error) {
                return {
                    isError: true,
                    content: [
                        {
                            type: 'text',
                            text: `Error fetching context data: ${error instanceof Error ? error.message : String(error)}`
                        }
                    ]
                };
            }
        }
    );
}
