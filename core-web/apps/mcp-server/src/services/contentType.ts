import { z } from 'zod';

import { AgnosticClient } from './client';

import {
    ContentType,
    ContentTypeBaseTypeEnum,
    ContentTypeField,
    ContentTypeSchema,
    FieldClazz,
    KnownFieldClazz,
    Layout
} from '../types/contentype';
import { Logger } from '../utils/logger';

const DEFAULT_PAGE = 1;
const DEFAULT_PER_PAGE = 100;
const DEFAULT_ORDER_BY = 'name';
const DEFAULT_DIRECTION = 'ASC';

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

type FieldType = z.infer<typeof FieldTypeEnum>;

// Mapping function to get clazz from fieldType using the known field types
const getClazzFromFieldType = (fieldType: FieldType): KnownFieldClazz => {
    const fieldTypeToClazzMap: Record<FieldType, KnownFieldClazz> = {
        Binary: 'com.dotcms.contenttype.model.field.ImmutableBinaryField',
        'Story-Block': 'com.dotcms.contenttype.model.field.ImmutableStoryBlockField',
        'Block Editor': 'com.dotcms.contenttype.model.field.ImmutableStoryBlockField',
        Category: 'com.dotcms.contenttype.model.field.ImmutableCategoryField',
        Checkbox: 'com.dotcms.contenttype.model.field.ImmutableCheckboxField',
        'Constant-Field': 'com.dotcms.contenttype.model.field.ImmutableConstantField',
        'Custom-Field': 'com.dotcms.contenttype.model.field.ImmutableCustomField',
        Date: 'com.dotcms.contenttype.model.field.ImmutableDateField',
        'Date-and-Time': 'com.dotcms.contenttype.model.field.ImmutableDateTimeField',
        File: 'com.dotcms.contenttype.model.field.ImmutableFileField',
        'Hidden-Field': 'com.dotcms.contenttype.model.field.ImmutableHiddenField',
        Image: 'com.dotcms.contenttype.model.field.ImmutableImageField',
        'JSON-Field': 'com.dotcms.contenttype.model.field.ImmutableJSONField',
        'Key-Value': 'com.dotcms.contenttype.model.field.ImmutableKeyValueField',
        'Multi-Select': 'com.dotcms.contenttype.model.field.ImmutableMultiSelectField',
        Radio: 'com.dotcms.contenttype.model.field.ImmutableRadioField',
        Relationship: 'com.dotcms.contenttype.model.field.ImmutableRelationshipField',
        Select: 'com.dotcms.contenttype.model.field.ImmutableSelectField',
        'Host-Folder': 'com.dotcms.contenttype.model.field.ImmutableHostFolderField',
        Tag: 'com.dotcms.contenttype.model.field.ImmutableTagField',
        Text: 'com.dotcms.contenttype.model.field.ImmutableTextField',
        Textarea: 'com.dotcms.contenttype.model.field.ImmutableTextAreaField',
        Time: 'com.dotcms.contenttype.model.field.ImmutableTimeField',
        WYSIWYG: 'com.dotcms.contenttype.model.field.ImmutableWysiwygField'
    };

    return (
        fieldTypeToClazzMap[fieldType] || 'com.dotcms.contenttype.model.field.ImmutableTextField'
    );
};

const ContentTypeFieldSchema = z
    .object({
        name: z.string(),
        fieldType: FieldTypeEnum,
        clazz: z.custom<FieldClazz>().optional(), // Optional since it will be auto-generated
        required: z.boolean().optional(),
        listed: z.boolean().optional(),
        searchable: z.boolean().optional(),
        indexed: z.boolean().optional(),
        hint: z.string().optional(),
        // For Checkbox, Multi-Select, Radio, Select: value is required, otherwise optional
        values: z.string().optional()
    })
    .superRefine((data, ctx) => {
        const needsValue = ['Checkbox', 'Multi-Select', 'Radio', 'Select'];
        if (needsValue.includes(data.fieldType) && (!data.values || data.values.trim() === '')) {
            ctx.addIssue({
                code: z.ZodIssueCode.custom,
                message: `Field type '${data.fieldType}' requires a 'value' property with options in the format 'Label|value' (one per line).`,
                path: ['values']
            });
        }
    });

export const ContentTypeCreateParamsSchema = z.object({
    name: z.string(),
    description: z.string(),
    host: z.string(),
    workflow: z.array(z.string()),
    fields: z.array(ContentTypeFieldSchema).min(1)
});

type ContentTypeCreateParams = z.infer<typeof ContentTypeCreateParamsSchema>;

export class ContentTypeService extends AgnosticClient {
    private serviceLogger: Logger;

    constructor() {
        super();
        this.serviceLogger = new Logger('CONTENT_TYPE_SERVICE');
    }

