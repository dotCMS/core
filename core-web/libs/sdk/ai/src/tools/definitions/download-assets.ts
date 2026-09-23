import { z } from 'zod';

import {
    DOWNLOAD_ASSETS_ENDPOINTS,
    downloadAssets,
    type DownloadAssetsManifest
} from '../operations/assets-transfer';
import { createTool, defineTool, type ToolContext } from '../toolkit/create-tool';

import type { DotCMSConnection } from '../toolkit/connection';
import type { AssetToolOptions, DotCMSTool } from '../toolkit/types';

const definition = defineTool({
    name: 'download_assets',
    title: 'Download dotCMS Assets',
    inputSchema: z.object({
        path: z
            .string()
            .min(1)
            .describe(
                'dotCMS folder or asset path to download, e.g. /application/themes/travel or //demo.dotcms.com/application/themes/travel/css/styles.scss'
            ),
        dest: z
            .string()
            .min(1)
            .describe(
                'Absolute local directory the server writes files into. Must be inside the directory the server allows; a path outside it fails and names that directory.'
            ),
        recursive: z.boolean().default(true).describe('Include files in nested folders'),
        overwrite: z
            .enum(['skip', 'overwrite', 'error'])
            .default('skip')
            .describe('Behavior when a destination file already exists'),
        include: z
            .string()
            .optional()
            .describe('Optional comma-separated glob filter, e.g. *.vtl,*.scss')
    }),
    description: `Download one or more dotCMS file assets (themes, VTL, CSS, JS, images, fonts, …) to a local directory.

ALWAYS use this tool to pull files out of dotCMS — do NOT hand-roll downloads with the
\`execute\` tool, a direct API call, or a custom script. This is the supported path and it is
strictly better for two reasons:
  1. File bytes are written to disk by the server — they NEVER pass through your context. Use
     this whenever you'd otherwise read file content into a tool call; it keeps that content
     out of the conversation entirely.
  2. Auth is already configured on the server. You do NOT need a dotCMS token, a \`.env\` file, or
     any local credentials — never go looking for them.

Use it whenever you need dotCMS files on the local disk — whether the user explicitly asks to
"download/pull/export," OR you decided you need the existing files to inspect or edit them as
part of a larger task. Example: the user says "update the theme's CSS"; you download the current
theme files, edit them locally, then upload them back. Getting files out of dotCMS is always a
step you take with this tool — not something you wait to be told to do, and not something you improvise.

Provide the dotCMS folder or asset path and an absolute destination directory (\`dest\`). Optional
\`include\` globs limit which files are fetched. The tool preserves relative paths and returns only
a JSON manifest — never the file bytes.`,
    annotations: {
        readOnlyHint: false,
        destructiveHint: false,
        idempotentHint: true,
        openWorldHint: true
    },
    endpoints: DOWNLOAD_ASSETS_ENDPOINTS,
    async handler(args, ctx: ToolContext<AssetToolOptions>): Promise<DownloadAssetsManifest> {
        return downloadAssets({
            dotcms: ctx.runtime(),
            root: ctx.options.root,
            path: args.path,
            dest: args.dest,
            recursive: args.recursive,
            overwrite: args.overwrite,
            include: args.include
        });
    }
});

/**
 * The `download_assets` tool: streams dotCMS file assets to a local directory — the bytes
 * never pass through the model. Needs Node or Bun. Resolves to a {@link DownloadAssetsManifest}.
 * `options.root` bounds which local directories the model may write into.
 */
export function downloadAssetsTool(
    connection: DotCMSConnection,
    options: AssetToolOptions
): DotCMSTool<typeof definition.inputSchema, DownloadAssetsManifest> {
    return createTool(definition, connection, options);
}
