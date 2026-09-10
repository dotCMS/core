import { byTestId, createHostFactory, SpectatorHost } from '@openng/spectator/jest';

import { DotTruncatedLabelComponent } from './dot-truncated-label.component';

describe('DotTruncatedLabelComponent', () => {
    let spectator: SpectatorHost<DotTruncatedLabelComponent>;

    const createHost = createHostFactory(DotTruncatedLabelComponent);

    const clip = () => spectator.query(byTestId('tree-node-label-clip'));

    /**
     * `offsetWidth` / `scrollWidth` are always 0 under jsdom, and PrimeNG's `showOnEllipsis` gate
     * compares exactly those two. Forcing them is what makes both branches of the gate reachable
     * in a unit test — the pattern already used in `host-folder-field.component.spec.ts`.
     */
    const setOverflow = (element: Element, { fits }: { fits: boolean }): void => {
        Object.defineProperty(element, 'offsetWidth', { value: 100, configurable: true });
        Object.defineProperty(element, 'scrollWidth', {
            value: fits ? 100 : 400,
            configurable: true
        });
    };

    const hover = (element: Element): void => {
        element.dispatchEvent(new MouseEvent('mouseenter'));
        // Change detection before the timers: the tooltip text is read from the rendered content
        // when the pointer arrives, and PrimeNG only reads it when the show delay elapses.
        spectator.detectChanges();
        jest.advanceTimersByTime(1000);
    };

    const tooltipText = (): string | null =>
        document.querySelector('.p-tooltip-text')?.textContent?.trim() ?? null;

    describe('single-line clipping', () => {
        it('should render the projected content', () => {
            spectator = createHost(
                `<dot-truncated-label>a-very-long-folder-name-that-does-not-fit</dot-truncated-label>`
            );

            expect(spectator.element.textContent?.trim()).toBe(
                'a-very-long-folder-name-that-does-not-fit'
            );
        });

        it('should wrap the projected content in a single-line clipping element', () => {
            // Structural assertion on purpose: Tailwind's stylesheet is not loaded under jsdom, so
            // `truncate` resolves to no computed style here and `scrollWidth` is always 0. What can
            // be verified at this level is that the clipping element exists, carries the classes,
            // and holds the content. The visual proof is quickstart.md items 1, 5 and 6.
            spectator = createHost(`<dot-truncated-label>documents</dot-truncated-label>`);

            const element = clip();

            expect(element).toBeTruthy();
            expect(element).toHaveClass('truncate');
            expect(element).toHaveClass('min-w-0');
            expect(element?.textContent?.trim()).toBe('documents');
        });

        it('should keep the clipping element as the only wrapper around the content', () => {
            // Guards against a second nested wrapper appearing later: the tooltip measures the
            // element it sits on, so the element that clips and the element that reveals must be
            // the same one (research.md R2).
            spectator = createHost(`<dot-truncated-label>images</dot-truncated-label>`);

            expect(spectator.queryAll(byTestId('tree-node-label-clip'))).toHaveLength(1);
        });
    });

    describe('overflow tooltip', () => {
        beforeEach(() => {
            jest.useFakeTimers();
        });

        afterEach(() => {
            jest.useRealTimers();
            document.querySelectorAll('.p-tooltip').forEach((node) => node.remove());
        });

        it('should reveal the full text on hover when the name is clipped', () => {
            spectator = createHost(
                `<dot-truncated-label>a-very-long-folder-name-that-does-not-fit</dot-truncated-label>`
            );
            const element = clip() as HTMLElement;
            setOverflow(element, { fits: false });

            hover(element);

            expect(tooltipText()).toBe('a-very-long-folder-name-that-does-not-fit');
        });

        it('should dismiss the tooltip when the pointer leaves', () => {
            spectator = createHost(
                `<dot-truncated-label>a-very-long-folder-name-that-does-not-fit</dot-truncated-label>`
            );
            const element = clip() as HTMLElement;
            setOverflow(element, { fits: false });
            hover(element);

            element.dispatchEvent(new MouseEvent('mouseleave'));
            jest.advanceTimersByTime(1000);

            expect(document.querySelector('.p-tooltip')).toBeNull();
        });

        it('should not show a tooltip when the name fits', () => {
            spectator = createHost(`<dot-truncated-label>documents</dot-truncated-label>`);
            const element = clip() as HTMLElement;
            setOverflow(element, { fits: true });

            hover(element);

            expect(document.querySelector('.p-tooltip')).toBeNull();
        });

        it('should reveal the wording the row displays, not an underlying value', () => {
            // FR-012: the Asset Picker labels the tree root with a localized string while the
            // node still carries the hostname. Reading the tooltip from the rendered content is
            // what keeps the two from disagreeing.
            spectator = createHost(`<dot-truncated-label>Site root</dot-truncated-label>`);
            const element = clip() as HTMLElement;
            setOverflow(element, { fits: false });

            hover(element);

            expect(tooltipText()).toBe('Site root');
        });
    });
});
