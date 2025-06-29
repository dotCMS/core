import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { z } from 'zod';

import { WorkflowService } from '../services/workflow';
import { ContentCreateParamsSchema } from '../types/workflow';

const workflowService = new WorkflowService();

const log = (message: string, data?: unknown) => {
    const timestamp = new Date().toISOString();
    const logMessage = `[${timestamp}] ${message}${data ? '\n' + JSON.stringify(data, null, 2) : ''}\n`;
    process.stderr.write(logMessage);
};

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
                const response = await workflowService.saveContent(params.content, params.comments);
                const entity = response.entity;

                log('SAVED CONTENT SUCCESSFULLY ===============================', entity);

                return {
                    content: [
                        {
                            type: 'text',
                            text: `Content saved successfully!\n\nIdentifier: ${entity.identifier}\nInode: ${entity.inode}\nContent Type: ${entity.contentType}\nLanguage ID: ${entity.languageId}`
                        }
                    ]
                };
            } catch (error) {
                log('ERROR SAVING CONTENT ===============================', error);

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
}
