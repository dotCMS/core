import { AgnosticClient } from './client';

import {
    ContentCreateParams,
    WorkflowActionRequest,
    WorkflowActionResponse,
    WorkflowActionRequestSchema,
    WorkflowActionResponseSchema,
    ContentCreateParamsSchema,
    WorkflowSchemesResponse,
    WorkflowSchemesResponseSchema,
    ContentActionParams,
    ContentActionParamsSchema
} from '../types/workflow';
import { Logger } from '../utils/logger';

export class WorkflowService extends AgnosticClient {
    private serviceLogger: Logger;

    private API_RESOURCE_URL = '/api/v1/workflow';
    private ACTION_FIRE_URL = `${this.API_RESOURCE_URL}/actions/default/fire`;

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

        const url = `${this.ACTION_FIRE_URL}/NEW`;

        try {
            const response = await this.fetch(url, {
                method: 'PUT',
                body: JSON.stringify(validated.data)
            });

            const data = await response.json();

            const parsed = WorkflowActionResponseSchema.safeParse(data);

            if (!parsed.success) {
                this.serviceLogger.error('Invalid workflow response format', parsed.error);
                throw new Error(
                    'Invalid workflow response: ' + JSON.stringify(parsed.error.format())
                );
            }

            this.serviceLogger.log('Content saved successfully', parsed.data);

            return parsed.data;
        } catch (error) {
            this.serviceLogger.error('Error during content save operation', error);
            throw error;
        }
    }

    /**
     * Performs content actions (publish/unpublish) by firing a workflow action.
     * This method publishes or unpublishes existing content based on the provided parameters.
     *
     * @param params - Content action parameters including the action type
     * @returns Promise with the workflow action response
     */
    async performContentAction(params: ContentActionParams): Promise<WorkflowActionResponse> {
        this.serviceLogger.log('Starting content action operation', params);

        const validatedParams = ContentActionParamsSchema.safeParse(params);

        if (!validatedParams.success) {
            this.serviceLogger.error('Invalid content action parameters', validatedParams.error);
            throw new Error(
                'Invalid content action parameters: ' +
                    JSON.stringify(validatedParams.error.format())
            );
        }

        this.serviceLogger.log(
            'Content action parameters validated successfully',
            validatedParams.data
        );

        const actionType = validatedParams.data.action;
        const defaultComments = {
            PUBLISH: 'Publishing content via API',
            UNPUBLISH: 'Unpublishing content via API',
            ARCHIVE: 'Archiving content via API',
            UNARCHIVE: 'Unarchiving content via API',
            DELETE: 'Deleting content via API'
        };
        const defaultComment = defaultComments[actionType];

        const urlObj = new URL(`${this.ACTION_FIRE_URL}/${actionType}`, this.dotcmsUrl);
        urlObj.searchParams.set('identifier', validatedParams.data.identifier);
        urlObj.searchParams.set('variantName', validatedParams.data.variantName);

        try {
            const response = await this.fetch(urlObj.toString(), {
                method: 'PUT',
                body: JSON.stringify({
                    comments: validatedParams.data.comments || defaultComment
                })
            });

            const data = await response.json();

            const parsed = WorkflowActionResponseSchema.safeParse(data);

            if (!parsed.success) {
                this.serviceLogger.error('Invalid workflow response format', parsed.error);
                throw new Error(
                    'Invalid workflow response: ' + JSON.stringify(parsed.error.format())
                );
            }

            const successMessages = {
                PUBLISH: 'Content published successfully',
                UNPUBLISH: 'Content unpublished successfully',
                ARCHIVE: 'Content archived successfully',
                UNARCHIVE: 'Content unarchived successfully',
                DELETE: 'Content deleted successfully'
            };
            this.serviceLogger.log(successMessages[actionType], parsed.data);

            return parsed.data;
        } catch (error) {
            const errorMessages = {
                PUBLISH: 'Error during content publish operation',
                UNPUBLISH: 'Error during content unpublish operation',
                ARCHIVE: 'Error during content archive operation',
                UNARCHIVE: 'Error during content unarchive operation',
                DELETE: 'Error during content delete operation'
            };
            this.serviceLogger.error(errorMessages[actionType], error);
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

        const url = `${this.API_RESOURCE_URL}/schemes?showArchived=true`;

        try {
            const response = await this.fetch(url, {
                method: 'GET'
            });

            const data = await response.json();

            const parsed = WorkflowSchemesResponseSchema.safeParse(data);

            if (!parsed.success) {
                this.serviceLogger.error('Invalid workflow schemes response format', parsed.error);
                throw new Error(
                    'Invalid workflow schemes response: ' + JSON.stringify(parsed.error.format())
                );
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
