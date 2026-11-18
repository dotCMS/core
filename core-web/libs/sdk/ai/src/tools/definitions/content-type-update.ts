import { z } from 'zod';

import { fieldInputSchema, optionSchema } from './content-type-fields';

import {
    UPDATE_CONTENT_TYPE_ENDPOINTS,
    updateContentType,
    type UpdateContentTypeManifest
} from '../operations/content-type-update';
import { createTool, defineTool, type ToolContext } from '../toolkit/create-tool';
import { lenientBoolean } from '../toolkit/lenient-boolean';

import type { DotCMSConnection } from '../toolkit/connection';
import type { DotCMSTool, RequestToolOptions } from '../toolkit/types';

const definition = defineTool({
    name: 'update_content_type',
    title: 'Update a dotCMS Content Type',
    inputSchema: z.object({
        contentType: z.string().min(1).describe('Content type variable or id. Case-sensitive.'),
        name: z.string().optional(),
        description: z.string().optional(),
        workflows: z
            .array(z.string())
            .optional()
            .describe('Replace the workflow schemes (names or ids).'),
        urlMapPattern: z.string().optional(),
        detailPage: z.string().optional(),
        addFields: z
            .array(fieldInputSchema)
            .optional()
            .describe('New fields, appended. Same shape and validation as create_content_type.'),
        updateFields: z
            .array(
                z.object({
                    variable: z.string().describe('Existing field variable.'),
                    name: z.string().optional(),
                    required: z.boolean().optional(),
                    listed: z.boolean().optional(),
                    indexed: z.boolean().optional(),
                    searchable: z.boolean().optional(),
                    hint: z.string().optional(),
                    defaultValue: z.string().optional(),
                    options: z.array(optionSchema).optional()
                })
            )
            .optional()
            .describe("Change existing fields' properties. A field's type cannot change."),
        removeFields: z
            .array(z.string())
            .optional()
            .describe('Field variables to delete. DELETES their data; requires allowDataLoss.'),
        allowDataLoss: lenientBoolean(false).describe('Confirm removeFields.'),
        dryRun: lenientBoolean(false).describe('Report what would change, without changing it.')
    }),
    description: `Change an existing content type: add, update or remove fields; rename; change workflows or URL map.

Prefer this over editing a content type through execute. dotCMS has no endpoint that adds a
field to an existing type (the fields endpoints answer 404/405 to POST) and POST
/api/v1/contenttype only creates; the one way is a PUT of the WHOLE type, which answers 404
with no message when the body is incomplete. This tool reads the full type, applies your changes
— keeping every field's id so existing content keeps its data — converts the workflow list to
the shape the PUT expects, and sends it back. Then it reads the type again and verifies.

New fields get the same validation as create_content_type (friendly types, reserved variables
refused, relationship shape). removeFields deletes data and needs allowDataLoss: true. Use
dryRun first for anything beyond adding fields.`,
    annotations: {
        readOnlyHint: false,
        destructiveHint: true,
        idempotentHint: false,
        openWorldHint: true
    },
    endpoints: UPDATE_CONTENT_TYPE_ENDPOINTS,
    async handler(args, ctx: ToolContext<RequestToolOptions>): Promise<UpdateContentTypeManifest> {
        return updateContentType({ dotcms: ctx.runtime(), ...args });
    }
});

/**
 * The `update_content_type` tool: a full-body content type update that adds, changes and
 * removes fields without dropping field ids, with validation and a read-back.
 * Resolves to an {@link UpdateContentTypeManifest}.
 */
export function updateContentTypeTool(
    connection: DotCMSConnection,
    options: RequestToolOptions = {}
): DotCMSTool<typeof definition.inputSchema, UpdateContentTypeManifest> {
    return createTool(definition, connection, options);
}
