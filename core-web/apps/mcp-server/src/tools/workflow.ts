import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { z } from 'zod';

import { WorkflowService } from '../services/workflow';
import { ContentCreateParamsSchema, ContentActionParamsSchema } from '../types/workflow';
import { Logger } from '../utils/logger';
import { executeWithErrorHandling, createEntitySuccessResponse } from '../utils/response';

const workflowService = new WorkflowService();
const logger = new Logger('WORKFLOW_TOOL');

// Action messages mapping for different workflow actions
const ACTION_MESSAGES = {
    'PUBLISH': 'Content published successfully!',
    'UNPUBLISH': 'Content unpublished successfully!',
    'ARCHIVE': 'Content archived successfully!',
    'UNARCHIVE': 'Content unarchived successfully!',
    'DELETE': 'Content deleted successfully!'
} as const;

export function registerWorkflowTools(server: McpServer) {
    server.registerTool(
        'content_save',
        {
            title: 'Save Content',
            description:
                'Saves content by firing a workflow action. This method creates new content or updates existing content based on the provided parameters. Only contentType and languageId are required fields. All other fields depend on the content type schema.',
            annotations: {
                title: 'Save Content',
                readOnlyHint: false,
                idempotentHint: false,
                openWorldHint: true
            },
            inputSchema: z.object({
                content: ContentCreateParamsSchema,
                comments: z.string().optional()
            }).shape
        },
        async (params) => {
            return executeWithErrorHandling(
                async () => {
                    logger.log('Starting content save tool execution', params);

                    const response = await workflowService.saveContent(params.content, params.comments);
                    const entity = response.entity;

                    logger.log('Content saved successfully', entity);

                    return createEntitySuccessResponse('Content saved successfully!', entity);
                },
                'Error saving content'
            );
        }
    );

    server.registerTool(
        'content_action',
        {
            title: 'Perform Content Action',
            description:
                'Performs content actions (publish/unpublish/archive/unarchive/delete) by firing a workflow action. This method can publish, unpublish, archive, unarchive, or delete existing content based on the provided parameters.',
            annotations: {
                title: 'Perform Content Action',
                readOnlyHint: false,
                idempotentHint: false,
                openWorldHint: true
            },
            inputSchema: ContentActionParamsSchema.shape
        },
        async (params) => {
            return executeWithErrorHandling(
                async () => {
                    logger.log('Starting content action tool execution', params);

                    const response = await workflowService.performContentAction(params);
                    const entity = response.entity;

                    logger.log('Content action performed successfully', entity);

                    const actionMessage = ACTION_MESSAGES[params.action];

                    return createEntitySuccessResponse(actionMessage, entity);
                },
                'Error performing content action'
            );
        }
    );
}
