import { z } from 'zod';

import { formatContentTypesAsText } from './formatters';

import {
    ContentTypeService,
    ContentTypeListParamsSchema,
    ContentTypeCreateParamsSchema
} from '../../services/contentType';
import { Logger } from '../../utils/logger';
import { executeWithErrorHandling, createSuccessResponse } from '../../utils/response';

const contentTypeService = new ContentTypeService();
const logger = new Logger('CONTENT_TYPES_TOOL');

// Type definitions for handlers
export type ContentTypeListParams = z.infer<typeof ContentTypeListParamsSchema>;

export type ContentTypeCreateParams = {
    contentType: z.infer<typeof ContentTypeCreateParamsSchema>;
};

/**
 * Handles content type list operations
 */
export async function contentTypeListHandler(params: ContentTypeListParams) {
    return executeWithErrorHandling(async () => {
        logger.log('Starting content type list tool execution', params);

        const contentTypes = await contentTypeService.list(params);
        const formattedText = formatContentTypesAsText(contentTypes);

        logger.log('Content types listed successfully', { count: contentTypes.length });

        return createSuccessResponse(formattedText);
    }, 'Error fetching content type schemas');
}

/**
 * Handles content type creation operations
 */
export async function contentTypeCreateHandler(params: ContentTypeCreateParams) {
    return executeWithErrorHandling(async () => {
        logger.log('Starting content type creation tool execution', params);

        const contentTypes = await contentTypeService.create(params.contentType);

        logger.log('Content type created successfully', { count: contentTypes.length });

        return createSuccessResponse(formatContentTypesAsText(contentTypes));
    }, 'Error creating content type');
}
