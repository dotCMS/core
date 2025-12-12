import { z } from 'zod';

// Schema for workflow action response (correct format)
export const WorkflowActionResponseSchema = z.object({
    entity: z
        .object({
            identifier: z.string(),
            inode: z.string(),
            title: z.string().optional(),
            contentType: z.string(),
            // API returns number, but sometimes string
            languageId: z.union([z.string(), z.number()])
        })
        .catchall(z.any()),
    errors: z.array(z.string()),
    messages: z.array(z.string()),
    i18nMessagesMap: z.record(z.string(), z.unknown()),
    // TODO: Add pagination schema
    pagination: z.any(),
    permissions: z.array(z.string())
});

// Schema for content creation parameters
export const ContentCreateParamsSchema = z
    .object({
        contentType: z.string(),
        languageId: z.string()
    })
    .catchall(z.any());

// Schema for workflow action request
export const WorkflowActionRequestSchema = z.object({
    actionName: z.string(),
    comments: z.string().optional(),
    contentlet: ContentCreateParamsSchema
});

// Schema for content action parameters (publish/unpublish/archive/unarchive/delete)
export const ContentActionParamsSchema = z.object({
    identifier: z.string(),
    variantName: z.string().default('DEFAULT'),
    comments: z.string().optional(),
    action: z.enum(['PUBLISH', 'UNPUBLISH', 'ARCHIVE', 'UNARCHIVE', 'DELETE'])
});

// Schema for workflow scheme
export const WorkflowSchemeSchema = z.object({
    archived: z.boolean(),
    creationDate: z.number(),
    defaultScheme: z.boolean(),
    description: z.string(),
    entryActionId: z.string().nullable(),
    id: z.string(),
    mandatory: z.boolean(),
    modDate: z.number(),
    name: z.string(),
    system: z.boolean(),
    variableName: z.string()
});

// Schema for workflow schemes response
export const WorkflowSchemesResponseSchema = z.object({
    entity: z.array(WorkflowSchemeSchema),
    errors: z.array(z.string()),
    i18nMessagesMap: z.record(z.string(), z.unknown()),
    messages: z.array(z.string()),
    pagination: z.any().nullable(),
    permissions: z.array(z.string())
});

export type WorkflowActionResponse = z.infer<typeof WorkflowActionResponseSchema>;

export type ContentCreateParams = z.infer<typeof ContentCreateParamsSchema>;

export type WorkflowActionRequest = z.infer<typeof WorkflowActionRequestSchema>;

export type ContentActionParams = z.infer<typeof ContentActionParamsSchema>;

export type WorkflowScheme = z.infer<typeof WorkflowSchemeSchema>;

export type WorkflowSchemesResponse = z.infer<typeof WorkflowSchemesResponseSchema>;
