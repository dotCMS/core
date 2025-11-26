import { z } from 'zod';

import { AgnosticClient } from './client';

import { SearchResponse, SearchResponseSchema } from '../types/search';
import { Logger } from '../utils/logger';

export const SearchFormSchema = z.object({
    query: z.string(),
    sort: z.string().optional(),
    limit: z.number().int().optional().default(10),
    offset: z.number().int().optional(),
    userId: z.string().optional(),
    render: z.string().optional(),
    depth: z.number().int().optional().default(1),
    languageId: z.number().int().optional().default(1),
    allCategoriesInfo: z.boolean().optional().default(false)
});

export type SearchForm = z.infer<typeof SearchFormSchema>;

export class ContentSearchService extends AgnosticClient {
    private serviceLogger: Logger;

    constructor() {
        super();
        this.serviceLogger = new Logger('CONTENT_SEARCH_SERVICE');
    }

    /**
     * Search content using lucene query and SearchForm fields.
     * @param body - SearchForm object
     * @param rememberQuery - Optional, whether to remember the query (default: false)
     * @returns Promise with the API response (unknown structure for now)
     */
    async search(body: SearchForm, rememberQuery = false): Promise<SearchResponse> {
        this.serviceLogger.log('Starting content search operation', { body, rememberQuery });

        const validated = SearchFormSchema.safeParse(body);
        if (!validated.success) {
            this.serviceLogger.error('Invalid search parameters', validated.error);
            throw new Error(
                'Invalid search parameters: ' + JSON.stringify(validated.error.format())
            );
        }

        this.serviceLogger.log('Search parameters validated successfully', validated.data);

        const url = `/api/content/_search?rememberQuery=${rememberQuery}`;
        try {
            this.serviceLogger.log('Sending search request to dotCMS', {
                url,
                params: validated.data
            });
            const response = await this.fetch(url, {
                method: 'POST',
                body: JSON.stringify(validated.data)
            });

            const data = await response.json();
            this.serviceLogger.log('Received response from dotCMS', data);

            const parsed = SearchResponseSchema.safeParse(data);

            if (!parsed.success) {
                this.serviceLogger.error('Invalid search response format', parsed.error);
                throw new Error(
                    'Invalid search response: ' + JSON.stringify(parsed.error.format())
                );
            }

            this.serviceLogger.log('Search response validated successfully', parsed.data);

            return parsed.data;
        } catch (error) {
            this.serviceLogger.error('Error during content search', error);
            throw error;
        }
    }
}
