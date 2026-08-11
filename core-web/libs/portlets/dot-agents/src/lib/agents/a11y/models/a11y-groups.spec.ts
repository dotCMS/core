import { PageScannerA11yResponse } from '@dotcms/portlets/dot-ema/ui';

import { buildA11yGroups } from './a11y-groups';

/** Wrap axe nodes in the scanner response shape `buildA11yGroups` consumes. */
function scanWith(target: string[] | undefined): PageScannerA11yResponse {
    return {
        ok: true,
        standard: 'WCAG2AA',
        axe: {
            violations: [
                {
                    id: 'button-name',
                    impact: 'serious',
                    description: 'Buttons must have discernible text',
                    help: '',
                    helpUrl: 'https://example.com/button-name',
                    tags: [],
                    nodes: [{ html: '<button>', target, impact: 'serious', failureSummary: '' }]
                }
            ],
            incomplete: []
        }
    } as unknown as PageScannerA11yResponse;
}

describe('buildA11yGroups', () => {
    describe('selector', () => {
        it('takes the LAST entry of the target chain, not a joined list', () => {
            // axe's `target` is an ancestor chain — one entry per frame or shadow-root
            // boundary crossed — so the element's own selector is the last one. Joining
            // them made a selector LIST, and `querySelector` returns whichever matches
            // FIRST, so the marker overlay outlined the iframe instead of the button.
            const [group] = buildA11yGroups(scanWith(['iframe#promo', 'button.cta']));

            expect(group.items[0].selector).toBe('button.cta');
            expect(group.items[0].selector).not.toContain('iframe');
        });

        it('uses the only entry when the element is not nested', () => {
            const [group] = buildA11yGroups(scanWith(['button.cta']));
            expect(group.items[0].selector).toBe('button.cta');
        });

        it('handles a deeper chain (frame inside a frame)', () => {
            const [group] = buildA11yGroups(scanWith(['iframe#outer', 'iframe#inner', 'a.link']));
            expect(group.items[0].selector).toBe('a.link');
        });

        it('falls back to an empty selector when axe reported no target', () => {
            expect(buildA11yGroups(scanWith(undefined))[0].items[0].selector).toBe('');
            expect(buildA11yGroups(scanWith([]))[0].items[0].selector).toBe('');
        });
    });

    it('returns nothing for a null or axe-less payload', () => {
        expect(buildA11yGroups(null)).toEqual([]);
        expect(buildA11yGroups({ ok: true } as unknown as PageScannerA11yResponse)).toEqual([]);
    });
});
