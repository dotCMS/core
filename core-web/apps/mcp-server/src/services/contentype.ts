import { z } from 'zod';

import { AgnosticClient } from './client';

import { ContentType, ContentTypeBaseTypeEnum, ContentTypeSchema } from '../types/contentype';

const ContentTypeBaseTypeQueryEnum = z.union([
    z.literal('ANY'),
    ContentTypeBaseTypeEnum
]);

export const ContentTypeListParamsSchema = z.object({
    filter: z.string().optional(),
    page: z.number().int().positive().optional(),
    per_page: z.number().int().positive().optional(),
    orderby: z.string().optional(),
    direction: z.enum(['ASC', 'DESC']).optional(),
    type: ContentTypeBaseTypeQueryEnum.optional(),
    host: z.string().optional(),
    sites: z.string().optional()
});

type ContentTypeListParams = z.infer<typeof ContentTypeListParamsSchema>;


export class ContentTypeService extends AgnosticClient {
    constructor() {
        super();
    }

    /**
     * Fetches a list of content types from dotCMS.
     * @param params - Query parameters for filtering, pagination, etc.
     * @returns Promise with the API response JSON
     */
    async list(params?: ContentTypeListParams): Promise<ContentType[]> {
        const validated = ContentTypeListParamsSchema.safeParse(params || {});
        if (!validated.success) {
            throw new Error('Invalid parameters: ' + JSON.stringify(validated.error.format()));
        }

        const searchParams = new URLSearchParams();
        Object.entries(validated.data).forEach(([key, value]) => {
            if (value !== undefined) searchParams.append(key, String(value));
        });

        const url = `${this.dotcmsUrl}/api/v1/contenttype${searchParams.toString() ? '?' + searchParams.toString() : ''}`;
        const response = await this.fetch(url, { method: 'GET' });

        if (!response.ok) {
            throw new Error(`Failed to fetch content types: ${response.status} ${response.statusText}`);
        }

        const data = await response.json();
        // dotCMS returns { entity: ContentType[] }
        const entity = data.entity;
        const parsed = z.array(ContentTypeSchema).safeParse(entity);
        if (!parsed.success) {
            throw new Error('Invalid content type response: ' + JSON.stringify(parsed.error.format()));
        }

        return parsed.data;
    }
}
