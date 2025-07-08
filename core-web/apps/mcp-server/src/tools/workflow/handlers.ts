import { z } from 'zod';

import { ACTION_MESSAGES } from './constants';
import { createEntitySuccessResponse } from './utils';

import { WorkflowService } from '../../services/workflow';
import { ContentCreateParamsSchema, ContentActionParamsSchema } from '../../types/workflow';
import { Logger } from '../../utils/logger';
import { executeWithErrorHandling } from '../../utils/response';

const workflowService = new WorkflowService();
const logger = new Logger('WORKFLOW_TOOL');

// Type definitions for handlers
export type ContentSaveParams = {
    content: z.infer<typeof ContentCreateParamsSchema>;
    comments?: string;
};

export type ContentActionParams = z.infer<typeof ContentActionParamsSchema>;

/**
 * Handles content save operations
 */
export async function contentSaveHandler(params: ContentSaveParams) {
    return executeWithErrorHandling(async () => {
        logger.log('Starting content save tool execution', params);

        const response = await workflowService.saveContent(params.content, params.comments);
        const entity = response.entity;

        logger.log('Content saved successfully', entity);

        return createEntitySuccessResponse('Content saved successfully!', entity);
    }, 'Error saving content');
}

/**
 * Handles content action operations (publish, unpublish, etc.)
 */
export async function contentActionHandler(params: ContentActionParams) {
    return executeWithErrorHandling(async () => {
        logger.log('Starting content action tool execution', params);

        const response = await workflowService.performContentAction(params);
        const entity = response.entity;

        logger.log('Content action performed successfully', entity);

        const actionMessage = ACTION_MESSAGES[params.action];

        return createEntitySuccessResponse(actionMessage, entity);
    }, 'Error performing content action');
}
