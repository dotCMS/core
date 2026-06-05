import { type InferSchema, type ToolExtraArguments, type ToolMetadata } from 'xmcp';
import { z } from 'zod';

import { createApiAdapter, createExecutor, getSharedContextCache } from '@dotcms/agentic-tools';

export const schema = {
    code: z
        .string()
        .max(100_000)
        .describe(
            'JavaScript async function body to execute against the dotCMS API. Use `api.request({ method, path, query, body, formData })` to make authenticated API calls. Use `formData` instead of `body` for multipart/form-data uploads. Return the result.'
        )
};

export const metadata: ToolMetadata = {
    name: 'execute',
    description: `Execute code against the dotCMS REST API. Write JavaScript that runs in an isolated sandbox with the \`api\` adapter for making authenticated HTTP requests.

Use api.request(options) where options is:
  - method: HTTP method (GET, POST, PUT, DELETE) — default: GET
  - path: API path (e.g., "/api/v1/content")
  - query: Query parameters object
  - body: Request body (auto-serialized to JSON)
  - formData: Multipart form data object for file uploads (mutually exclusive with body).
              String values become text fields. Object values { name, type, data|url } become file fields.
  - headers: Additional headers

Auth is handled automatically — tokens are never exposed to your code.

Pre-loaded instance context (available as globals — no API calls needed to read these):
  - contentTypes: Array<{ id, name, variable, baseType, host?, folder? }>
  - sites: Array<{ identifier, hostname, isDefault, archived }>
  - languages: Array<{ id, languageCode, countryCode, language, country, isoCode }>
  - currentUser: { userId, email, givenName?, surname?, admin, roles? } | null
  Examples:
    const blog = contentTypes.find(c => c.variable === 'Blog');
    const defaultSite = sites.find(s => s.isDefault);
    const en = languages.find(l => l.languageCode === 'en');

Always use the \`search\` tool first to discover the correct endpoint path and request/response schema before calling \`execute\`.

Tips:
- Use \`pick(arr, fields)\` to return only the fields you need — responses can be very large
- For file uploads use \`formData\` with \`{ name, type, data }\` (base64) or \`{ name, type, url }\` (remote URL)

Block Editor (Story Block) fields:
- A Story Block field stores a string. When creating or updating content via a fire endpoint, send the field value as an **HTML or Markdown string** — do NOT hand-author the ProseMirror/JSON document. dotCMS stores it as-is and converts it to the Block Editor structure when the contentlet is opened in the editor.
- Example: \`{ "contentType": "Blog", "title": "My Post", "body": "<h2>Intro</h2><p>Hello <strong>world</strong>.</p>" }\` — where \`body\` is the Story Block field.
- Identify Story Block fields from the content type (\`fields[].clazz\` is \`...ImmutableStoryBlockField\`).

Workflow fires and Elasticsearch (indexPolicy):
- All \`/fire\` and \`/firemultipart\` endpoints (e.g. \`/api/v1/workflow/actions/default/fire/PUBLISH\`)
  accept an \`indexPolicy\` query parameter controlling when Elasticsearch reflects the change:
    - \`DEFER\`    — default; returns immediately, index may lag by seconds
    - \`WAIT_FOR\` — waits until the document is indexed before responding
    - \`FORCE\`    — forces an immediate index flush; expensive, avoid in production

- Use \`WAIT_FOR\` on **every** fire call when:
    - Chaining multiple workflow actions on the same contentlet, or
    - Reading state immediately after firing (via \`api.request\`, \`/api/content/_search\`, or GET by inode)
  Without it, follow-up reads may return stale data.

- Use \`DEFER\` for isolated, one-off fires where nothing depends on immediate index visibility.
- Reserve \`FORCE\` for debugging and testing only — it is heavy on the cluster.

Workflow action discovery (when you need a workflow action ID):
- The 'fire' endpoints that take \`{actionId}\` in the path (e.g. PUT /api/v1/workflow/actions/{actionId}/fire and bulk fire) require a workflow action **UUID**, not the system action enum (NEW, EDIT, PUBLISH, …).
- To find a UUID, call GET /api/v1/workflow/contentlet/{inode}/actions — returns actions firable on that contentlet right now.
- Without an inode, GET /api/v1/workflow/contenttypes/{contentTypeVarOrId}/system/actions returns the system→action mapping for a content type.
- To verify an action wires a Move actionlet (required before \`pathToMove\` does anything): GET /api/v1/workflow/actions/{actionId}/actionlets.
- The /actions/{actionId}/fire endpoint bypasses scheme checks; use it for System Workflow actions (like Move) on content from custom schemes. The /contentlet/actions/bulk/fire endpoint enforces scheme association — input contentlets whose scheme does not own the supplied action are skipped (\`skippedCount\` populated, \`skipReason\` explains).

Helper utilities available: pick(arr, fields), table(arr), count(arr, field), sum(arr, field), first(arr, n)`,
    annotations: {
        title: 'Execute dotCMS API Call',
        readOnlyHint: false,
        destructiveHint: true,
        idempotentHint: false,
        openWorldHint: true
    }
};

export default async function handler(
    { code }: InferSchema<typeof schema>,
    extra?: ToolExtraArguments
) {
    const timeout = Number(process.env.SANDBOX_TIMEOUT) || 15000;

    const executor = createExecutor();
    const apiAdapter = createApiAdapter({
        dotcmsUrl: process.env.DOTCMS_URL ?? '',
        authToken: process.env.AUTH_TOKEN ?? ''
    });
    executor.registerAdapter(apiAdapter);

    const sessionId = extra?.sessionId ?? '__default__';
    const cache = getSharedContextCache({
        onError: (label, error) => {
            const msg = error instanceof Error ? error.message : String(error);
            console.error(`[context] failed to load ${label}: ${msg}`);
        }
    });
    const context = await cache.get(sessionId, apiAdapter);

    const result = await executor.execute(code, {
        sandbox: { timeout },
        adapters: ['api'],
        variables: {
            contentTypes: context.contentTypes,
            sites: context.sites,
            languages: context.languages,
            currentUser: context.currentUser
        }
    });

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
