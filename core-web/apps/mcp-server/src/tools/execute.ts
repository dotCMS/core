import { type InferSchema, type ToolMetadata } from 'xmcp';
import { z } from 'zod';

import { createExecutor } from '../lib/executor';
import { createApiAdapter } from '../lib/http-client';

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

Always use the \`search\` tool first to discover the correct endpoint path and request/response schema before calling \`execute\`.

Tips:
- Use \`pick(arr, fields)\` to return only the fields you need — responses can be very large
- For file uploads use \`formData\` with \`{ name, type, data }\` (base64) or \`{ name, type, url }\` (remote URL)

Helper utilities available: pick(arr, fields), table(arr), count(arr, field), sum(arr, field), first(arr, n)`,
    annotations: {
        title: 'Execute dotCMS API Call',
        readOnlyHint: false,
        destructiveHint: true,
        idempotentHint: false,
        openWorldHint: true
    }
};

export default async function handler({ code }: InferSchema<typeof schema>) {
    const timeout = Number(process.env.SANDBOX_TIMEOUT) || 15000;

    const executor = createExecutor();
    const apiAdapter = createApiAdapter();
    executor.registerAdapter(apiAdapter);

    const result = await executor.execute(code, {
        sandbox: { timeout },
        adapters: ['api']
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
