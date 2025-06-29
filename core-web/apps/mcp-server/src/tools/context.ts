import { McpServer } from "@modelcontextprotocol/sdk/server/mcp";
import { z } from "zod";

import { ContentTypeService } from "../services/contentype";
import { SiteService } from "../services/site";
import { ContentType } from "../types/contentype";
import { Site } from "../types/site";
import { formatContentTypesAsText } from "../utils/contenttypes";
import { Logger } from "../utils/logger";

// Cache for content type schemas to avoid repeated API calls
let contentTypeSchemasCache: ContentType[] | null = null;
let currentSiteCache: Site | null = null;
let cacheTimestamp = 0;
const CACHE_DURATION = 30 * 60 * 1000; // 5 minutes in milliseconds

const contentTypeService = new ContentTypeService();
const siteService = new SiteService();
const logger = new Logger('CONTEXT_TOOL');

function formatSiteInfo(site: Site): string {
    return `Current Site: ${site.name} (${site.hostname})

IMPORTANT: When creating or updating content, use this current site's identifier (${site.identifier}) for any host or site fields. This ensures content is properly associated with the current site.

TIPS:

1. Url to edit a piece of content after creation:
https://<dotcms-url>/dotAdmin/#/c/content/<content-inode>/

2. Url to edit a content type after creation:
https://<dotcms-url>dotAdmin/#/content-types-angular/edit/<content-type-identifier>
`;
}

function createResponseText(contentTypes: ContentType[], site: Site, isCached: boolean, cacheAge?: number): string {
    const formattedText = formatContentTypesAsText(contentTypes);
    const siteInfo = formatSiteInfo(site);
    const cacheInfo = isCached
        ? `[CACHED RESPONSE] (cached for ${cacheAge}s)`
        : `[FRESH RESPONSE] (cached for ${Math.round((Date.now() - cacheTimestamp) / 1000)}s)`;

    return `${cacheInfo} ${siteInfo}:\n\n${formattedText}`;
}

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
            logger.log('Starting context initialization tool execution');
            const now = Date.now();

            // Return cached data if it's still valid
            if (contentTypeSchemasCache && currentSiteCache && (now - cacheTimestamp) < CACHE_DURATION) {
                const cacheAge = Math.round((now - cacheTimestamp) / 1000);
                logger.log('Returning cached context data', {
                    cacheAge,
                    contentTypeCount: contentTypeSchemasCache.length
                });

                return {
                    content: [
                        {
                            type: 'text',
                            text: createResponseText(contentTypeSchemasCache, currentSiteCache, true, cacheAge)
                        }
                    ]
                };
            }

            // Fetch fresh data
            try {
                logger.log('Cache miss or expired, fetching fresh context data');

                const [contentTypes, currentSite] = await Promise.all([
                    contentTypeService.getContentTypesSchema(),
                    siteService.getCurrentSite()
                ]);

                logger.log('Fresh context data fetched successfully', {
                    contentTypeCount: contentTypes.length,
                    siteName: currentSite.name
                });

                contentTypeSchemasCache = contentTypes;
                currentSiteCache = currentSite;
                cacheTimestamp = now;

                return {
                    content: [
                        {
                            type: 'text',
                            text: createResponseText(contentTypes, currentSite, false)
                        }
                    ]
                };
            } catch (error) {
                logger.error('Error fetching context data', error);

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
