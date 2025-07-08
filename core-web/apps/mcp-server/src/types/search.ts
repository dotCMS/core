import { z } from 'zod';

// Dynamic contentlet: known fields + arbitrary fields
export const ContentletSchema = z
    .object({
        hostName: z.string(),
        modDate: z.string(),
        publishDate: z.string(),
        title: z.string(),
        baseType: z.string(),
        inode: z.string(),
        archived: z.boolean(),
        host: z.string(),
        ownerUserName: z.string(),
        working: z.boolean(),
        locked: z.boolean(),
        stInode: z.string(),
        contentType: z.string(),
        live: z.boolean(),
        owner: z.string(),
        identifier: z.string(),
        publishUserName: z.string(),
        publishUser: z.string(),
        languageId: z.number(),
        creationDate: z.string(),
        shortyId: z.string(),
        url: z.string(),
        titleImage: z.string(),
        modUserName: z.string(),
        hasLiveVersion: z.boolean(),
        folder: z.string(),
        hasTitleImage: z.boolean(),
        sortOrder: z.number(),
        modUser: z.string(),
        __icon__: z.string(),
        contentTypeIcon: z.string(),
        variant: z.string()
    })
    .catchall(z.unknown());

export const JsonObjectViewSchema = z.object({
    contentlets: z.array(ContentletSchema)
});

export const EntitySchema = z.object({
    contentTook: z.number(),
    jsonObjectView: JsonObjectViewSchema,
    queryTook: z.number(),
    resultsSize: z.number()
});

export const SearchResponseSchema = z.object({
    entity: EntitySchema,
    errors: z.array(z.unknown()),
    i18nMessagesMap: z.record(z.unknown()),
    messages: z.array(z.unknown()),
    pagination: z.unknown().nullable(),
    permissions: z.array(z.unknown())
});

export type Contentlet = z.infer<typeof ContentletSchema>;

export type SearchResponse = z.infer<typeof SearchResponseSchema>;
