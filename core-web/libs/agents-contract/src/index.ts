/**
 * @dotcms/agent-contracts — shared wire contracts for the dotcms-agents service.
 *
 * Single source of truth for the request/response shapes exchanged between the
 * Node agent (apps/ai-agents), the dotCMS proxy, and the Studio frontend.
 * Schemas are zod (the agent validates against them); consumers that only need
 * types import the inferred `z.infer` exports. One namespace per agent capability.
 */
export * from './lib/a11y.contract';
