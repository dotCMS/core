import { Logger } from '../../utils/logger';
import { executeWithErrorHandling, createSuccessResponse } from '../../utils/response';

// TODO: Import the services you need
// Examples:
// import { ContentTypeService } from '../../services/contentType';
// import { WorkflowService } from '../../services/workflow';
// import { SearchService } from '../../services/search';

// TODO: Import your input schema from index.ts and zod for validation
// import { z } from 'zod';
// const ExampleToolInputSchema = z.object({ ... });

const logger = new Logger('EXAMPLE_TOOL'); // TODO: Update logger name

// TODO: Create service instances you need
// const workflowService = new WorkflowService();
// const searchService = new SearchService();

/**
 * Handles the example tool execution
 *
 * This is where your tool logic lives. It should:
 * 1. Log the execution start
 * 2. Call one or more services to do the work
 * 3. Format the response using formatters.ts
 * 4. Return a success response
 *
 * Error handling is automatic via executeWithErrorHandling
 */
// Note: Using 'any' type here as a template - replace with your actual input schema type
// eslint-disable-next-line @typescript-eslint/no-explicit-any
export async function exampleToolHandler(params: any) {
    // TODO: Type this properly
    return executeWithErrorHandling(async () => {
        logger.log('Starting example tool execution', params);

        // TODO: Implement your tool logic here
        //
        // Example pattern for multi-step operations:
        //
        // Step 1: Search for content
        // const searchResults = await searchService.search({
        //     query: `+contentType:${params.contentType} +tags:${params.tag}`
        // });
        //
        // Step 2: Process each result
        // const results = [];
        // for (const content of searchResults.entity.jsonObjectView.contentlets) {
        //     const result = await workflowService.performContentAction({
        //         identifier: content.identifier,
        //         action: params.action
        //     });
        //     results.push(result);
        // }
        //
        // Step 3: Format response
        // const formattedText = formatExampleResponse(results);

        const formattedText = 'TODO: Implement tool logic and format response';

        logger.log('Example tool executed successfully');

        return createSuccessResponse(formattedText);
    }, 'Error executing example tool'); // TODO: Update error prefix
}

/**
 * Additional helper functions for this tool
 *
 * TODO: Add any tool-specific helpers here
 * Examples:
 * - Validation functions
 * - Data transformation functions
 * - Business logic functions
 */
