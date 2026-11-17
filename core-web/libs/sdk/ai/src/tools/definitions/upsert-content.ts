import { z } from 'zod';

import {
    UPSERT_CONTENT_ENDPOINTS,
    upsertContent,
    type UpsertContentManifest
} from '../operations/upsert-content';
import { createTool, defineTool, type ToolContext } from '../toolkit/create-tool';
import { lenientBoolean } from '../toolkit/lenient-boolean';

import type { DotCMSConnection } from '../toolkit/connection';
import type { DotCMSTool, RequestToolOptions } from '../toolkit/types';

const definition = defineTool({
    name: 'upsert_content',
    title: 'Create or Update dotCMS Content',
    inputSchema: z.object({
        contentType: z
            .string()
            .min(1)
            .describe('Content type variable, e.g. "Blog". Case-sensitive.'),
        languageId: z
            .number()
            .int()
            .positive()
            .describe(
                'Language to write. Required: without it dotCMS writes to the instance DEFAULT ' +
                    'language, which may not be the one you mean. See the `languages` global.'
            ),
        fields: z
            .record(z.string(), z.unknown())
            .describe(
                'Field values keyed by exact field variable. Relationship fields take an ' +
                    'identifier, an array of identifiers, or "id1,id2"; an empty value clears it.'
            ),
        identifier: z
            .string()
            .optional()
            .describe(
                'Existing identifier. Updates that content in `languageId`, or adds the ' +
                    '`languageId` version when it does not exist yet. Omit to create new content.'
            ),
        site: z
            .string()
            .optional()
            .describe('Site for new content — hostname or identifier. Omit for the default site.'),
        publish: lenientBoolean(true).describe(
            'Publish (default true). false saves a working version only.'
        ),
        onRelationshipIssue: z
            .enum(['warn', 'fail'])
            .optional()
            .describe(
                '"warn" (default): write, and report relationship problems in `warnings`. ' +
                    '"fail": write nothing when a related item cannot resolve in this language.'
            )
    }),
    description: `Create or update ONE contentlet in ONE language, and verify its relationships resolve.

Prefer this over a raw workflow fire for content with relationship fields. Run the lookup tool
first to find content to reuse — never create an author/category/tag that already exists.

What it handles for you:
  - Field keys are checked against the content type. An unknown or wrong-case key (\`Title\` for
    \`title\`) is silently dropped by dotCMS; here it is refused with the list of real fields.
  - Relationship values: pass an identifier, an array, or "id1,id2". (The fire endpoint itself
    only takes a comma-separated string — an array fails with a 500.)
  - Related content is checked BEFORE writing. dotCMS accepts all of these without an error and
    the field then resolves EMPTY:
      related-content-not-found         the identifier does not exist
      related-content-wrong-type        it is not the type the field relates to
      related-content-missing-language  it has no version in this languageId → create that
                                        version first (upsert_content with its identifier and
                                        this languageId), then link it
  - After writing, relationships are read back. relationship-not-resolved means a value was sent
    but does not resolve yet — related content can take a few seconds after a change; read again
    before changing anything.

Languages: \`identifier\` + a languageId the content does not have yet creates that language
version of the SAME content (outcome "created-language-version"). The manifest lists every
language the content now exists in.`,
    annotations: {
        readOnlyHint: false,
        destructiveHint: false,
        idempotentHint: false,
        openWorldHint: true
    },
    endpoints: UPSERT_CONTENT_ENDPOINTS,
    async handler(args, ctx: ToolContext<RequestToolOptions>): Promise<UpsertContentManifest> {
        return upsertContent({
            dotcms: ctx.runtime(),
            contentType: args.contentType,
            languageId: args.languageId,
            fields: args.fields,
            identifier: args.identifier,
            site: args.site,
            publish: args.publish,
            onRelationshipIssue: args.onRelationshipIssue
        });
    }
});

/**
 * The `upsert_content` tool: create or update one contentlet in one language, with field-key
 * validation, relationship normalization, and relationship checks before and after the write.
 * Resolves to an {@link UpsertContentManifest}.
 */
export function upsertContentTool(
    connection: DotCMSConnection,
    options: RequestToolOptions = {}
): DotCMSTool<typeof definition.inputSchema, UpsertContentManifest> {
    return createTool(definition, connection, options);
}
