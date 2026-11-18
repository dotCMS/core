import { z } from 'zod';

import { fieldInputSchema } from './content-type-fields';

import {
    CREATE_CONTENT_TYPE_ENDPOINTS,
    createContentType,
    type CreateContentTypeManifest
} from '../operations/content-type-create';
import { createTool, defineTool, type ToolContext } from '../toolkit/create-tool';
import { lenientBoolean } from '../toolkit/lenient-boolean';

import type { DotCMSConnection } from '../toolkit/connection';
import type { DotCMSTool, RequestToolOptions } from '../toolkit/types';

const definition = defineTool({
    name: 'create_content_type',
    title: 'Create a dotCMS Content Type',
    inputSchema: z.object({
        name: z.string().min(1).describe('Content type name, e.g. "Blog Post".'),
        variable: z
            .string()
            .optional()
            .describe('Content type variable (PascalCase), e.g. "BlogPost". Omit to derive it.'),
        description: z.string().optional(),
        site: z
            .string()
            .optional()
            .describe('Site the type belongs to (hostname or identifier). Omit for all sites.'),
        workflows: z
            .array(z.string())
            .optional()
            .describe('Workflow scheme names or ids. Default: ["System Workflow"].'),
        urlMapPattern: z
            .string()
            .optional()
            .describe('Detail-page URL pattern, e.g. "/blog/{urlTitle}" (fields in braces).'),
        detailPage: z
            .string()
            .optional()
            .describe('Identifier of the page that renders the urlMapPattern.'),
        icon: z.string().optional(),
        fields: z.array(fieldInputSchema).min(1).describe('Fields, in display order.'),
        dryRun: lenientBoolean(false).describe(
            'Validate and return the request body without creating anything.'
        )
    }),
    description: `Create a content type from a friendly field list, validated BEFORE anything is written.

Prefer this over POST /api/v1/contenttype through execute. It handles:
  - Field types by friendly name (text, integer, decimal, select, date, image, story-block,
    relationship, …). dotCMS has no numeric field class: integer/decimal become text fields
    with a numeric data type; an unknown class otherwise fails with a JSON parse error.
  - Field variables that collide with contentlet system properties (sortOrder, host,
    identifier, folder, …) are refused with a suggestion. dotCMS accepts them and then fails
    when content is saved or read.
  - Relationships: { type: "relationship", relationship: { target, cardinality } } with
    cardinality one-to-many | many-to-many | one-to-one | many-to-one, target "self" for a
    self-relationship. The tool sets relationType, the cardinality ordinal, indexed: true and
    listed: false — the exact shape dotCMS requires. The target type must exist: create it first.
  - Workflows by name (default System Workflow); site by hostname.
  - Every problem is reported in one error; nothing is created when any field is invalid.

After creating, the type is read back and each field and relationship record is verified;
anything missing comes back in \`warnings\`. Use dryRun to preview the body.

To change an existing type use update_content_type. A new type appears in the GraphQL schema
after a short delay.`,
    annotations: {
        readOnlyHint: false,
        destructiveHint: false,
        idempotentHint: false,
        openWorldHint: true
    },
    endpoints: CREATE_CONTENT_TYPE_ENDPOINTS,
    async handler(args, ctx: ToolContext<RequestToolOptions>): Promise<CreateContentTypeManifest> {
        return createContentType({ dotcms: ctx.runtime(), ...args });
    }
});

/**
 * The `create_content_type` tool: a validated content type create with friendly field types,
 * reserved-variable checks, correct relationship fields and a read-back.
 * Resolves to a {@link CreateContentTypeManifest}.
 */
export function createContentTypeTool(
    connection: DotCMSConnection,
    options: RequestToolOptions = {}
): DotCMSTool<typeof definition.inputSchema, CreateContentTypeManifest> {
    return createTool(definition, connection, options);
}
