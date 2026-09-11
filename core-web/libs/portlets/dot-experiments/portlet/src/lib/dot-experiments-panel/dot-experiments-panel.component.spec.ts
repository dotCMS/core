import { createComponentFactory, Spectator, byTestId } from '@openng/spectator/vitest';
import { MockPipe } from 'ng-mocks';
import { vi } from 'vitest';

import { ButtonModule } from 'primeng/button';
import { Drawer, DrawerModule } from 'primeng/drawer';
import { ZIndexUtils } from 'primeng/utils';

import { DotExperimentsPanelStore } from '@dotcms/portlets/dot-experiments/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { DotExperimentsPanelComponent } from './dot-experiments-panel.component';

import { PANEL_EXPANDED_WIDTH, PANEL_WIDTH } from '../shared/constants';

const EXPANDED_STORAGE_KEY = 'dot-experiments-panel-expanded';

describe('DotExperimentsPanelComponent', () => {
    let spectator: Spectator<DotExperimentsPanelComponent>;
    let store: InstanceType<typeof DotExperimentsPanelStore>;

    const createComponent = createComponentFactory({
        component: DotExperimentsPanelComponent,
        // The message pipe is swapped for one that echoes its key, so every heading and accessible
        // name below asserts on the i18n key rather than on a translation this spec would have to
        // keep in step with `Language.properties`.
        overrideComponents: [
            [
                DotExperimentsPanelComponent,
                {
                    set: {
                        imports: [
                            DrawerModule,
                            ButtonModule,
                            MockPipe(DotMessagePipe, (key: string) => key)
                        ]
                    }
                }
            ]
        ],
        // The real store, not a mock: the panel's whole job is to be a projection of it, and a
        // mocked store would let the projection and the state drift apart unnoticed.
        providers: [DotExperimentsPanelStore]
    });

    beforeEach(() => {
        // The expanded preference is persisted, so it leaks between tests unless cleared.
        localStorage.clear();
        spectator = createComponent({ detectChanges: false });
        store = spectator.inject(DotExperimentsPanelStore, true);
    });

    // `ZIndexUtils.getCurrent` is spied on below to stand in for an overlay above the panel.
    // Nothing restores spies between tests in this project, and a leaked one makes every later
    // Escape look consumed — which silently turns the teardown assertion into a test that cannot
    // fail.
    afterEach(() => vi.restoreAllMocks());

    /**
     * `appendTo="body"` teleports the drawer out of the fixture and into `document.body`, so every
     * query for anything inside it has to start at the document root.
     */
    const panel = () => spectator.query(byTestId('experiments-panel'), { root: true });

    const surface = (testId: string) => spectator.query(byTestId(testId), { root: true });

    const title = () => surface('experiments-panel-title')?.textContent?.trim();

    /** PrimeNG renders the clickable `<button>` inside the `p-button` host. */
    const clickButton = (testId: string): void => {
        const button = spectator.query(byTestId(testId), { root: true })?.querySelector('button');
        spectator.click(button as HTMLElement);
    };

    /** Drawer width from the `pt.root.style` binding — what the editor actually sees. */
    const drawerWidth = (): string => (panel() as HTMLElement | null)?.style?.width ?? '';

    /**
     * The expand button's glyph, the user-visible reflection of the expanded state. Asserted
     * instead of the protected signal so the test fails when the button stops reflecting it.
     */
    const expandIcon = (): string | undefined =>
        spectator
            .query(byTestId('experiments-panel-expand'), { root: true })
            ?.querySelector('i')
            ?.textContent?.trim();

    /**
     * The accessible name, read from the descendant native `<button>` rather than the `p-button`
     * host: the name has to land on the focusable control, whose only content is an icon glyph.
     * Reading the host would also pass with `[attr.aria-label]`, which leaves it unnamed.
     */
    const expandAriaLabel = (): string | null | undefined =>
        spectator
            .query(byTestId('experiments-panel-expand'), { root: true })
            ?.querySelector('button')
            ?.getAttribute('aria-label');

    const pressEscape = (): void => {
        document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    };

    /**
     * Stands in for a drawer's modal mask and dispatches a bubbling click from it, as the browser
     * would. PrimeNG builds the real mask during the show animation, which jsdom never runs.
     *
     * `ownedByPanel` decides whose mask it is: assigned to this panel's drawer instance (what the
     * handler compares against) or left as a foreign drawer's, which must be ignored.
     */
    const clickMask = ({ ownedByPanel }: { ownedByPanel: boolean }): void => {
        const mask = document.createElement('div');
        mask.classList.add('p-drawer-mask');
        document.body.appendChild(mask);

        if (ownedByPanel) {
            spectator.query(Drawer).mask = mask;
        }

        mask.dispatchEvent(new MouseEvent('click', { bubbles: true }));
        mask.remove();
    };

    const openOn = (open: () => void): void => {
        open();
        spectator.detectChanges();
    };

    describe('what it shows', () => {
        it('should render nothing while the store is closed', () => {
            spectator.detectChanges();

            expect(panel()).toBeNull();
        });

        it('should render the list when the store opens', () => {
            openOn(() => store.open());

            expect(panel()).not.toBeNull();
            expect(surface('experiments-panel-list')).not.toBeNull();
            expect(title()).toBe('experiments.panel.title');
        });

        /**
         * One surface at a time (spec Assumptions). Asserted as the *absence* of the list on every
         * other view, because the failure this guards against is two surfaces rendering at once —
         * which a test that only checks the current one would never see.
         */
        it.each([
            { view: 'create', open: () => store.showCreate() },
            { view: 'configure', open: () => store.showConfigure('exp-1') },
            { view: 'results', open: () => store.showResults('exp-1') }
        ])('should not keep the list rendered on the $view view', ({ open }) => {
            openOn(() => store.open());
            expect(surface('experiments-panel-list')).not.toBeNull();

            openOn(open);

            expect(surface('experiments-panel-list')).toBeNull();
        });

        /**
         * The heading names the view rather than repeating "Experiments" four times, so each view
         * carries its own key.
         */
        it.each([
            {
                view: 'create',
                open: () => store.showCreate(),
                key: 'experiments.configure.header.new-experiment'
            },
            {
                view: 'configure',
                open: () => store.showConfigure('exp-1'),
                key: 'experiment.container.configuration.title'
            },
            {
                view: 'results',
                open: () => store.showResults('exp-1'),
                key: 'experiment.container.report.title'
            }
        ])('should title the $view view with its own key', ({ open, key }) => {
            openOn(() => store.open());
            openOn(open);

            expect(title()).toBe(key);
        });
    });

    describe('width', () => {
        it('should open at the panel width and widen on demand, persisting the choice', () => {
            openOn(() => store.open());

            expect(drawerWidth()).toBe(PANEL_WIDTH);
            expect(expandIcon()).toBe('open_in_full');
            expect(expandAriaLabel()).toBe('experiments.panel.action.expand');

            clickButton('experiments-panel-expand');
            spectator.detectChanges();

            expect(drawerWidth()).toBe(PANEL_EXPANDED_WIDTH);
            expect(expandIcon()).toBe('close_fullscreen');
            expect(expandAriaLabel()).toBe('experiments.panel.action.collapse');
            expect(localStorage.getItem(EXPANDED_STORAGE_KEY)).toBe('true');

            clickButton('experiments-panel-expand');
            spectator.detectChanges();

            expect(drawerWidth()).toBe(PANEL_WIDTH);
            expect(localStorage.getItem(EXPANDED_STORAGE_KEY)).toBe('false');
        });

        it('should open in the mode the editor last chose', () => {
            localStorage.setItem(EXPANDED_STORAGE_KEY, 'true');
            spectator = createComponent({ detectChanges: false });
            store = spectator.inject(DotExperimentsPanelStore, true);

            openOn(() => store.open());

            expect(drawerWidth()).toBe(PANEL_EXPANDED_WIDTH);
        });
    });

    describe('dismissal', () => {
        it('should close on the close button', () => {
            openOn(() => store.open());

            clickButton('experiments-panel-close');

            expect(store.isOpen()).toBe(false);
        });

        it('should close on Escape', () => {
            openOn(() => store.open());

            pressEscape();

            expect(store.isOpen()).toBe(false);
        });

        /**
         * Something opened from inside the panel — a confirm, a dialog — owns the key. The panel
         * must not close underneath it.
         */
        it('should leave Escape to an overlay stacked above it', () => {
            openOn(() => store.open());
            vi.spyOn(ZIndexUtils, 'getCurrent').mockReturnValue(100000);

            pressEscape();

            expect(store.isOpen()).toBe(true);
        });

        it('should close on a click on its own mask', () => {
            openOn(() => store.open());

            clickMask({ ownedByPanel: true });

            expect(store.isOpen()).toBe(false);
        });

        /**
         * `p-drawer-mask` is shared by every drawer in the app, so a class check would let another
         * drawer's mask close this panel. The handler compares against this drawer's own mask.
         */
        it('should ignore a click on another drawer’s mask', () => {
            openOn(() => store.open());

            clickMask({ ownedByPanel: false });

            expect(store.isOpen()).toBe(true);
        });

        /**
         * FR-039. The shell destroys the panel rather than hiding it, so a claim left behind would
         * have a dead component consuming Escape for the editor — invisible until someone presses
         * it and nothing happens.
         */
        it('should withdraw its Escape claim when it is destroyed', () => {
            openOn(() => store.open());

            spectator.fixture.destroy();
            store.open();
            pressEscape();

            expect(store.isOpen()).toBe(true);
        });
    });
});
