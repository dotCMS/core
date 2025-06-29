import { AgnosticClient } from './client';

import {
    ContentCreateParams,
    WorkflowActionRequest,
    WorkflowActionResponse,
    WorkflowActionRequestSchema,
    WorkflowActionResponseSchema,
    ContentCreateParamsSchema
} from '../types/workflow';
import { Logger } from '../utils/logger';

export class WorkflowService extends AgnosticClient {
    private logger: Logger;

    constructor() {
        super();
        this.logger = new Logger('WORKFLOW_SERVICE');
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
        this.logger.log('Starting content save operation', { params, comments });

        const validatedParams = ContentCreateParamsSchema.safeParse(params);

        if (!validatedParams.success) {
            this.logger.error('Invalid content parameters', validatedParams.error);
            throw new Error(
                'Invalid content parameters: ' + JSON.stringify(validatedParams.error.format())
            );
        }

        this.logger.log('Content parameters validated successfully', validatedParams.data);

        const workflowRequest: WorkflowActionRequest = {
            actionName: 'save',
            comments: comments || 'Saving content via API',
            contentlet: validatedParams.data
        };

        const validated = WorkflowActionRequestSchema.safeParse(workflowRequest);
        if (!validated.success) {
            this.logger.error('Invalid workflow request', validated.error);
            throw new Error(
                'Invalid workflow request: ' + JSON.stringify(validated.error.format())
            );
        }

        this.logger.log('Workflow request validated successfully', validated.data);

        // Use FormData to match curl -F behavior
        const formData = new FormData();
        formData.append('json', JSON.stringify(validated.data));

        // Create a proper File object instead of Blob
        const dummyFile = new File(['dummy content'], 'dummy.txt', {
            type: 'text/plain'
        });
        formData.append('file', dummyFile);

        const url = `${this.dotcmsUrl}/api/v1/workflow/actions/fire`;
        this.logger.log('Making request to dotCMS server', { url, method: 'PUT' });

        try {
            const response = await this.fetch(url, {
                method: 'PUT',
                body: formData,
                headers: {
                    Authorization: `Bearer ${this.authToken}`
                }
            });

            this.logger.log('Received response from dotCMS server', {
                status: response.status,
                statusText: response.statusText,
                ok: response.ok
            });

            if (!response.ok) {
                // Try to get error details from response
                let errorDetails = '';
                try {
                    const errorData = await response.text();
                    errorDetails = errorData;
                } catch (e) {
                    errorDetails = 'Could not read error response';
                }

                this.logger.error('dotCMS server returned error', {
                    status: response.status,
                    statusText: response.statusText,
                    errorDetails
                });

                throw new Error(`Failed to save content: ${response.status} ${response.statusText}. Details: ${errorDetails}`);
            }

            const data = await response.json();
            this.logger.log('Parsed JSON response from dotCMS server', data);

            const parsed = WorkflowActionResponseSchema.safeParse(data);

            if (!parsed.success) {
                this.logger.error('Invalid workflow response format', parsed.error);
                throw new Error('Invalid workflow response: ' + JSON.stringify(parsed.error.format()));
            }

            this.logger.log('Content saved successfully', parsed.data);

            return parsed.data;

        } catch (error) {
            this.logger.error('Error during content save operation', error);
            throw error;
        }
    }
}
