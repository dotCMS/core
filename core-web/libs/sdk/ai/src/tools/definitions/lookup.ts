import { z } from 'zod';

import {
    DEFAULT_LOOKUP_LIMIT,
    LOOKUP_ENDPOINTS,
    lookupContent,
    MAX_LOOKUP_LIMIT,
    type LookupManifest
} from '../operations/lookup';
import { createTool, defineTool, type ToolContext } from '../toolkit/create-tool';
import { lenientBoolean } from '../toolkit/lenient-boolean';

import type { DotCMSConnection } from '../toolkit/connection';
import type { DotCMSTool, RequestToolOptions } from '../toolkit/types';

const definition = defineTool({
    name: 'lookup',
    title: 'Look Up Existing dotCMS Content',
    inputSchema: z.object({
        contentType: z
            .string()
            .min(1)
            .describe('Content type variable to look in, e.g. "Author". Case-sensitive.'),
        match: z
            .object({
                field: z
                    .string()
                    .optional()
                    .describe('Field variable to match. Default: "title", else "name".'),
                value: z.string().min(1).describe('Phrase to match (case-insensitive).')
            })
            .optional()
            .describe('Narrow the results by a phrase on one field. Omit to list the type.'),
        query: z
            .string()
            .optional()
            .describe(
                'Extra Lucene clauses, e.g. "+Author.role:editor". Prefix fields with the type ' +
                    'variable.'
            ),
        includeArchived: lenientBoolean(false).describe('Include archived content. Default false.'),
        limit: z
            .number()
            .int()
            .positive()
            .max(MAX_LOOKUP_LIMIT)
            .optional()
            .describe(`Maximum contentlets to return. Default ${DEFAULT_LOOKUP_LIMIT}.`)
    }),
    description: `Find content that ALREADY EXISTS before creating or relating content. Read-only.

Call this before creating content that other content will point to (authors, categories, tags,
people, locations…) and before setting a relationship field. It answers "does this exist, and in
which languages?" — the two things that decide between reusing an item and creating one.

What it returns that a plain search does not make obvious:
  - Unpublished (working) content is included. Something that is not live still exists, and
    creating it again makes a duplicate.
  - One entry per contentlet, with EVERY language version it has, and \`missingLanguages\`: the
    instance languages it does not exist in. A relationship resolves only in a language the
    related content exists in — link an item from content in one of its missingLanguages and the
    field resolves EMPTY in that language, with no error anywhere. Create the missing language
    version (same identifier) first; upsert_content can do it.
  - \`relationshipFields\`: this type's relationship fields and their targets.
  - \`referencedBy\`: relationship fields on OTHER types that point to this one ("Blog.author").
    A referenced type holds shared content: reuse, don't duplicate.

Matching: \`match\` is a phrase match on one field (default title/name). Titles are often
written differently — when a match finds nothing, try a shorter phrase or list the type before
creating anything.`,
    annotations: {
        readOnlyHint: true,
        destructiveHint: false,
        idempotentHint: true,
        openWorldHint: true
    },
    endpoints: LOOKUP_ENDPOINTS,
    async handler(args, ctx: ToolContext<RequestToolOptions>): Promise<LookupManifest> {
        return lookupContent({
            dotcms: ctx.runtime(),
            contentType: args.contentType,
            match: args.match,
            query: args.query,
            includeArchived: args.includeArchived,
            limit: args.limit
        });
    }
});

/**
 * The `lookup` tool: what already exists for a content type — unpublished content included,
 * grouped per contentlet with its language versions, plus the relationships that point to the
 * type. Read-only. Resolves to a {@link LookupManifest}.
 */
export function lookupTool(
    connection: DotCMSConnection,
    options: RequestToolOptions = {}
): DotCMSTool<typeof definition.inputSchema, LookupManifest> {
    return createTool(definition, connection, options);
}
