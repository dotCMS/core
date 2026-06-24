import { type InferSchema, type ToolExtraArguments, type ToolMetadata } from 'xmcp';
import { z } from 'zod';

import { downloadAssets } from '../lib/assets-transfer';
import { errorMessage, runtimeFromEnv } from '../lib/runtime';

export const schema = {
    path: z
        .string()
        .min(1)
        .describe(
            'dotCMS folder or asset path to download, e.g. /application/themes/travel or //demo.dotcms.com/application/themes/travel/css/styles.scss'
        ),
    dest: z
        .string()
        .min(1)
        .describe('Absolute local directory the MCP server writes files into'),
    recursive: z.boolean().default(true).describe('Include files in nested folders'),
    overwrite: z
        .enum(['skip', 'overwrite', 'error'])
        .default('skip')
        .describe('Behavior when a destination file already exists'),
    include: z
        .string()
        .optional()
        .describe('Optional comma-separated glob filter, e.g. *.vtl,*.scss')
};

export const metadata: ToolMetadata = {
    name: 'download_assets',
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
        title: 'Download dotCMS Assets',
        readOnlyHint: false,
        destructiveHint: false,
        idempotentHint: true,
        openWorldHint: true
    }
};

export default async function handler(args: InferSchema<typeof schema>, extra?: ToolExtraArguments) {
    try {
        const manifest = await downloadAssets({
            dotcms: runtimeFromEnv(extra?.sessionId),
            path: args.path,
            dest: args.dest,
            recursive: args.recursive,
            overwrite: args.overwrite,
            include: args.include
        });

        return JSON.stringify(manifest, null, 2);
    } catch (error) {
        return `Error: ${errorMessage(error)}`;
    }
}
