import { McpServer } from "@modelcontextprotocol/sdk/server/mcp";
import { z } from "zod";

import { ContentTypeService } from "../services/contentype";
import { SiteService } from "../services/site";
import { WorkflowService } from "../services/workflow";
import { ContentType } from "../types/contentype";
import { Site } from "../types/site";
import { WorkflowScheme } from "../types/workflow";
import { formatContentTypesAsText } from "../utils/contenttypes";
import { Logger } from "../utils/logger";
import { executeWithErrorHandling, createSuccessResponse } from "../utils/response";

// Cache for content type schemas to avoid repeated API calls
let contentTypeSchemasCache: ContentType[] | null = null;
let currentSiteCache: Site | null = null;
let workflowSchemesCache: WorkflowScheme[] | null = null;
let cacheTimestamp = 0;
const CACHE_DURATION = 30 * 60 * 1000; // 5 minutes in milliseconds

const contentTypeService = new ContentTypeService();
const siteService = new SiteService();
const workflowService = new WorkflowService();
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

function createResponseText(contentTypes: ContentType[], site: Site, workflowSchemes: WorkflowScheme[], isCached: boolean, cacheAge?: number): string {
    const formattedText = formatContentTypesAsText(contentTypes);
    const workflowText = formatWorkflowSchemesAsText(workflowSchemes);
    const siteInfo = formatSiteInfo(site);
    const cacheInfo = isCached
        ? `[CACHED RESPONSE] (cached for ${cacheAge}s)`
        : `[FRESH RESPONSE] (cached for ${Math.round((Date.now() - cacheTimestamp) / 1000)}s)`;

    return `${cacheInfo} ${siteInfo}:\n\n${formattedText}\n\n${workflowText}`;
}

function formatWorkflowSchemesAsText(workflowSchemes: WorkflowScheme[]): string {
    if (workflowSchemes.length === 0) {
        return 'No workflow schemes found.';
    }

    const schemesText = workflowSchemes.map(scheme => {
        const status = scheme.archived ? '[ARCHIVED]' : '[ACTIVE]';
        const systemFlag = scheme.system ? ' (SYSTEM)' : '';
        const defaultFlag = scheme.defaultScheme ? ' (DEFAULT)' : '';

        return `${status} ${scheme.name}${systemFlag}${defaultFlag}
  ID: ${scheme.id}
  Variable Name: ${scheme.variableName}
  Description: ${scheme.description || 'No description'}
  Mandatory: ${scheme.mandatory}
  Creation Date: ${new Date(scheme.creationDate).toISOString()}
  Modified Date: ${new Date(scheme.modDate).toISOString()}`;
    }).join('\n\n');

    return `WORKFLOW SCHEMES (${workflowSchemes.length} total):

${schemesText}`;
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
            return executeWithErrorHandling(
                async () => {
                    logger.log('Starting context initialization tool execution');
                    const now = Date.now();

                    // Return cached data if it's still valid
                    if (contentTypeSchemasCache && currentSiteCache && workflowSchemesCache && (now - cacheTimestamp) < CACHE_DURATION) {
                        const cacheAge = Math.round((now - cacheTimestamp) / 1000);
                        logger.log('Returning cached context data', {
                            cacheAge,
                            contentTypeCount: contentTypeSchemasCache.length,
                            workflowSchemeCount: workflowSchemesCache.length
                        });

                        const responseText = createResponseText(contentTypeSchemasCache, currentSiteCache, workflowSchemesCache, true, cacheAge);

                        return createSuccessResponse(responseText);
                    }

                    // Fetch fresh data
                    logger.log('Cache miss or expired, fetching fresh context data');

                    const [contentTypes, currentSite, workflowSchemes] = await Promise.all([
                        contentTypeService.getContentTypesSchema(),
                        siteService.getCurrentSite(),
                        workflowService.getWorkflowSchemes()
                    ]);

                    logger.log('Fresh context data fetched successfully', {
                        contentTypeCount: contentTypes.length,
                        siteName: currentSite.name,
                        workflowSchemeCount: workflowSchemes.entity.length
                    });

                    contentTypeSchemasCache = contentTypes;
                    currentSiteCache = currentSite;
                    workflowSchemesCache = workflowSchemes.entity;
                    cacheTimestamp = now;

                    const responseText = createResponseText(contentTypes, currentSite, workflowSchemes.entity, false);

                    return createSuccessResponse(responseText);
                },
                'Error fetching context data'
            );
        }
    );
}
