import { normalizeAxe, type RawScanResponse } from './dotcms-client';

/**
 * The agent treats the scanner as "pure axe" and normalizes it here. These tests
 * lock the raw-axe → ScanResult mapping, especially that the per-node `data`
 * (fgColor/bgColor/ratio) survives — it's what makes contrast fixing deterministic.
 */
describe('normalizeAxe', () => {
    const raw: RawScanResponse = {
        ok: true,
        documentTitle: 'Home',
        stylesheets: ['https://cdn/fonts.css', 'http://localhost:8080/theme/styles.dotsass'],
        axe: {
            violations: [
                {
                    id: 'color-contrast',
                    impact: 'serious',
                    description: 'Ensures contrast…',
                    help: 'Elements must meet contrast thresholds',
                    helpUrl: 'https://dequeuniversity.com/…',
                    nodes: [
                        {
                            target: ['#book'],
                            html: '<a class="button-primary" id="book">Book</a>',
                            impact: 'serious',
                            any: [
                                {
                                    id: 'color-contrast',
                                    data: {
                                        fgColor: '#ffffff',
                                        bgColor: '#e76300',
                                        contrastRatio: 3.39,
                                        expectedContrastRatio: '4.5:1'
                                    }
                                }
                            ],
                            all: [],
                            none: []
                        },
                        {
                            target: ['a.btn'],
                            html: '<a class="btn">Link</a>',
                            any: [{ id: 'color-contrast', data: { fgColor: '#999', bgColor: '#fff' } }]
                        }
                    ]
                }
            ],
            incomplete: [
                {
                    id: 'aria-roles',
                    help: 'ARIA roles must be valid',
                    nodes: [{ target: ['.x'], html: '<div class="x"></div>', any: [] }]
                }
            ],
            passes: [{ id: 'document-title', nodes: [] }] // should be ignored
        }
    };

    it('flattens violation rules into one finding per node', () => {
        const r = normalizeAxe(raw);
        const cc = r.findings.items.filter((i) => i.code === 'color-contrast');
        expect(cc).toHaveLength(2); // two nodes → two findings
        expect(r.findings.violations).toBe(2);
    });

    it('carries the axe check `data` (fg/bg/ratio) onto the finding', () => {
        const r = normalizeAxe(raw);
        const book = r.findings.items.find((i) => i.selector === '#book');
        expect(book?.data).toMatchObject({
            fgColor: '#ffffff',
            bgColor: '#e76300',
            contrastRatio: 3.39,
            expectedContrastRatio: '4.5:1'
        });
    });

    it('maps target[] to a selector string and html to context', () => {
        const r = normalizeAxe(raw);
        const book = r.findings.items.find((i) => i.selector === '#book');
        expect(book?.context).toContain('id="book"');
        expect(book?.resultType).toBe('violation');
    });

    it('treats incomplete as needs-review (not violations)', () => {
        const r = normalizeAxe(raw);
        expect(r.findings.needsReview).toBe(1);
        const inc = r.findings.items.find((i) => i.code === 'aria-roles');
        expect(inc?.resultType).toBe('needsReview');
        expect(inc?.type).toBe('warning');
    });

    it('ignores passes/inapplicable and preserves stylesheets', () => {
        const r = normalizeAxe(raw);
        expect(r.findings.items.some((i) => i.code === 'document-title')).toBe(false);
        expect(r.stylesheets).toEqual([
            'https://cdn/fonts.css',
            'http://localhost:8080/theme/styles.dotsass'
        ]);
    });

    it('handles an empty / missing axe object', () => {
        const r = normalizeAxe({ ok: true });
        expect(r.totalIssues).toBe(0);
        expect(r.findings.items).toEqual([]);
    });
});
