import { AgnosticClient } from './client';

import {
    ContentCreateParams,
    WorkflowActionRequest,
    WorkflowActionResponse,
    WorkflowActionRequestSchema,
    WorkflowActionResponseSchema,
    ContentCreateParamsSchema
} from '../types/workflow';

const log = (message: string, data?: unknown) => {
    const timestamp = new Date().toISOString();
    const logMessage = `[${timestamp}] ${message}${data ? '\n' + JSON.stringify(data, null, 2) : ''}\n`;
    process.stderr.write(logMessage);
};

export class WorkflowService extends AgnosticClient {
    constructor() {
        super();
    }

    /**
     * Saves content by firing a workflow action.
     * This method creates new content or updates existing content based on the provided parameters.
     *
     * @param params - Content creation/update parameters
     * @param comments - Optional comments for the workflow action
     * @returns Promise with the workflow action response
     */
    async saveContent(
        params: ContentCreateParams,
        comments?: string
    ): Promise<WorkflowActionResponse> {
        const validatedParams = ContentCreateParamsSchema.safeParse(params);

        if (!validatedParams.success) {
            throw new Error(
                'Invalid content parameters: ' + JSON.stringify(validatedParams.error.format())
            );
        }

        const workflowRequest: WorkflowActionRequest = {
            actionName: 'save',
            comments: comments || 'Saving content via API',
            contentlet: validatedParams.data
        };

        const validated = WorkflowActionRequestSchema.safeParse(workflowRequest);
        if (!validated.success) {
            throw new Error(
                'Invalid workflow request: ' + JSON.stringify(validated.error.format())
            );
        }

        // Use FormData to match curl -F behavior
        const formData = new FormData();
        formData.append('json', JSON.stringify(validated.data));

        // Add dummy file like curl
        // const dummyFile = new Blob(['dummy content'], { type: 'text/plain' });
        // formData.append('file', dummyFile, 'dummy.txt');

        // Create a proper File object instead of Blob
        const dummyFile = new File(['dummy content'], 'dummy.txt', {
            type: 'text/plain'
        });
        formData.append('file', dummyFile);

        const url = `${this.dotcmsUrl}/api/v1/workflow/actions/fire`;
        const response = await this.fetch(url, {
            method: 'PUT',
            body: formData,
            headers: {
                Authorization: `Bearer ${this.authToken}`
            }
        });

        if (!response.ok) {
            log('WORKFLOW ACTION RESPONSE ERROR 1 ===============================', response);
            throw new Error(`Failed to save content: ${response.status} ${response.statusText}`);
        }

        const data = await response.json();

        const parsed = WorkflowActionResponseSchema.safeParse(data);

        if (!parsed.success) {
            log('WORKFLOW ACTION RESPONSE ERROR ===============================', parsed.error);
            throw new Error('Invalid workflow response: ' + JSON.stringify(parsed.error.format()));
        }

        // Log to stderr to avoid interfering with MCP protocol
        log('WORKFLOW ACTION RESPONSE SUCCESS ===============================', parsed.data);

        return parsed.data;
    }
}
