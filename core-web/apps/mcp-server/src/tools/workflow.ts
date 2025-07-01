import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { z } from 'zod';

import { WorkflowService } from '../services/workflow';
import { ContentCreateParamsSchema, ContentActionParamsSchema } from '../types/workflow';
import { Logger } from '../utils/logger';

const workflowService = new WorkflowService();
const logger = new Logger('WORKFLOW_TOOL');

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
            try {
                logger.log('Starting content save tool execution', params);

                const response = await workflowService.saveContent(params.content, params.comments);
                const entity = response.entity;

                logger.log('Content saved successfully', entity);

                return {
                    content: [
                        {
                            type: 'text',
                            text: `Content saved successfully!\n\nIdentifier: ${entity.identifier}\nInode: ${entity.inode}\nContent Type: ${entity.contentType}\nLanguage ID: ${entity.languageId}`
                        }
                    ]
                };
            } catch (error) {
                logger.error('Error saving content', error);

                return {
                    isError: true,
                    content: [
                        {
                            type: 'text',
                            text: `Error saving content: ${JSON.stringify(error, null, 2)}`
                        }
                    ]
                };
            }
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
            try {
                logger.log('Starting content action tool execution', params);

                const response = await workflowService.performContentAction(params);
                const entity = response.entity;

                logger.log('Content action performed successfully', entity);

                const actionMessages = {
                    'PUBLISH': 'Content published successfully!',
                    'UNPUBLISH': 'Content unpublished successfully!',
                    'ARCHIVE': 'Content archived successfully!',
                    'UNARCHIVE': 'Content unarchived successfully!',
                    'DELETE': 'Content deleted successfully!'
                };

                return {
                    content: [
                        {
                            type: 'text',
                            text: `${actionMessages[params.action]}\n\nIdentifier: ${entity.identifier}\nInode: ${entity.inode}\nContent Type: ${entity.contentType}\nLanguage ID: ${entity.languageId}`
                        }
                    ]
                };
            } catch (error) {
                logger.error('Error performing content action', error);

                return {
                    isError: true,
                    content: [
                        {
                            type: 'text',
                            text: `Error performing content action: ${JSON.stringify(error, null, 2)}`
                        }
                    ]
                };
            }
        }
    );
}
