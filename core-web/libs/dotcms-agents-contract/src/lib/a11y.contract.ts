import { z } from 'zod';

/**
 * Placeholder — replaced in the next step by the real a11y-fix agent contract
 * (the zod schemas currently in apps/dotcms-agents/src/a11y/domain/contract.ts).
 * Exists now only to verify both consumers (the Angular frontend and the Node
 * agent) can import this lib.
 */
export const ContractPing = z.object({ ok: z.literal(true) });
export type ContractPing = z.infer<typeof ContractPing>;
