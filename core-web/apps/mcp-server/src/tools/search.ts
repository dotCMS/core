import { type InferSchema, type ToolMetadata } from 'xmcp';
import { z } from 'zod';

import { createExecutor } from '../lib/executor';
import { getSpec } from '../lib/spec';

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

Example:
  const op = spec.paths['/api/v1/contenttype'].get
  return { summary: op.summary, params: op.parameters?.map(p => p.name) }`,
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
