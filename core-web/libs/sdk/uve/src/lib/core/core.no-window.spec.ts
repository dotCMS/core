// @vitest-environment node

import { describe, expect, it } from 'vitest';

import { getUVEState } from './core.utils';

/**
 * The `typeof window === 'undefined'` branch of getUVEState(), which is what a server
 * render hits. It needs its own file because it needs its own environment: under jsdom
 * `window` exists and is a non-configurable global, so the branch is unreachable — see
 * the note in core.spec.ts.
 */
describe('getUVEState without a window', () => {
    it('should return undefined when there is no window', () => {
        expect(typeof window).toBe('undefined');
        expect(getUVEState()).toBe(undefined);
    });
});
