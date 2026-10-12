import { byTestId, createHostFactory, mockProvider, SpectatorHost } from '@openng/spectator/vitest';
import { Mock, vi } from 'vitest';

import { Drawer } from 'primeng/drawer';
import { ZIndexUtils } from 'primeng/utils';

import { DotMessageService } from '@dotcms/data-access';

import { DotSidePanelComponent } from './dot-side-panel.component';

import { DotSidePanelNavController } from '../../services/dot-side-panel-nav.service';

const EXPANDED_STORAGE_KEY = 'dot-edit-content-side-panel-expanded';

const PANEL = `
    <dot-side-panel heading="header.vtl">
        <p data-testid="panel-body">The source</p>
    </dot-side-panel>`;

const PANEL_WITH_FOOTER = `
    <dot-side-panel heading="header.vtl">
        <p data-testid="panel-body">The source</p>
        <ng-template #footer>
            <button data-testid="panel-save">Save</button>
        </ng-template>
    </dot-side-panel>`;

describe('DotSidePanelComponent', () => {
    let spectator: SpectatorHost<DotSidePanelComponent>;
    let navController: { acquire: Mock; release: Mock; isTop: Mock };
    let closeRequested: Mock;

    const createHost = createHostFactory({
        component: DotSidePanelComponent,
        providers: [
            mockProvider(DotMessageService, { get: (key: string) => key }),
            // Read per creation, so each test's controller reaches the panel.
            { provide: DotSidePanelNavController, useFactory: () => navController }
        ],
        detectChanges: false
    });

    const open = (template = PANEL): void => {
        spectator = createHost(template);
        closeRequested = vi.fn();
        spectator.output('closeRequested').subscribe(closeRequested);
        spectator.detectChanges();
    };

    beforeEach(() => {
        localStorage.clear();
        navController = {
            acquire: vi.fn(),
            release: vi.fn(),
            isTop: vi.fn().mockReturnValue(true)
        };
    });

    // `appendTo="body"` teleports the drawer out of the fixture, so DOM queries search from the
    // document root.
    const query = (testId: string): HTMLElement | null =>
        spectator.query<HTMLElement>(byTestId(testId), { root: true });

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
            spectator.query(Drawer).mask = mask;
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

        it('draws no footer bar without a footer', () => {
            open();

            expect(spectator.query('.p-drawer-footer', { root: true })).toBeNull();
        });

        it("draws the owner's footer", () => {
            open(PANEL_WITH_FOOTER);

            expect(
                spectator.query('.p-drawer-footer', { root: true })?.contains(query('panel-save'))
            ).toBe(true);
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
            open();
            navController.isTop.mockReturnValue(false);

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

        it('is the same handling when a panel forwards an Escape the registry cannot see', () => {
            open();

            expect(spectator.component.escape()).toBe(true);
            expect(closeRequested).toHaveBeenCalledTimes(1);
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
            open();
            navController.isTop.mockReturnValue(false);

            clickMask({ ownedByPanel: true });

            expect(closeRequested).not.toHaveBeenCalled();
        });
    });

    describe('side-panel stack', () => {
        it('joins the stack after the first render', () => {
            open();

            expect(navController.acquire).toHaveBeenCalledWith(spectator.component);
        });

        it('leaves the stack, and gives Escape and the mask back, when destroyed', () => {
            open();
            const { component } = spectator;
            const drawer = spectator.query(Drawer);
            const mask = document.createElement('div');
            drawer.mask = mask;

            spectator.fixture.destroy();

            expect(navController.release).toHaveBeenCalledWith(component);
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
