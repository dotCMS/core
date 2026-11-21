import { byTestId, createHostFactory, mockProvider, SpectatorHost } from '@openng/spectator/vitest';
import { Mock, vi } from 'vitest';

import { Drawer } from 'primeng/drawer';
import { ZIndexUtils } from 'primeng/utils';

import { DotMessageService } from '@dotcms/data-access';
import { DotTitleStatus } from '@dotcms/dotcms-models';

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

    describe('title status chip', () => {
        const PANEL_WITH_STATUS = `
            <dot-side-panel heading="A very long heading" [titleStatus]="status">
                <p data-testid="panel-body">The source</p>
            </dot-side-panel>`;

        const openWithStatus = (status: DotTitleStatus): void => {
            spectator = createHost(PANEL_WITH_STATUS, {
                hostProps: { status } as unknown as { front: boolean }
            }) as unknown as typeof spectator;
            spectator.detectChanges();
        };

        it('shows no chip when the panel was not given a status', () => {
            open();

            expect(query('title-status-chip')).toBeNull();
        });

        it('shows no chip for a null status', () => {
            openWithStatus(null);

            expect(query('title-status-chip')).toBeNull();
        });

        it('shows the status badge right after the heading', () => {
            openWithStatus({ state: { live: true, working: true, hasLiveVersion: true } });

            const title = query('side-panel-title') as HTMLElement;
            const chip = query('title-status-chip') as HTMLElement;

            expect(chip.textContent?.trim()).toBe('Published');
            expect(
                title.compareDocumentPosition(chip) & Node.DOCUMENT_POSITION_FOLLOWING
            ).toBeTruthy();
        });

        it('reads "New" for a status with a null state', () => {
            openWithStatus({ state: null });

            expect(query('title-status-chip')?.textContent?.trim()).toBe('New');
        });

        it('lets the heading truncate while the chip keeps its size', () => {
            openWithStatus({ state: null });

            expect(query('side-panel-title')?.classList.contains('truncate')).toBe(true);
            expect(query('title-status-chip')?.classList.contains('shrink-0')).toBe(true);
        });
    });
});
