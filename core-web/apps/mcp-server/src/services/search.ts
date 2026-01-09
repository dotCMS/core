import { z } from 'zod';

import { AgnosticClient } from './client';

import { SearchResponse, SearchResponseSchema } from '../types/search';
import { Logger } from '../utils/logger';

/**
 * Drive Search input schema
 * Matches /api/v1/drive/search expected payload
 */
export const SearchFormSchema = z.object({
    assetPath: z.string().describe('Root path to search from, e.g. "//" or "//SiteName/"'),
    includeSystemHost: z.boolean().default(true),
    filters: z
        .object({
            text: z.string().describe('Free text to search for'),
            filterFolders: z.boolean().default(true)
        })
        .describe('Search filters'),
    language: z.array(z.string()).optional().describe('Array of languageIds as strings'),
    contentTypes: z.array(z.string()).optional().default([]),
    baseTypes: z.array(z.string()).optional().describe('e.g. ["HTMLPAGE","FILEASSET","CONTENT"]'),
    offset: z.number().int().min(0).default(0),
    maxResults: z.number().int().positive().default(20),
    sortBy: z.string().default('modDate:desc'),
    archived: z.boolean().default(false),
    showFolders: z.boolean().default(false)
});

export type SearchForm = z.infer<typeof SearchFormSchema>;

export class ContentSearchService extends AgnosticClient {
    private serviceLogger: Logger;

    constructor() {
        super();
        this.serviceLogger = new Logger('CONTENT_SEARCH_SERVICE');
    }

    /**
     * Performs Drive Search using the /api/v1/drive/search endpoint.
     * @param body - Drive Search form parameters
     * @returns Promise with the API response
     */
    async search(body: SearchForm): Promise<SearchResponse> {
        this.serviceLogger.log('Starting drive search operation', { body });

        const validated = SearchFormSchema.safeParse(body);
        if (!validated.success) {
            this.serviceLogger.error('Invalid drive search parameters', validated.error);
            throw new Error(
                'Invalid drive search parameters: ' + JSON.stringify(validated.error.format())
            );
        }

        this.serviceLogger.log('Drive search parameters validated successfully', validated.data);

        const url = `/api/v1/drive/search`;
        try {
            this.serviceLogger.log('Sending drive search request to dotCMS', {
                url,
                params: validated.data
            });
            const response = await this.fetch(url, {
                method: 'POST',
                body: JSON.stringify(validated.data)
            });

            const data = await response.json();
            this.serviceLogger.log('Received response from dotCMS (drive search)', data);

            const parsed = SearchResponseSchema.safeParse(data);

            if (!parsed.success) {
                this.serviceLogger.error('Invalid drive search response format', parsed.error);
                throw new Error(
                    'Invalid drive search response: ' + JSON.stringify(parsed.error.format())
                );
            }

            this.serviceLogger.log('Drive search response validated successfully', parsed.data);

            return parsed.data;
        } catch (error) {
            this.serviceLogger.error('Error during drive search', error);
            throw error;
        }
    }
}
