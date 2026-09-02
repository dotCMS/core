import { type InferSchema, type ToolExtraArguments, type ToolMetadata } from 'xmcp';
import { z } from 'zod';

import { formatSandboxResult } from '@dotcms/ai/runtime';

import { runtimeFromEnv, toolFailure } from '../lib/runtime';

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

This is a **JavaScript sandbox, NOT Velocity/VTL**:
- Velocity variables like \`$dotcontent\`, \`$dotcontent.pull(...)\`, \`$date\`, \`#foreach\` do NOT exist here — referencing \`$dotcontent\` throws \`ReferenceError\`. To query content, call \`api.request({ method: 'POST', path: '/api/content/_search', body: { query, ... } })\`. Run VTL only via \`POST /api/vtl/dynamic\`.
- **\`await\` every \`api.request\`** and return only JSON-serializable values (objects, arrays, strings, numbers). Returning an un-awaited Promise (or a function/class instance) throws \`DataCloneError\` — the result is structured-cloned out of the worker.
- Watch string literals: a raw apostrophe inside a single-quoted JS string (e.g. \`'grandchild's'\`) is a \`SyntaxError\`. Use double quotes or escape it.

Pre-loaded instance context (available as globals — no API calls needed to read these):
  - contentTypes: Array<{ id, name, variable, baseType, host?, folder? }>
  - sites: Array<{ identifier, hostname, isDefault, archived, live }> — all accessible non-system
           sites, including stopped and archived states
  - languages: Array<{ id, languageCode, countryCode, language, country, isoCode }>
  - currentUser: { userId, email, givenName?, surname?, admin, roles? } | null
  Examples:
    const blog = contentTypes.find(c => c.variable === 'Blog');
    const defaultSite = sites.find(s => s.isDefault);
    const en = languages.find(l => l.languageCode === 'en');

The pre-loaded globals are a snapshot taken at the start of one tool invocation and do not mutate
halfway through the current script. Each MCP invocation constructs fresh runtime context, so a
subsequent call sees successful changes to sites, content types, or languages without maintaining
resource-specific invalidation rules.

Do not use \`PUT /api/v1/site/switch/{id}\` as a targeting mechanism. MCP API requests are
independent and session-scoped site selection is not guaranteed to carry to the next request.
Pass explicit site/host identifiers (for example \`host_id\` or \`contentHost\`) instead.

Always use the \`search\` tool first to discover the correct endpoint path and request/response schema before calling \`execute\`.

Transferring file assets? Do NOT use this tool. Use the dedicated \`upload_assets\` /
\`download_assets\` tools instead — they stream file bytes between disk and dotCMS on the
server side, so the content never enters your context (and you never need a token or \`.env\`).
Reach for \`execute\` only for JSON/API work; inlining base64 file bytes here just bloats your
context. The \`formData\`/base64 path below exists only for small, programmatic payloads — not
for transferring real files, themes, or directories.

Tips:
- Output is hard-capped (~25k chars). Use \`pick(arr, fields)\` / \`first(arr, n)\` to return only the fields you need — responses can be very large and are truncated past the cap.
- For a small programmatic upload (NOT real files — use \`upload_assets\` for those) use \`formData\` with \`{ name, type, data }\` (base64) or \`{ name, type, url }\` (remote URL)

Binary responses (small/programmatic reads only — for real files use \`download_assets\`):
- Endpoints that return non-text bodies (e.g. GET \`/api/v2/assets/{identifier}\` and \`/dA/{id}\`, content-type \`application/octet-stream\` or \`image/*\`) come back as an envelope: \`{ __dotcmsBinary: true, contentType, base64, byteLength }\`.
- The \`base64\` field IS the raw file bytes — base64-decode it to recover the exact file. Do NOT treat it as text; the bytes are intact (not UTF-8-mangled).
- JSON and textual responses (\`text/*\`, xml, js, \`+json\`/\`+xml\`) are returned as parsed objects / strings as before — only binary bodies use the envelope.

Content field variables (the \`contentlet\` fire body):
- The fire body's \`contentlet\` is keyed by each field's exact **field variable** (from the content type's \`fields[].variable\`) — casing is significant and a wrong-case key is silently ignored (its value is dropped, and a required field then 400s as "required").
- Page (htmlpageasset) system fields are lowercase/camel exactly: \`contentHost\` (the SITE — NOT \`host\`), \`hostFolder\` (the folder id), \`cachettl\` (all lowercase — NOT \`cacheTTL\`/\`cacheTtl\`), \`template\`, \`url\`, \`title\`, \`friendlyName\`, \`pageTitle\`. \`contentHost\` must be a site **identifier UUID**, not a hostname. Prefer the \`page_create\` tool, which sets all of these correctly.
- For any content type, read the real field variables first: \`GET /api/v1/contenttype/id/{idOrVar}\` → \`entity.fields[].variable\`. Don't guess.

Block Editor (Story Block) fields:
- A Story Block field stores a string. When creating or updating content via a fire endpoint, send the field value as an **HTML or Markdown string** — do NOT hand-author the ProseMirror/JSON document. The server converts it to the Block Editor structure **on save**, so the field immediately reads back as structured content.
- Example: \`{ "contentType": "Blog", "title": "My Post", "body": "<h2>Intro</h2><p>Hello <strong>world</strong>.</p>" }\` — where \`body\` is the Story Block field.
- Rich blocks are expressed in Markdown as fenced code blocks: the info string is a \`dotcms-*\` label, the body is one JSON object. All keys are lowercase; **bold** keys are required (a payload missing them, or with a wrong-typed required key, silently degrades to a plain code block):
    - \`dotcms-content\` — embedded contentlet: \`{"identifier": "<contentlet-id>", "languageId": 1}\`. Required: **\`identifier\`**; optional \`languageId\` (default 1). The server rehydrates the full embed data from the identifier on read.
    - \`dotcms-image\` — dotCMS-bound/decorated image. Required: one of **\`identifier\`** or **\`src\`**; optional \`alt\`, \`title\`, \`href\`, \`target\`, \`textWrap\`, \`textAlign\`, \`languageId\`. Plain external images need no fence — \`![alt](url "title")\` works.
    - \`dotcms-video\` — Required: one of **\`identifier\`** or **\`src\`**; optional \`mimeType\`, \`width\`, \`height\`, \`languageId\`.
    - \`dotcms-youtube\` — Required: **\`src\`**; optional \`start\` (seconds), \`width\`, \`height\`.
    - \`dotcms-grid\` — verbatim \`gridBlock\` node JSON: \`{"type":"gridBlock","attrs":{"columns":[n,n]},"content":[<exactly two gridColumn nodes>]}\`.
    - \`dotcms-node\` — any other node type verbatim: \`{"type": "<nodeType>", ...}\` (fallback for custom blocks).
- In HTML, the same labels are custom elements. Scalar payloads ride as attributes with **hyphenated names** (HTML lowercases attribute names, so \`languageId\` is spelled \`language-id\`, \`mimeType\` is \`mime-type\`, etc.): \`<dotcms-content identifier="<contentlet-id>" language-id="1"></dotcms-content>\`. \`dotcms-grid\` and \`dotcms-node\` take the same JSON object as the element's **text body** instead: \`<dotcms-grid>{"type":"gridBlock",...}</dotcms-grid>\` (HTML-escape \`<\` and \`&\` inside JSON string values). Always write an explicit closing tag — HTML parsing ignores the \`/\` in \`<dotcms-video ... />\` and would swallow the content after it.
- Block styling (e.g. alignment) is an HTML comment on its own line immediately before the block, in Markdown and HTML alike: \`<!-- dotcms:attrs {"textAlign":"center"} -->\`.
- A Markdown/HTML write **fully replaces** the stored document. If the stored document has rich blocks your value does not carry, the save still succeeds (200) and the response \`messages\` field lists the replaced blocks — read \`messages\` after every fire and carry rich blocks over as \`dotcms-*\` fences (Markdown) or \`<dotcms-*>\` elements (HTML) to preserve them. Invalid payloads degrade to plain code blocks (or are dropped when there is no body to keep), never errors.
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

Velocity \`$dotcontent.pull\` sorting:
- Pass content field variables in canonical unsuffixed form, e.g. \`Book.title asc\`. The search
  layer selects the keyword mapping by appending \`_dotraw\` internally.
- Already-suffixed input such as \`Book.title_dotraw asc\` is accepted for compatibility and is
  normalized without producing \`_dotraw_dotraw\`.

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
    const timeout = Number(process.env.SANDBOX_TIMEOUT) || 45000;

    // Guarded: `runtimeFromEnv` throws on a misconfigured server, and an unguarded throw
    // here escapes as an MCP PROTOCOL error rather than a tool result — the model sees a
    // transport failure with none of the explanation the error itself carries.
    try {
        // The front door absorbs the executor + adapter + context-cache wiring and injects
        // dotCMS instance context automatically. Auth tokens never enter the sandbox.
        const dotcms = runtimeFromEnv(extra?.sessionId, { timeout });

        const result = await dotcms.run(code); // code === the model's output

        return formatSandboxResult(result, {
            truncationHint:
                'Return only the fields you need — use pick(arr, fields) and first(arr, n).'
        });
    } catch (error) {
        return toolFailure('execute', error);
    }
}
