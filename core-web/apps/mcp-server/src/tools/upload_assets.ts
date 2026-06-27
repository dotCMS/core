import { type InferSchema, type ToolExtraArguments, type ToolMetadata } from 'xmcp';
import { z } from 'zod';

import { uploadAssets } from '../lib/assets-transfer';
import { errorMessage, runtimeFromEnv } from '../lib/runtime';

export const schema = {
    src: z.string().min(1).describe('Absolute local directory the MCP server reads files from'),
    dest: z
        .string()
        .min(1)
        .describe(
            'Host-qualified dotCMS destination folder, e.g. //demo.dotcms.com/application/themes/travel'
        ),
    include: z
        .string()
        .optional()
        .describe('Optional comma-separated glob filter, e.g. *.vtl,*.scss'),
    publish: z
        .boolean()
        .default(true)
        .describe('Use /api/v2/assets/publish when true, otherwise /api/v2/assets/save'),
    verify: z
        .boolean()
        .default(true)
        .describe('After publishing, verify live status through /api/v1/content/{identifier}')
};

export const metadata: ToolMetadata = {
    name: 'upload_assets',
    description: `Upload one or more local files as dotCMS file assets (themes, VTL, CSS, JS, images, fonts, …).

ALWAYS use this tool to put files into dotCMS — do NOT hand-roll uploads with the \`execute\`
tool, a direct API call, or a custom script. This is the supported path and it is strictly
better for two reasons:
  1. File bytes are read from disk by the server and streamed straight to dotCMS — they NEVER
     pass through your context. Use this whenever you'd otherwise inline file content (e.g. large
     VTL/CSS blobs) into a tool call; it keeps that content out of the conversation entirely.
  2. Auth is already configured on the server. You do NOT need a dotCMS token, a \`.env\` file, or
     any local credentials — never go looking for them.

Use it whenever local files need to land in dotCMS — whether the user explicitly asks to
"upload/push/publish," OR you decided to write files as part of a larger task and now need them
in dotCMS. Example: the user says "create a theme"; you write the theme files to a local
directory, then call this tool to upload them. Putting files in dotCMS is always a step you
take with this tool — not something you wait to be told to do, and not something you improvise.

Provide an absolute source directory (\`src\`) and a host-qualified destination (\`dest\`, e.g.
\`//demo.dotcms.com/application/themes/travel\`). Optional \`include\` globs limit which files go.
The tool preserves relative paths and returns only a JSON manifest — never the file bytes.

Tip: to avoid inlining large templates, write them to files on disk and upload them with this
tool, then reference them from a container/template via \`#dotParse\`.`,
    annotations: {
        title: 'Upload dotCMS Assets',
        readOnlyHint: false,
        destructiveHint: true,
        idempotentHint: false,
        openWorldHint: true
    }
};

export default async function handler(
    args: InferSchema<typeof schema>,
    extra?: ToolExtraArguments
) {
    try {
        const manifest = await uploadAssets({
            dotcms: runtimeFromEnv(extra?.sessionId),
            src: args.src,
            dest: args.dest,
            include: args.include,
            publish: args.publish,
            verify: args.verify
        });

        return JSON.stringify(manifest, null, 2);
    } catch (error) {
        return `Error: ${errorMessage(error)}`;
    }
}
