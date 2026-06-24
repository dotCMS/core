import { type InferSchema, type ToolExtraArguments, type ToolMetadata } from 'xmcp';
import { z } from 'zod';

import { createDotCMSRuntime } from '@dotcms/ai';
import { getSpec } from '@dotcms/ai/spec';

export const schema = {
    code: z
        .string()
        .max(100_000)
        .describe(
            'JavaScript async function body. The `spec` global contains the dereferenced OpenAPI spec. Return the data you need.'
        )
};

export const metadata: ToolMetadata = {
    name: 'search',
    description: `Explore the dotCMS REST API specification. Write JavaScript that runs in a sandbox with the \`spec\` global.

Spec structure:
- \`spec.paths\` — object keyed by path string (e.g. "/api/v1/contenttype")
- Method keys are lowercase: get, post, put, delete
- Each operation has: summary, parameters, requestBody, responses
- \`requestBody.content\` is keyed by MIME type (e.g. "application/json"), then \`.schema\` for the body shape
- \`parameters\` is an array of { name, in, required, schema } — "in" is "query", "path", or "header"
- \`responses\` keys are HTTP status codes; schemas are stripped — only description and content MIME type keys remain (e.g. \`responses['200'].content['application/json']\` is \`{}\`)

Pre-loaded instance context (also available as globals here):
  - contentTypes, sites, languages, currentUser
  Use these to cross-reference spec endpoints with what the connected instance actually has.

Example:
  const op = spec.paths['/api/v1/contenttype'].get
  return { summary: op.summary, params: op.parameters?.map(p => p.name) }

When inspecting workflow \`fire\` operations, always check the \`indexPolicy\` query parameterand its allowed values. When the \`execute\` tool needs to chain multiple fire calls, or fireand then immediately read content, use \`indexPolicy=WAIT_FOR\` to ensure the index isupdated before the next operation runs.

Common recipes:
- **Find a workflow action ID for a contentlet:** call GET /api/v1/workflow/contentlet/{inode}/actions — direct lookup of actions firable on this contentlet right now. The \`workflowActionId\` returned here is the UUID required by PUT /api/v1/workflow/actions/{actionId}/fire and PUT /api/v1/workflow/contentlet/actions/bulk/fire. **Do not** pass system-action enum values (NEW, EDIT, PUBLISH, …) as the action ID; those are only valid for the /default/fire/{systemAction} endpoints.
- **Find actions for a content type when you don't have an inode:** GET /api/v1/workflow/contenttypes/{contentTypeVarOrId}/system/actions.
- **Verify what an action actually does (e.g. whether it has a Move actionlet wired):** GET /api/v1/workflow/actions/{actionId}/actionlets.
- **Move content:** the system action enum has no MOVE; \`pathToMove\` only works on actions with the Move actionlet. Discover the right action via the recipes above, then PUT /api/v1/workflow/actions/{actionId}/fire with \`pathToMove\` in the body, or use bulk fire with \`additionalParams.additionalParamsMap._path_to_move\`.`,
    annotations: {
        title: 'Search dotCMS API Spec',
        readOnlyHint: true,
        destructiveHint: false,
        idempotentHint: true,
        openWorldHint: false
    }
};

export default async function handler(
    { code }: InferSchema<typeof schema>,
    extra?: ToolExtraArguments
) {
    const spec = getSpec();

    if (!spec || typeof spec !== 'object' || Object.keys(spec).length === 0) {
        return 'Error: OpenAPI spec is not available. The server may not have been built with a generated spec (run the generate-spec step), so the search tool cannot run.';
    }

    // The front door injects the instance context AND the `spec` global (includeSpec).
    const dotcms = createDotCMSRuntime({
        url: process.env.DOTCMS_URL ?? '',
        token: process.env.AUTH_TOKEN ?? '',
        sessionId: extra?.sessionId ?? '__default__',
        timeout: 10000,
        includeSpec: true,
        onContextError: (label, error) => {
            const msg = error instanceof Error ? error.message : String(error);
            console.error(`[context] failed to load ${label}: ${msg}`);
        }
    });

    const result = await dotcms.run(code);

    if (!result.success) {
        const errorMsg = result.error
            ? `${result.error.name}: ${result.error.message}`
            : 'Unknown error';
        const logs = result.logs.length > 0 ? `\nLogs:\n${result.logs.join('\n')}` : '';
        return `Error: ${errorMsg}${logs}`;
    }

    const output =
        typeof result.value === 'string' ? result.value : JSON.stringify(result.value, null, 2);

    const logs = result.logs.length > 0 ? `\n\n--- Logs ---\n${result.logs.join('\n')}` : '';

    return `${output}${logs}`;
}
