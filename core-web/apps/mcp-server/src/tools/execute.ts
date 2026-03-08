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

Examples:
  // List content types
  const result = await api.request({ path: '/api/v1/contenttype' })
  return result

  // Get a specific content type
  const result = await api.request({ path: '/api/v1/contenttype/id/webPageContent' })
  return result

  // Search content with Elasticsearch
  const result = await api.request({
    method: 'POST',
    path: '/api/v1/es/search',
    body: { query: 'contentType:webPageContent +languageId:1 +deleted:false +working:true' }
  })
  return pick(result.contentlets, ['identifier', 'title', 'modDate'])

  // Get page render
  const result = await api.request({
    path: '/api/v1/page/render/about-us',
    query: { language_id: '1' }
  })
  return result

  // Browse navigation
  const result = await api.request({ path: '/api/v1/nav/?depth=2' })
  return result

  // Upload an asset (base64)
  const result = await api.request({
    method: 'PUT',
    path: '/api/v1/assets',
    formData: {
      assetPath: '/images/logo.png',
      file: { name: 'logo.png', type: 'image/png', data: '<base64-encoded-content>' }
    }
  })
  return result

  // Upload a file from URL
  const result = await api.request({
    method: 'PUT',
    path: '/api/v1/assets',
    formData: {
      assetPath: '/documents/report.pdf',
      file: { name: 'report.pdf', type: 'application/pdf', url: 'https://example.com/report.pdf' }
    }
  })
  return result

  // Create a temp file from a remote URL (returns temp ID for use in content creation)
  const result = await api.request({
    method: 'POST',
    path: '/api/v1/temp/byUrl',
    body: {
      remoteUrl: 'https://example.com/photo.png',
      fileName: 'my-image.png',
      maxFileLength: '50MB'
    }
  })
  return result.tempFiles

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
