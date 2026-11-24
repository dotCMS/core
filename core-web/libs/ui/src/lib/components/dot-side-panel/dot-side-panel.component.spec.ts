import { byTestId, createHostFactory, mockProvider, SpectatorHost } from '@openng/spectator/vitest';
import { Mock, vi } from 'vitest';

import { Drawer } from 'primeng/drawer';
import { ZIndexUtils } from 'primeng/utils';

import { DotMessageService } from '@dotcms/data-access';

import { DotSidePanelComponent } from './dot-side-panel.component';

const EXPANDED_STORAGE_KEY = 'dot-edit-content-side-panel-expanded';

const PANEL = `
    <dot-side-panel heading="header.vtl" [front]="front">
        <p data-testid="panel-body">The source</p>
    </dot-side-panel>`;

describe('DotSidePanelComponent', () => {
    let spectator: SpectatorHost<DotSidePanelComponent, { front: boolean }>;
    let closeRequested: Mock;

    const createHost = createHostFactory({
        component: DotSidePanelComponent,
        providers: [mockProvider(DotMessageService, { get: (key: string) => key })],
        detectChanges: false
    });

    /** Renders the panel, in front of the others unless told otherwise. */
    const open = ({ front = true }: { front?: boolean } = {}): void => {
        spectator = createHost(PANEL, { hostProps: { front } });
        closeRequested = vi.fn();
        spectator.output('closeRequested').subscribe(closeRequested);
        spectator.detectChanges();
    };

    beforeEach(() => localStorage.clear());

    // `appendTo="body"` teleports the drawer out of the fixture, so DOM queries search from the
    // document root.
    const query = (testId: string): HTMLElement | null =>
        spectator.query<HTMLElement>(byTestId(testId), { root: true });

    /** The PrimeNG drawer the panel renders. */
    const drawer = (): Drawer => {
        const found = spectator.query(Drawer);

        if (!found) {
            throw new Error('The panel rendered no drawer');
        }

        return found;
    };

    const clickButton = (testId: string): void => {
        spectator.click(query(testId)?.querySelector('button') as HTMLElement);
    };

    const pressEscape = (): KeyboardEvent => {
        const event = new KeyboardEvent('keydown', {
            key: 'Escape',
            bubbles: true,
            cancelable: true
        });
        document.dispatchEvent(event);

        return event;
    };

    /**
     * Clicks a stand-in for a drawer's mask. PrimeNG creates the real one during the show
     * animation, which jsdom never runs.
     */
    const clickMask = ({ ownedByPanel }: { ownedByPanel: boolean }): HTMLElement => {
        const mask = document.createElement('div');
        mask.classList.add('p-drawer-mask');
        document.body.appendChild(mask);

        if (ownedByPanel) {
            drawer().mask = mask;
        }

        mask.dispatchEvent(new MouseEvent('click', { bubbles: true }));
        mask.remove();

        return mask;
    };

    describe('drawer', () => {
        it('shows the header and the content it is given', () => {
            open();

            expect(query('side-panel-title')?.textContent?.trim()).toBe('header.vtl');
            expect(query('panel-body')?.textContent).toBe('The source');
        });

        it('asks to close from the close button', () => {
            open();

            clickButton('side-panel-close');

            expect(closeRequested).toHaveBeenCalledTimes(1);
        });
    });

    describe('Escape', () => {
        it('asks the frontmost panel to close, and consumes the key', () => {
            open();

            const event = pressEscape();

            expect(closeRequested).toHaveBeenCalledTimes(1);
            expect(event.defaultPrevented).toBe(true);
        });

        it('consumes the key but leaves a panel that is not frontmost open', () => {
            open({ front: false });

            const event = pressEscape();

            expect(closeRequested).not.toHaveBeenCalled();
            expect(event.defaultPrevented).toBe(true);
        });

        it('leaves the key to an overlay above the panel', () => {
            open();
            // Stands in for a dialog opened from inside the panel: every PrimeNG overlay registers
            // itself in this shared stack when it opens.
            const overlay = document.createElement('div');
            ZIndexUtils.set('modal', overlay, 2000);

            pressEscape();

            expect(closeRequested).not.toHaveBeenCalled();
            // Shared global stack: leaving the entry behind would leak into later tests.
            ZIndexUtils.clear(overlay);
        });

        /**
         * A PrimeNG dialog under the panel listens to the document's keydown itself and closes when
         * it is the top of `ZIndexUtils`. The panel's close destroys its drawer, which drops the
         * drawer's entry, so a dialog listener later in the same event found itself on top and
         * closed too: one Escape closed two layers (#38303, US3 AC 3, SC-007).
         */
        describe('with a dialog under the panel', () => {
            let laterListener: Mock;

            beforeEach(() => {
                laterListener = vi.fn();
            });

            afterEach(() => document.removeEventListener('keydown', laterListener));

            it('stops the Escape that closes the panel before any later document listener', () => {
                open();
                document.addEventListener('keydown', laterListener);

                pressEscape();

                expect(closeRequested).toHaveBeenCalledTimes(1);
                expect(laterListener).not.toHaveBeenCalled();
            });

            it('lets an overlay above the panel hear the Escape it owns', () => {
                open();
                const overlay = document.createElement('div');
                ZIndexUtils.set('modal', overlay, 2000);
                document.addEventListener('keydown', laterListener);

                pressEscape();

                expect(laterListener).toHaveBeenCalledTimes(1);
                ZIndexUtils.clear(overlay);
            });

            it('lets the dialog hear Escape again once the panel is gone', () => {
                open();
                document.addEventListener('keydown', laterListener);

                spectator.fixture.destroy();
                pressEscape();

                expect(laterListener).toHaveBeenCalledTimes(1);
            });
        });
    });

    describe('mask click', () => {
        it('asks to close on a click on its own mask', () => {
            open();

            clickMask({ ownedByPanel: true });

            expect(closeRequested).toHaveBeenCalledTimes(1);
        });

        it("ignores a click on another drawer's mask", () => {
            open();

            clickMask({ ownedByPanel: false });

            expect(closeRequested).not.toHaveBeenCalled();
        });

        it('ignores a click on its mask when another panel is in front', () => {
            open({ front: false });

            clickMask({ ownedByPanel: true });

            expect(closeRequested).not.toHaveBeenCalled();
        });
    });

    describe('when it closes', () => {
        it('gives Escape and the mask back', () => {
            open();
            const mask = document.createElement('div');
            drawer().mask = mask;

            spectator.fixture.destroy();

            expect(pressEscape().defaultPrevented).toBe(false);
            document.body.appendChild(mask);
            mask.dispatchEvent(new MouseEvent('click', { bubbles: true }));
            mask.remove();
            expect(closeRequested).not.toHaveBeenCalled();
        });
    });

    describe('full width', () => {
        const width = (): string =>
            spectator.query<HTMLElement>('.p-drawer', { root: true })?.style.width ?? '';

        // Read from the native `<button>`, the focusable control the name has to land on.
        const expandLabel = (): string | null | undefined =>
            query('side-panel-expand')?.querySelector('button')?.getAttribute('aria-label');

        it('opens at 80% and expands to the full width, remembering the choice', () => {
            open();

            expect(width()).toBe('80%');
            expect(expandLabel()).toBe('edit.content.side-panel.expand');

            clickButton('side-panel-expand');
            spectator.detectChanges();

            expect(width()).toBe('100%');
            expect(expandLabel()).toBe('edit.content.side-panel.collapse');
            expect(localStorage.getItem(EXPANDED_STORAGE_KEY)).toBe('true');
        });

        it('opens at the full width when that was the last choice', () => {
            localStorage.setItem(EXPANDED_STORAGE_KEY, 'true');

            open();

            expect(width()).toBe('100%');
        });
    });
});
