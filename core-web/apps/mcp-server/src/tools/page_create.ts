import { type InferSchema, type ToolExtraArguments, type ToolMetadata } from 'xmcp';
import { z } from 'zod';

import { createPage } from '../lib/page-create';
import { errorMessage, runtimeFromEnv } from '../lib/runtime';

export const schema = {
    site: z
        .string()
        .min(1)
        .describe('Site the page lives on — identifier (UUID) or hostname, e.g. "demo.dotcms.com"'),
    urlPath: z
        .string()
        .min(1)
        .describe(
            'Page URL path on the site, e.g. "/books/index" or "/books". The parent folder is created automatically; the leaf segment becomes the page url (a bare folder or trailing slash defaults the leaf to "index").'
        ),
    title: z.string().min(1).describe('Page title'),
    template: z
        .string()
        .min(1)
        .describe('Template identifier the page renders with (the template UUID, not its name)'),
    contentType: z
        .string()
        .optional()
        .describe(
            'Page content type — variable or id. Defaults to "htmlpageasset". Must be a content type whose base type is HTMLPAGE; a custom page type may add its own fields (pass values via extraFields).'
        ),
    extraFields: z
        .record(z.string(), z.unknown())
        .optional()
        .describe(
            'Values for content-type fields beyond the common page fields, keyed by field variable. Required for any user-added required field on a custom page type that has no default value (the tool validates this before firing and tells you which fields are missing).'
        ),
    friendlyName: z.string().optional().describe('Friendly name; defaults to the title'),
    pageTitle: z.string().optional().describe('Browser <title>; defaults to the title'),
    languageId: z.number().int().default(1).describe('Language id. Default 1'),
    cacheTtl: z.string().default('0').describe('Cache TTL in seconds, as a string. Default "0"'),
    sortOrder: z.number().int().default(0).describe('Sort order within the folder. Default 0')
};

export const metadata: ToolMetadata = {
    name: 'page_create',
    description: `Create and publish a dotCMS page in one safe call.

A dotCMS "page" is a contentlet whose content type's base type is HTMLPAGE, fired through the
generic workflow endpoint — there is NO dedicated create-page endpoint. The content type defaults
to \`htmlpageasset\`, but pass \`contentType\` to use a custom page type. The tool resolves the type,
confirms it really is a page type, and validates its required fields BEFORE creating anything — so
a custom page type's user-added required fields surface as a clear "pass extraFields: { … }" error
instead of an opaque 400 after the folder was already created. Hand-rolling that fire with the
\`execute\` tool is the path that hits two sharp edges; this tool exists to absorb the first one:

  The URL-collapse trap. If you fire a page with \`url: "/books/index"\` but the \`/books\` folder
  does not exist yet, dotCMS silently collapses the url down to \`/index\` — which then 400s with
  "Page URL [/index] already exists" because the home page already owns it. This tool splits
  \`urlPath\` into folder + leaf, creates the parent folder first (idempotent), then fires the
  page with the leaf url under that folder — so the url lands exactly where you meant.

WHAT THIS TOOL DOES NOT DO — read this. It creates the page; it does NOT place any content on it.
The page comes up live but BLANK. Placing content (and the re-publish that makes it render) is a
separate, explicit step you perform afterward with the \`execute\` tool against the page's layout.
The returned manifest flags a \`live: false\` / warning case so a successful create is never
mistaken for a fully-populated page.

Returns a JSON manifest: { identifier, inode, folder, url, fullPath, site, live, warnings }.

Use the \`execute\` tool (not this one) when you need a page variant, want to set custom page
fields beyond the common set, or need to fire a non-PUBLISH workflow action.`,
    annotations: {
        title: 'Create dotCMS Page',
        readOnlyHint: false,
        destructiveHint: false,
        idempotentHint: false,
        openWorldHint: true
    }
};

export default async function handler(
    args: InferSchema<typeof schema>,
    extra?: ToolExtraArguments
) {
    try {
        const manifest = await createPage({
            dotcms: runtimeFromEnv(extra?.sessionId),
            site: args.site,
            urlPath: args.urlPath,
            title: args.title,
            template: args.template,
            contentType: args.contentType,
            extraFields: args.extraFields,
            friendlyName: args.friendlyName,
            pageTitle: args.pageTitle,
            languageId: args.languageId,
            cacheTtl: args.cacheTtl,
            sortOrder: args.sortOrder
        });

        return JSON.stringify(manifest, null, 2);
    } catch (error) {
        return `Error: ${errorMessage(error)}`;
    }
}
