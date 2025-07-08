import z from 'zod';

import { ContentSearchService, SearchFormSchema } from '../../services/search';
import { executeWithErrorHandling, createSuccessResponse } from '../../utils/response';

export type ContentSearchParams = z.infer<typeof SearchFormSchema>;

const contentSearchService = new ContentSearchService();

/**
 * Handles content search operations for MCP tool
 */
export async function contentSearchHandler(params: ContentSearchParams) {
    return executeWithErrorHandling(async () => {
        const result = await contentSearchService.search(params);

        return createSuccessResponse('Content search completed successfully!', result);
    }, 'Error performing content search');
}
