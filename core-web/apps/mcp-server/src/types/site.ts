import { z } from 'zod';

export const SiteSchema = z
    .object({
        aliases: z.string().optional(),
        archived: z.boolean(),
        categoryId: z.string(),
        contentTypeId: z.string(),
        default: z.boolean(),
        dotAsset: z.boolean(),
        fileAsset: z.boolean(),
        folder: z.string(),
        form: z.boolean(),
        host: z.string(),
        hostThumbnail: z.string().nullable(),
        hostname: z.string(),
        htmlpage: z.boolean(),
        identifier: z.string(),
        indexPolicyDependencies: z.string(),
        inode: z.string(),
        keyValue: z.boolean(),
        languageId: z.number(),
        languageVariable: z.boolean(),
        live: z.boolean(),
        locked: z.boolean(),
        lowIndexPriority: z.boolean(),
        modDate: z.number(),
        modUser: z.string(),
        name: z.string(),
        new: z.boolean(),
        owner: z.string(),
        parent: z.boolean(),
        permissionId: z.string(),
        permissionType: z.string(),
        persona: z.boolean(),
        sortOrder: z.number(),
        structureInode: z.string(),
        systemHost: z.boolean(),
        tagStorage: z.string(),
        title: z.string(),
        titleImage: z.string().nullable(),
        type: z.string(),
        vanityUrl: z.boolean(),
        variantId: z.string(),
        versionId: z.string(),
        working: z.boolean()
    })
    .passthrough();

export const SiteResponseSchema = z.object({
    entity: SiteSchema,
    errors: z.array(z.any()),
    i18nMessagesMap: z.record(z.any()),
    messages: z.array(z.any()),
    pagination: z.any().nullable(),
    permissions: z.array(z.any())
});

export type Site = z.infer<typeof SiteSchema>;

export type SiteResponse = z.infer<typeof SiteResponseSchema>;
