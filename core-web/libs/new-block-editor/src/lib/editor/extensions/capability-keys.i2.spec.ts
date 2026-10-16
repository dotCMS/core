import { describe, expect, it } from 'vitest';

import {
    consumedKeysFromCatalog,
    consumedKeysFromSource,
    producibleOptionCodes
} from './capability-keys.testing';

/**
 * I2 — no producible option without a destination.
 *
 * The inverse of I1. Every option the Settings tab can write into a field's `allowedBlocks` must
 * mean something to the editor. An option that resolves to nothing is a checkbox that does
 * nothing: the administrator ticks it, the field becomes restricted, and the capability they
 * asked for is nowhere — because the editor is asking about a different identifier.
 *
 * This is the other half of the `aiContent` / `aiImage` defect and the more perverse one. The
 * Settings tab emits `aiContentPrompt` / `aiImagePrompt`; the slash menu consults `aiContent` /
 * `aiImage`. So ticking "AI Content" is exactly what hides AI Content, and not ticking it hides
 * it too. On a restricted field there is no configuration that shows it at all.
 *
 * Contract: specs/37601-allowed-blocks-capability-contract/contracts/capability-key-registry.md
 */
describe('I2 — every option the Settings tab can emit resolves to a destination', () => {
    it('sanity: both sides are reachable and non-empty', () => {
        // Without this, an import that silently yields an empty set would make the assertion
        // below pass while proving nothing.
        expect(producibleOptionCodes().size).toBeGreaterThan(0);
        expect(consumedKeysFromCatalog().size).toBeGreaterThan(0);
        expect(consumedKeysFromSource().size).toBeGreaterThan(0);
    });

    it('has no option that nothing consumes', () => {
        const consumed = new Set([
            ...consumedKeysFromCatalog(),
            ...consumedKeysFromSource().keys()
        ]);

        const orphans = [...producibleOptionCodes()].filter((code) => !consumed.has(code)).sort();

        expect(
            orphans,
            `The Settings tab offers options nothing in the editor consults: ${orphans.join(', ')}.\n` +
                `Ticking one of these restricts the field without enabling anything — the capability\n` +
                `it names is gated on a different identifier, so it can never appear.\n` +
                `Fix the consumer to use the key Settings emits; do not delete the option.`
        ).toEqual([]);
    });
});
