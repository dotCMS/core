/**
 * Type helpers for MCP SDK Zod schema registration.
 *
 * The MCP SDK expects `inputSchema` to be of type `ZodRawShapeCompat | AnySchema`.
 * While the SDK supports Zod v4 schemas at runtime (via `isZ4Schema()` detection),
 * TypeScript cannot infer the correct types due to differences between:
 * - `zod` package exports (v4 "classic" API)
 * - `zod/v4/core` types expected by MCP SDK
 *
 * This utility provides type-safe wrappers to bridge this gap without
 * scattering `as any` casts throughout the codebase.
 *
 * @see https://github.com/modelcontextprotocol/typescript-sdk/blob/main/src/server/zod-compat.ts
 */

import type { AnySchema } from '@modelcontextprotocol/sdk/server/zod-compat.js';
import type { ZodTypeAny } from 'zod';

/**
 * Converts a Zod schema to the AnySchema type expected by MCP SDK's registerTool().
 *
 * This is a type-only helper - the runtime value is unchanged. The MCP SDK's
 * `isZ4Schema()` function correctly detects and handles Zod v4 schemas.
 *
 * @param schema - Any Zod v4 schema (z.object(), z.string(), etc.)
 * @returns The same schema typed as AnySchema for MCP SDK compatibility
 *
 * @example
 * ```typescript
 * const MySchema = z.object({ name: z.string() });
 *
 * server.registerTool('my_tool', {
 *   inputSchema: asMcpSchema(MySchema)
 * }, handler);
 * ```
 */
export function asMcpSchema<T extends ZodTypeAny>(schema: T): AnySchema {
    // Runtime: schema is unchanged
    // Compile-time: TypeScript sees it as AnySchema
    return schema as unknown as AnySchema;
}
