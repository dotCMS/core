import { z } from 'zod';

import { AgnosticClient } from './client';

import { ContentType, ContentTypeBaseTypeEnum, ContentTypeField, ContentTypeSchema, Layout } from '../types/contentype';

const ContentTypeBaseTypeQueryEnum = z.union([z.literal('ANY'), ContentTypeBaseTypeEnum]);

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

const FieldTypeEnum = z.enum([
    'Story-Block',
    'Block Editor',
    'Category',
    'Checkbox',
    'Constant-Field',
    'Custom-Field',
    'Date',
    'Date-and-Time',
    'File',
    'Hidden-Field',
    'Image',
    'JSON-Field',
    'Key-Value',
    'Multi-Select',
    'Radio',
    'Relationship',
    'Select',
    'Host-Folder',
    'Tag',
    'Text',
    'Textarea',
    'Time',
    'WYSIWYG',
    'Binary'
]);

const ContentTypeFieldSchema = z.object({
    clazz: z.string(),
    contentTypeId: z.string().optional(),
    dataType: z.string().optional(),
    fieldType: FieldTypeEnum,
    fieldTypeLabel: z.string().optional(),
    fieldVariables: z.array(z.any()).optional(),
    fixed: z.boolean().optional(),
    forceIncludeInApi: z.boolean().optional(),
    iDate: z.number().optional(),
    id: z.string().nullable().optional(),
    indexed: z.boolean().optional(),
    listed: z.boolean().optional(),
    modDate: z.number().optional(),
    name: z.string(),
    readOnly: z.boolean().optional(),
    required: z.boolean().optional(),
    searchable: z.boolean().optional(),
    sortOrder: z.number().optional(),
    unique: z.boolean().optional(),
    variable: z.string().optional(),
    values: z.string().optional(),
    hint: z.string().optional(),
    categories: z
        .object({
            categoryName: z.string(),
            description: z.string().nullable(),
            inode: z.string(),
            key: z.string(),
            keywords: z.string(),
            sortOrder: z.number()
        })
        .optional(),
    relationships: z
        .object({
            cardinality: z.number(),
            isParentField: z.boolean(),
            velocityVar: z.string()
        })
        .optional(),
    skipRelationshipCreation: z.boolean().optional()
});

export const ContentTypeCreateParamsSchema = z.object({
    clazz: z.literal('com.dotcms.contenttype.model.type.ImmutableSimpleContentType'),
    name: z.string(),
    description: z.string().optional(),
    host: z.string().optional(),
    owner: z.string().optional(),
    variable: z.string().optional(),
    fixed: z.boolean().optional(),
    system: z.boolean().optional(),
    folder: z.string().optional(),
    systemActionMappings: z.record(z.any()).optional(),
    workflow: z.array(z.string()).optional(),
    fields: z.array(ContentTypeFieldSchema).min(1)
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
            throw new Error(
                `Failed to fetch content types: ${response.status} ${response.statusText}`
            );
        }

        const data = await response.json();
        // dotCMS returns { entity: ContentType[] }
        const entity = data.entity;
        const parsed = z.array(ContentTypeSchema).safeParse(entity);

        if (!parsed.success) {
            throw new Error(
                'Invalid content type response: ' + JSON.stringify(parsed.error.format())
            );
        }

        return parsed.data;
    }

    /**
     * Creates one or more content types in dotCMS.
     * @param params - Content type creation parameters
     * @returns Promise with the created content type(s)
     */
    async create(
        params: ContentTypeCreateParams | ContentTypeCreateParams[]
    ): Promise<ContentType[]> {
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
            throw new Error(
                `Failed to create content type(s): ${response.status} ${response.statusText}`
            );
        }

        const data = await response.json();
        // dotCMS returns { entity: ContentType[] }
        const entity = data.entity;
        const parsed = z.array(ContentTypeSchema).safeParse(entity);
        if (!parsed.success) {
            throw new Error(
                'Invalid content type response: ' + JSON.stringify(parsed.error.format())
            );
        }

        return parsed.data;
    }

    async getContentTypesSchema(): Promise<ContentType[]> {
        const allContentTypes = await this.list({
            page: 1,
            // TODO: allow the user to specify the number of content types to fetch
            per_page: 100,
            orderby: 'name',
            direction: 'ASC'
        });

        const result = allContentTypes.map((contentType) => {
            return {
                ...contentType,
                fields: this.#extractFieldsFromLayout(contentType.layout)
            }
        });

        return result;
    }


    #extractFieldsFromLayout(layout: Layout[]): ContentTypeField[] {
        const allFields = [];

        // Check if layout has a layout property (based on your structure)

        // Iterate through each row in the layout
        layout.forEach((row) => {
            // Skip if no columns exist
            if (!row.columns || !Array.isArray(row.columns)) {
                return;
            }

            // Iterate through each column in the row
            row.columns.forEach((column) => {
                // Skip if no fields exist in the column
                if (!column.fields || !Array.isArray(column.fields)) {
                    return;
                }

                // Add all fields from this column to our result array
                allFields.push(...column.fields);
            });
        });

        return allFields;
    }
}
