import { describe, expect, it } from 'vitest';

import {
    consumedKeysFromCatalog,
    consumedKeysFromSource,
    EXEMPT_KEYS,
    producibleOptionCodes,
    scannedFiles
} from './capability-keys.testing';

/**
 * I1 — no gate on a non-producible key.
 *
 * A capability may be gated on Allowed Blocks only if the Settings tab can produce its
 * identifier. A key that is consulted but not producible is permanently forbidden the instant an
 * administrator restricts the field to anything at all — an outcome nobody chose and nobody can
 * undo. Gating such a capability is always a defect, never a configuration.
 *
 * This test would have caught, in order:
 *   #36351 / FD 37852 — the hyperlink button vanishing on any restricted field. FD 38349 hit the
 *                       same defect a fortnight later and was closed as *user error* while #36351
 *                       sat open; it went unfixed for two more months.
 *   #37175 / FD 38997, FD 38993 — `link`, `emoji` and `youtube` gated together.
 *   #37340 / FD 39197 — the emoji gate, removed only as a side effect of a different fix.
 *
 * Six rounds of this class in sixteen months, every one triaged as an isolated incident, and the
 * same workaround — "uncheck everything under Allowed Blocks and save" — given to four customers.
 * A prose contract did not stop any of it. A failing build will.
 *
 * Contract: specs/37601-allowed-blocks-capability-contract/contracts/capability-key-registry.md
 */
describe('I1 — every consulted capability key is producible by the Settings tab', () => {
    const producible = producibleOptionCodes();
    const isAllowedKey = (key: string) => producible.has(key) || EXEMPT_KEYS.includes(key);

    it('sanity: the producer is reachable and non-empty', () => {
        // Guards against the invariant passing vacuously. `getEditorBlockOptions()` emits
        // `{ label, code }` — reading `.id` instead would yield undefined for every entry and
        // make every assertion below trivially true.
        expect(producible.size).toBeGreaterThan(0);
        expect([...producible].every((code) => typeof code === 'string' && code.length > 0)).toBe(
            true
        );
    });

    it('slash-menu catalog: every blockName is producible', () => {
        const offenders = [...consumedKeysFromCatalog()].filter((key) => !isAllowedKey(key)).sort();

        expect(
            offenders,
            `Gated on keys the Settings tab cannot produce: ${offenders.join(', ')}.\n` +
                `Settings emits: ${[...producible].sort().join(', ')}.\n` +
                `Fix the key, or ungate the capability — never add it to EXEMPT_KEYS to go green.`
        ).toEqual([]);
    });

    it('toolbar and popovers: every gate literal is producible', () => {
        const consumed = consumedKeysFromSource();
        const offenders = [...consumed.entries()]
            .filter(([key]) => !isAllowedKey(key))
            .map(([key, files]) => `${key} (${files.join(', ')})`)
            .sort();

        expect(
            offenders,
            `Gated on keys the Settings tab cannot produce:\n  ${offenders.join('\n  ')}`
        ).toEqual([]);
    });

    it('does not mistake popover ids or mark lookups for capability gates', () => {
        // `popovers.isOpen('link')`, `popovers.toggle('emoji')` and `m.type.name === 'link'` all
        // live in the scanned files. `link` and `emoji` were correctly ungated by #37539 and
        // #37442, so reporting them would be a false positive — and a false positive here gets
        // "fixed" by weakening the invariant. If this fails, repair the regex in
        // capability-keys.testing.ts, not the production code.
        const consumed = [...consumedKeysFromSource().keys()];

        expect(consumed).not.toContain('link');
        expect(consumed).not.toContain('emoji');
    });

    it('walks the whole editor tree rather than a hand-maintained list', () => {
        // The textual half only sees the files it reaches. Walking `editor/` means a gate added
        // in a NEW file is caught without anyone remembering to register it — the first draft of
        // this invariant used a fixed list and omitted `editor-extensions.ts`, the single most
        // consequential file in it.
        const files = scannedFiles();

        // The files that carried gates when this was written. If the walk stops reaching any of
        // them, it is silently scanning less than it claims.
        const mustReach = [
            'extensions/editor-extensions.ts',
            'components/toolbar/toolbar.component.html',
            'components/toolbar/toolbar.component.ts',
            'components/asset-by-url-popover/asset-by-url-popover.component.ts'
        ];

        for (const file of mustReach) {
            expect(files.some((found) => found.endsWith(file))).toBe(true);
        }

        // Test code is excluded on purpose: an `isAllowed` stub in a fixture is not a gate.
        expect(files.some((found) => /\.(spec|testing)\.ts$/.test(found))).toBe(false);
        expect(consumedKeysFromSource().size).toBeGreaterThan(0);
    });
});
