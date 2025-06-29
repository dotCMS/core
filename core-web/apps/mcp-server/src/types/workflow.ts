import { z } from 'zod';

// Schema for workflow action response (correct format)
export const WorkflowActionResponseSchema = z.object({
    entity: z.object({
        identifier: z.string(),
        inode: z.string(),
        title: z.string().optional(),
        contentType: z.string(),
        // API returns number, but sometimes string
        languageId: z.union([z.string(), z.number()])
    }).catchall(z.any()),
    errors: z.array(z.string()),
    messages: z.array(z.string()),
    i18nMessagesMap: z.record(z.string()),
    // TODO: Add pagination schema
    pagination: z.any(),
    permissions: z.array(z.string())
});

// Schema for content creation parameters
export const ContentCreateParamsSchema = z.object({
    contentType: z.string(),
    languageId: z.string()
}).catchall(z.any());

// Schema for workflow action request
export const WorkflowActionRequestSchema = z.object({
    actionName: z.string(),
    comments: z.string().optional(),
    contentlet: ContentCreateParamsSchema
});

export type WorkflowActionResponse = z.infer<typeof WorkflowActionResponseSchema>;

export type ContentCreateParams = z.infer<typeof ContentCreateParamsSchema>;

export type WorkflowActionRequest = z.infer<typeof WorkflowActionRequestSchema>;
