import { type InferSchema, type ToolMetadata } from 'xmcp';
import { z } from 'zod';

import { createExecutor } from '../lib/executor';
import { getSpec } from '../lib/spec';

export const schema = {
    code: z
        .string()
        .max(100_000)
        .describe(
            'JavaScript async function body to explore the OpenAPI spec. The `spec` global contains the dereferenced OpenAPI spec with `paths` object. Return the data you need.'
        )
};

export const metadata: ToolMetadata = {
    name: 'search',
    description: `Search and explore the dotCMS REST API specification. Write JavaScript code that runs in a sandbox with the \`spec\` global containing the full dereferenced OpenAPI spec.

The spec object has: { openapi, info, paths }
- spec.paths is an object keyed by path (e.g., "/api/v1/content")
- Each path has HTTP methods (get, post, put, delete) with operation details

Examples:
  // List all available endpoint paths
  return Object.keys(spec.paths)

  // Find endpoints related to content types
  return Object.entries(spec.paths)
    .filter(([path]) => path.includes('contenttype'))
    .map(([path, methods]) => ({ path, methods: Object.keys(methods) }))

  // Get details of a specific endpoint
  return spec.paths['/api/v1/content'].get

  // Find all POST endpoints
  return Object.entries(spec.paths)
    .filter(([_, methods]) => methods.post)
    .map(([path]) => path)

  // Inspect request body schema for creating content
  const post = spec.paths['/api/v1/content'].post
  return post?.requestBody?.content?.['application/json']?.schema`,
    annotations: {
        title: 'Search dotCMS API Spec',
        readOnlyHint: true,
        destructiveHint: false,
        idempotentHint: true,
        openWorldHint: false
    }
};

export default async function handler({ code }: InferSchema<typeof schema>) {
    const executor = createExecutor();
    const spec = getSpec();

    const result = await executor.execute(code, {
        variables: { spec },
        sandbox: { timeout: 10000 }
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