    /**
     * Fetches a list of content types from dotCMS.
     * @param params - Query parameters for filtering, pagination, etc.
     * @returns Promise with the API response JSON
     */
    async list(params?: ContentTypeListParams): Promise<ContentType[]> {
        this.serviceLogger.log('Starting content type list operation', { params });

        const validated = ContentTypeListParamsSchema.safeParse(params || {});
        if (!validated.success) {
            this.serviceLogger.error('Invalid content type list parameters', validated.error);
            throw new Error('Invalid parameters: ' + JSON.stringify(validated.error.format()));
        }

        this.serviceLogger.log(
            'Content type list parameters validated successfully',
            validated.data
        );

        const searchParams = new URLSearchParams();
        Object.entries(validated.data).forEach(([key, value]) => {
            if (value !== undefined) searchParams.append(key, String(value));
        });

        const url = `/api/v1/contenttype${searchParams.toString() ? '?' + searchParams.toString() : ''}`;

        try {
            const response = await this.fetch(url, { method: 'GET' });
            const data = await response.json();

            const result = data.entity.map((contentType: ContentType) => {
                return {
                    ...contentType,
                    fields: this.#extractFieldsFromLayout(contentType.layout)
                };
            });

            const parsed = z.array(ContentTypeSchema).safeParse(result);

            if (!parsed.success) {
                this.serviceLogger.error('Invalid content type response format', parsed.error);
                throw new Error(
                    'Invalid content type response: ' + JSON.stringify(parsed.error.format())
                );
            }

            this.serviceLogger.log('Content types fetched successfully', {
                count: parsed.data.length
            });

            return parsed.data;
        } catch (error) {
            this.serviceLogger.error('Error fetching content types', error);
            throw error;
        }
    }

    /**
     * Creates one or more content types in dotCMS.
     * @param params - Content type creation parameters
     * @returns Promise with the created content type(s)
     */
    async create(
        params: ContentTypeCreateParams | ContentTypeCreateParams[]
    ): Promise<ContentType[]> {
        this.serviceLogger.log('Starting content type creation operation', {
            isArray: Array.isArray(params),
            count: Array.isArray(params) ? params.length : 1
        });

        const isArray = Array.isArray(params);
        const dataToValidate = isArray ? params : [params];

        // Validate each content type object
        const validated = z.array(ContentTypeCreateParamsSchema).safeParse(dataToValidate);

        if (!validated.success) {
            this.serviceLogger.error('Invalid content type creation parameters', validated.error);
            throw new Error('Invalid parameters: ' + JSON.stringify(validated.error.format()));
        }

        // Extra runtime check for value property on special field types
        for (const contentType of validated.data) {
            for (const field of contentType.fields) {
                if (
                    ['Checkbox', 'Multi-Select', 'Radio', 'Select'].includes(field.fieldType) &&
                    (!field.values || field.values.trim() === '')
                ) {
                    throw new Error(
                        `Field '${field.name}' of type '${field.fieldType}' requires a 'value' property with options in the format 'Label|value' (one per line).`
                    );
                }
            }
        }

        this.serviceLogger.log(
            'Content type creation parameters validated successfully',
            validated.data
        );

        // Auto-generate clazz for fields that don't have it
        const processedData = validated.data.map((contentType) => ({
            ...contentType,
            clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
            fields: contentType.fields.map((field) => ({
                ...field,
                clazz: field.clazz || getClazzFromFieldType(field.fieldType)
            }))
        }));

        const url = '/api/v1/contenttype';
        try {
            const response = await this.fetch(url, {
                method: 'POST',
                body: JSON.stringify(isArray ? processedData : processedData[0])
            });

            const data = await response.json();
            this.serviceLogger.log('Parsed JSON response from dotCMS server', data);

            // dotCMS returns { entity: ContentType[] }
            const entity = data.entity;

            const parsed = z.array(ContentTypeSchema).safeParse(entity);
            if (!parsed.success) {
                this.serviceLogger.error('Invalid content type response format', parsed.error);
                throw new Error(
                    'Invalid content type response: ' + JSON.stringify(parsed.error.format())
                );
            }

            this.serviceLogger.log('Content types created successfully', {
                count: parsed.data.length
            });

            return parsed.data;
        } catch (error) {
            this.serviceLogger.error('Error creating content types', error);
            throw new Error('Error creating content types: ' + error);
        }
    }

    async getContentTypesSchema(): Promise<ContentType[]> {
        this.serviceLogger.log('Starting content types schema fetch operation');

        const allContentTypes = await this.list({
            page: DEFAULT_PAGE,
            // TODO: allow the user to specify the number of content types to fetch
            per_page: DEFAULT_PER_PAGE,
            orderby: DEFAULT_ORDER_BY,
            direction: DEFAULT_DIRECTION
        });

        this.serviceLogger.log('Retrieved content types for schema', {
            count: allContentTypes.length
        });

        const result = allContentTypes.map((contentType) => {
            return {
                ...contentType,
                fields: this.#extractFieldsFromLayout(contentType.layout)
            };
        });

        this.serviceLogger.log('Content types schema processed successfully', {
            count: result.length
        });

        return result;
    }

    #extractFieldsFromLayout(layout: Layout[]): ContentTypeField[] {
        const allFields: ContentTypeField[] = [];

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
