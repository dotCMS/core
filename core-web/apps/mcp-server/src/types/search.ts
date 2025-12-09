import { z } from 'zod';

// Dynamic contentlet: known fields + arbitrary fields
export const ContentletSchema = z
    .object({
        hostName: z.string(),
        modDate: z.preprocess((val) => {
            if (typeof val === 'number') return val;
            if (typeof val === 'string' && val) {
                const num = Number(val);
                return isNaN(num) ? 0 : num;
            }
            return 0;
        }, z.number()),
        publishDate: z.preprocess((val) => {
            if (val === null || val === undefined) return val;
            if (typeof val === 'number') return val;
            if (typeof val === 'string' && val) {
                const num = Number(val);
                return isNaN(num) ? null : num;
            }
            return null;
        }, z.number().nullish()),
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
        publishUserName: z.string().nullish(),
        publishUser: z.string().nullish(),
        languageId: z.number(),
        creationDate: z.preprocess((val) => {
            if (typeof val === 'number') return val;
            if (typeof val === 'string' && val) {
                const num = Number(val);
                return isNaN(num) ? 0 : num;
            }
            return 0;
        }, z.number()),
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
