/**
 * The a11y-fix agent contract now lives in the shared lib @dotcms/agent-contracts
 * (single source of truth for the agent + the Studio frontend). This module
 * re-exports it so the agent's internal imports (`../domain/contract`) stay stable.
 */
export * from '@dotcms/agent-contracts';
