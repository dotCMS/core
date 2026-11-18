import { z } from 'zod';

import { CARDINALITIES, FIELD_TYPES } from '../operations/shared/content-type-fields';

/**
 * The field shape `create_content_type` and `update_content_type` share — model-facing text,
 * so it lives with the definitions.
 */
export const optionSchema = z.union([
    z.string(),
    z.object({ label: z.string(), value: z.string() })
]);

export const fieldInputSchema = z.object({
    name: z.string().min(1).describe('Field label shown to editors.'),
    variable: z
        .string()
        .optional()
        .describe(
            'Field variable (camelCase). Omit to derive it from the name. System property names ' +
                '(sortOrder, host, identifier, folder, …) are refused.'
        ),
    type: z
        .enum(FIELD_TYPES)
        .describe(
            'Field type. Numbers: "integer" or "decimal" (dotCMS has no numeric field class; ' +
                'these are text fields with a numeric data type).'
        ),
    required: z.boolean().optional(),
    listed: z.boolean().optional().describe('Show as a column in the content list.'),
    indexed: z.boolean().optional().describe('Searchable through the API/Lucene.'),
    searchable: z.boolean().optional().describe('Searchable in the back-end search form.'),
    unique: z.boolean().optional().describe('Text fields only.'),
    hint: z.string().optional(),
    defaultValue: z.string().optional(),
    options: z
        .array(optionSchema)
        .optional()
        .describe('select / multi-select / radio / checkbox: values, or { label, value }.'),
    value: z
        .string()
        .optional()
        .describe('constant / hidden: the fixed value. category: the parent category inode.'),
    relationship: z
        .object({
            target: z
                .string()
                .describe('Related content type variable, or "self" for the same type.'),
            cardinality: z.enum(CARDINALITIES)
        })
        .optional()
        .describe('Required for type "relationship".')
});
