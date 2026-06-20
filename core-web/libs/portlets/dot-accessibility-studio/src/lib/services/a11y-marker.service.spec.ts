import { A11yMarkerService } from './a11y-marker.service';

import { A11yGroup } from '../models/a11y-groups';

const GROUPS: A11yGroup[] = [
    {
        code: 'image-alt',
        type: 'error',
        message: 'Images must have alternate text',
        impact: 'critical',
        helpUrl: '',
        items: [
            { context: '<img>', selector: 'img.hero' },
            { context: '<img>', selector: 'img.logo' }
        ],
        count: 2
    },
    {
        code: 'color-contrast',
        type: 'warning',
        message: 'Low contrast',
        impact: 'moderate',
        helpUrl: '',
        items: [{ context: '<a>', selector: 'a.cta' }],
        count: 1
    }
];

const MARKER_SELECTOR = '[data-a11y-marker]';

describe('A11yMarkerService', () => {
    let service: A11yMarkerService;
    let iframe: HTMLIFrameElement;

    beforeEach(() => {
        service = new A11yMarkerService();
        iframe = document.createElement('iframe');
        document.body.appendChild(iframe);
        const doc = iframe.contentDocument as Document;
        doc.body.innerHTML = `
            <img class="hero" />
            <img class="logo" />
            <a class="cta">x</a>
            <p class="not-flagged">y</p>
        `;
        // jsdom does no layout — every getBoundingClientRect() is 0×0, which the
        // service skips. Give flagged elements a non-zero box so markers render.
        doc.querySelectorAll('img, a').forEach((el, i) => {
            (el as HTMLElement).getBoundingClientRect = () =>
                ({
                    top: 100 + i * 50,
                    left: 20,
                    width: 200,
                    height: 40,
                    right: 220,
                    bottom: 140 + i * 50,
                    x: 20,
                    y: 100 + i * 50,
                    toJSON: () => ({})
                }) as DOMRect;
        });
    });

    afterEach(() => iframe.remove());

    it('injects one marker per flagged element that resolves', () => {
        service.render(iframe, GROUPS);
        const markers = iframe.contentDocument?.querySelectorAll(MARKER_SELECTOR);
        expect(markers?.length).toBe(3);
    });

    it('tags markers with the rule code', () => {
        service.render(iframe, GROUPS);
        const codes = Array.from(
            iframe.contentDocument?.querySelectorAll(MARKER_SELECTOR) ?? []
        ).map((el) => el.getAttribute('data-a11y-marker'));
        expect(codes).toEqual(['image-alt', 'image-alt', 'color-contrast']);
    });

    it('colors markers by finding type', () => {
        service.render(iframe, GROUPS);
        const markers = Array.from(
            iframe.contentDocument?.querySelectorAll<HTMLElement>(MARKER_SELECTOR) ?? []
        );
        // error → orange, warning → red
        expect(markers[0].style.border).toContain('#f59e0b');
        expect(markers[2].style.border).toContain('#dc2626');
    });

    it('skips selectors that do not match any element', () => {
        service.render(iframe, [
            { ...GROUPS[0], items: [{ context: '', selector: '.does-not-exist' }], count: 1 }
        ]);
        expect(iframe.contentDocument?.querySelectorAll(MARKER_SELECTOR).length).toBe(0);
    });

    it('ignores invalid selectors without throwing', () => {
        expect(() =>
            service.render(iframe, [
                { ...GROUPS[0], items: [{ context: '', selector: ')))bad' }], count: 1 }
            ])
        ).not.toThrow();
        expect(iframe.contentDocument?.querySelectorAll(MARKER_SELECTOR).length).toBe(0);
    });

    it('clears prior markers before re-rendering (no duplicates)', () => {
        service.render(iframe, GROUPS);
        service.render(iframe, GROUPS);
        expect(iframe.contentDocument?.querySelectorAll(MARKER_SELECTOR).length).toBe(3);
    });

    it('clear() removes all markers', () => {
        service.render(iframe, GROUPS);
        service.clear(iframe);
        expect(iframe.contentDocument?.querySelectorAll(MARKER_SELECTOR).length).toBe(0);
    });

    it('no-ops when the iframe is null', () => {
        expect(() => service.render(null, GROUPS)).not.toThrow();
        expect(() => service.clear(null)).not.toThrow();
    });
});
