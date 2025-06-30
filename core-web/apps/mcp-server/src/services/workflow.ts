import { AgnosticClient } from './client';

import {
    ContentCreateParams,
    WorkflowActionRequest,
    WorkflowActionResponse,
    WorkflowActionRequestSchema,
    WorkflowActionResponseSchema,
    ContentCreateParamsSchema,
    WorkflowSchemesResponse,
    WorkflowSchemesResponseSchema
} from '../types/workflow';
import { Logger } from '../utils/logger';

export class WorkflowService extends AgnosticClient {
    private serviceLogger: Logger;

    constructor() {
        super();
        this.serviceLogger = new Logger('WORKFLOW_SERVICE');
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
        this.serviceLogger.log('Starting content save operation', { params, comments });

        const validatedParams = ContentCreateParamsSchema.safeParse(params);

        if (!validatedParams.success) {
            this.serviceLogger.error('Invalid content parameters', validatedParams.error);
            throw new Error(
                'Invalid content parameters: ' + JSON.stringify(validatedParams.error.format())
            );
        }

        this.serviceLogger.log('Content parameters validated successfully', validatedParams.data);

        const workflowRequest: WorkflowActionRequest = {
            actionName: 'save',
            comments: comments || 'Saving content via API',
            contentlet: validatedParams.data
        };

        const validated = WorkflowActionRequestSchema.safeParse(workflowRequest);

        if (!validated.success) {
            this.serviceLogger.error('Invalid workflow request', validated.error);
            throw new Error(
                'Invalid workflow request: ' + JSON.stringify(validated.error.format())
            );
        }

        this.serviceLogger.log('Workflow request validated successfully', validated.data);

        // Use FormData to match curl -F behavior
        const formData = new FormData();
        formData.append('json', JSON.stringify(validated.data));

        // Create a proper File object instead of Blob
        const dummyFile = new File(['dummy content'], 'dummy.txt', {
            type: 'text/plain'
        });
        formData.append('file', dummyFile);

        const url = '/api/v1/workflow/actions/fire';

        try {
            const response = await this.fetch(url, {
                method: 'PUT',
                body: formData,
                headers: {
                    Authorization: `Bearer ${this.authToken}`
                }
            });

            const data = await response.json();

            const parsed = WorkflowActionResponseSchema.safeParse(data);

            if (!parsed.success) {
                this.serviceLogger.error('Invalid workflow response format', parsed.error);
                throw new Error('Invalid workflow response: ' + JSON.stringify(parsed.error.format()));
            }

            this.serviceLogger.log('Content saved successfully', parsed.data);

            return parsed.data;

        } catch (error) {
            this.serviceLogger.error('Error during content save operation', error);
            throw error;
        }
    }

    /**
     * Fetches all workflow schemes from the dotCMS instance.
     *
     * @returns Promise with the workflow schemes response
     */
    async getWorkflowSchemes(): Promise<WorkflowSchemesResponse> {
        this.serviceLogger.log('Starting workflow schemes fetch operation');

        const url = '/api/v1/workflow/schemes?showArchived=true';

        try {
            const response = await this.fetch(url, {
                method: 'GET',
                headers: {
                    Authorization: `Bearer ${this.authToken}`
                }
            });

            const data = await response.json();

            const parsed = WorkflowSchemesResponseSchema.safeParse(data);

            if (!parsed.success) {
                this.serviceLogger.error('Invalid workflow schemes response format', parsed.error);
                throw new Error('Invalid workflow schemes response: ' + JSON.stringify(parsed.error.format()));
            }

            this.serviceLogger.log('Workflow schemes fetched successfully', {
                schemeCount: parsed.data.entity.length
            });

            return parsed.data;

        } catch (error) {
            this.serviceLogger.error('Error during workflow schemes fetch operation', error);
            throw error;
        }
    }
}
