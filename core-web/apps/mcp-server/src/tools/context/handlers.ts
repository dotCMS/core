import { getCachedData, setCacheData } from './cache';
import { createResponseText } from './formatters';

import { ContentTypeService } from '../../services/contentType';
import { SiteService } from '../../services/site';
import { WorkflowService } from '../../services/workflow';
import { getContextStore } from '../../utils/context-store';
import { Logger } from '../../utils/logger';
import { executeWithErrorHandling, createSuccessResponse } from '../../utils/response';

const contentTypeService = new ContentTypeService();
const siteService = new SiteService();
const workflowService = new WorkflowService();
const logger = new Logger('CONTEXT_TOOL');
const contextStore = getContextStore();

/**
 * Handles context initialization operations
 * Fetches and caches content types, site info, and workflow schemes
 */
export async function contextInitializationHandler() {
    return executeWithErrorHandling(async () => {
        logger.log('Starting context initialization tool execution');

        // Check for cached data first
        const cachedData = getCachedData();
        if (cachedData) {
            logger.log('Returning cached context data', {
                cacheAge: cachedData.age,
                contentTypeCount: cachedData.data.contentTypes.length,
                workflowSchemeCount: cachedData.data.workflowSchemes.length
            });

            // Set context as initialized since we have valid cached data
            contextStore.setInitialized();

            const responseText = createResponseText(
                cachedData.data.contentTypes,
                cachedData.data.site,
                cachedData.data.workflowSchemes,
                true,
                cachedData.age
            );

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

        // Cache the fresh data
        setCacheData(contentTypes, currentSite, workflowSchemes.entity);

        // Set context as initialized since we successfully fetched fresh data
        contextStore.setInitialized();

        const responseText = createResponseText(
            contentTypes,
            currentSite,
            workflowSchemes.entity,
            false
        );

        return createSuccessResponse(responseText);
    }, 'Error fetching context data');
}
