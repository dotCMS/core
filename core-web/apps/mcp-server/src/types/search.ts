import { z } from 'zod';

/**
 * Drive Search response schema
 * Matches the structure returned by /api/v1/drive/search
 */
export const DriveSearchItemSchema = z.record(z.string(), z.unknown());

export const DriveSearchEntitySchema = z.object({
    contentCount: z.number(),
    contentTotalCount: z.number(),
    folderCount: z.number(),
    list: z.array(DriveSearchItemSchema)
});

export const SearchResponseSchema = z.object({
    entity: DriveSearchEntitySchema,
    errors: z.array(z.unknown()).default([]),
    i18nMessagesMap: z.record(z.string(), z.unknown()).default({}),
    messages: z.array(z.unknown()).default([]),
    pagination: z.unknown().nullable().optional(),
    permissions: z.array(z.unknown()).default([])
});

export type SearchResponse = z.infer<typeof SearchResponseSchema>;
