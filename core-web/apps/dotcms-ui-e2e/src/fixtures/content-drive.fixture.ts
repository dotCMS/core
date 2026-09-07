import { test as base } from './base.fixture';

/**
 * Content Drive e2e fixture — re-exports the shared base helpers.
 * Extend here when Content Drive needs feature-specific API helpers.
 */
export const test = base;

export { expect } from './base.fixture';
