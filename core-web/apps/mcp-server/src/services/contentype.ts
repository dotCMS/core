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

export const ContentTypeCreateParamsSchema = z.object({
    clazz: z.string(),
    name: z.string(),
    description: z.string().optional(),
    host: z.string().optional(),
    owner: z.string().optional(),
    variable: z.string().optional(),
    fixed: z.boolean().optional(),
    system: z.boolean().optional(),
    folder: z.string().optional(),
    systemActionMappings: z.record(z.string()).optional(),
    workflow: z.array(z.string()).optional(),
    // TODO: Add fields schema
    fields: z.array(z.any()).optional()
});

type ContentTypeCreateParams = z.infer<typeof ContentTypeCreateParamsSchema>;

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

    /**
     * Creates one or more content types in dotCMS.
     * @param params - Content type creation parameters
     * @returns Promise with the created content type(s)
     */
    async create(params: ContentTypeCreateParams | ContentTypeCreateParams[]): Promise<ContentType[]> {
        const isArray = Array.isArray(params);
        const dataToValidate = isArray ? params : [params];

        // Validate each content type object
        const validated = z.array(ContentTypeCreateParamsSchema).safeParse(dataToValidate);
        if (!validated.success) {
            throw new Error('Invalid parameters: ' + JSON.stringify(validated.error.format()));
        }

        const url = `${this.dotcmsUrl}/api/v1/contenttype`;
        const response = await this.fetch(url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(isArray ? validated.data : validated.data[0])
        });

        if (!response.ok) {
            throw new Error(`Failed to create content type(s): ${response.status} ${response.statusText}`);
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
