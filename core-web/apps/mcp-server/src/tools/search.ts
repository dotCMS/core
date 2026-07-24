import { type InferSchema, type ToolExtraArguments, type ToolMetadata } from 'xmcp';
import { z } from 'zod';

import { createRuntime, formatSandboxResult } from '@dotcms/ai/runtime';
import { getSpec } from '@dotcms/ai/spec';

export const schema = {
    code: z
        .string()
        .max(100_000)
        .describe(
            'JavaScript async function body. The `spec` global contains the filtered dotCMS OpenAPI spec (`$ref`-based: `spec.paths` + `spec.components.schemas`). Return the data you need.'
        )
};

export const metadata: ToolMetadata = {
    name: 'search',
    description: `Explore the dotCMS REST API specification. Write JavaScript that runs in a sandbox with the \`spec\` global.

Spec structure:
- \`spec.paths\` — object keyed by path string (e.g. "/api/v1/contenttype")
- Method keys are lowercase: get, post, put, delete
- Each operation has: summary, parameters, requestBody, responses
- \`requestBody.content\` / \`responses[status].content\` are keyed by MIME type (e.g. "application/json"), then \`.schema\` for the body shape — \`.schema\` is usually a \`$ref\` like \`{ $ref: '#/components/schemas/PageView' }\`, NOT an inline object
- \`parameters\` is an array of { name, in, required, schema } — "in" is "query", "path", or "header"
- \`responses\` keys are HTTP status codes
- \`spec.components.schemas\` — every schema referenced by the kept endpoints, keyed by name

Resolving \`$ref\`s: call \`resolveRef(schemaOrName, depth = 2)\` — it resolves a \`$ref\` (or a schema name) against \`spec.components.schemas\`, expanding nested refs \`depth\` levels and leaving deeper ones as \`$ref\` strings for a follow-up query. Use it instead of hand-walking refs.

Helpers available here: \`resolveRef(schemaOrName, depth)\`, \`pick(arr, fields)\`, \`table(arr)\`, \`count(arr, field)\`, \`sum(arr, field)\`, \`first(arr, n)\`.

Output is hard-capped (~25k chars). Return only what you need — resolve one schema at a bounded depth, not the whole spec.

This spec is a CURATED allow-list of the supported authoring endpoints — not every dotCMS endpoint is present, and that is deliberate. Treat it as the set of endpoints you should use. So:
- **Guard path access:** \`spec.paths['/x']\` may be \`undefined\`, and \`spec.paths['/x'].get\` on a missing path throws \`TypeError: Cannot read properties of undefined\`. Always use optional chaining: \`spec.paths['/x']?.get\`, or check \`if (!spec.paths['/x']) return 'not in spec'\` first.
- **Discover before assuming:** to find a path, filter the keys — \`Object.keys(spec.paths).filter(p => p.includes('template'))\` — rather than guessing an exact string.
- **Absent usually means "not the intended path."** If an endpoint isn't here, first look for a supported one that does the job (search the keys; a different path or verb often covers it). Reaching for an off-list endpoint should be a last resort for a genuine gap, not a reflex — the curated set is what these tools are designed and tested around.

Pre-loaded instance context (also available as globals here):
  - contentTypes, sites, languages, currentUser
  Use these to cross-reference spec endpoints with what the connected instance actually has.

Examples:
  // endpoint summary + param names
  const op = spec.paths['/api/v1/contenttype'].get
  return { summary: op.summary, params: op.parameters?.map(p => p.name) }

  // the request-body schema of an endpoint, one level deep
  return resolveRef(spec.paths['/api/v1/contenttype'].post.requestBody.content['application/json'].schema, 1)

  // a named schema, two levels deep
  return resolveRef('ContentType', 2)

When inspecting workflow \`fire\` operations, always check the \`indexPolicy\` query parameter and its allowed values. When the \`execute\` tool needs to chain multiple fire calls, or fire and then immediately read content, use \`indexPolicy=WAIT_FOR\` to ensure the index is updated before the next operation runs.

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
    const dotcms = createRuntime({
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

    return formatSandboxResult(result, {
        truncationHint: 'Use resolveRef(schemaOrName, depth) to expand one schema at a bounded depth.'
    });
}
