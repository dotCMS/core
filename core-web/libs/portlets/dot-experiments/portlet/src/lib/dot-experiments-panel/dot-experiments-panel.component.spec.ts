import { createComponentFactory, Spectator, byTestId } from '@openng/spectator/vitest';
import { MockComponent, MockPipe } from 'ng-mocks';
import { vi } from 'vitest';

import { ConfirmEventType } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { Drawer, DrawerModule } from 'primeng/drawer';
import { ZIndexUtils } from 'primeng/utils';

import { DotExperimentsPanelStore } from '@dotcms/portlets/dot-experiments/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { DotExperimentsPanelComponent } from './dot-experiments-panel.component';

import { DotExperimentsConfigureComponent } from '../dot-experiments-configure/dot-experiments-configure.component';
import { DotExperimentsListComponent } from '../dot-experiments-list/dot-experiments-list.component';
import { DotExperimentsResultsComponent } from '../dot-experiments-results/dot-experiments-results.component';
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
                            // Mocked: this spec is about the panel's chrome and its one-surface
                            // rule. The list screen has its own spec, and rendering the real one
                            // here would drag its store and every service behind it into a test
                            // that asserts nothing about them.
                            MockComponent(DotExperimentsListComponent),
                            MockComponent(DotExperimentsConfigureComponent),
                            MockComponent(DotExperimentsResultsComponent),
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

    /**
     * FR-019. Every way out of this panel — the X, the mask, Escape — is a click, not a
     * navigation, so `experimentsUnsavedChangesGuard` cannot see any of them. Without this the
     * editor loses everything typed since the last Save Draft to a stray click on a backdrop.
     */
    /**
     * FR-025f / FR-037. Results is the only screen here that pulls a charting library, and the
     * panel is opened far more often to glance at the list than to read a report. Its own `@defer`
     * is what keeps that cost off every other opening.
     *
     * `@defer` is usable here and was not in the shell (T034), and the difference is worth keeping
     * straight so neither is 'corrected' to match the other: results and the panel are in the same
     * lib, so the static `imports:` entry a defer block still needs crosses no Nx boundary.
     */
    describe('deferring the results screen', () => {
        it('should not render results when the panel opens on the list', () => {
            openOn(() => store.open());

            expect(spectator.query(DotExperimentsResultsComponent)).toBeNull();
        });

        it('should not render results while configuring either', () => {
            openOn(() => {
                store.open();
                store.showConfigure('exp-1');
            });

            expect(spectator.query(DotExperimentsResultsComponent)).toBeNull();
        });

        it('should render results only once they are the view', async () => {
            openOn(() => store.openResults('exp-1'));

            await vi.waitFor(() => {
                spectator.detectChanges();
                expect(spectator.query(DotExperimentsResultsComponent)).not.toBeNull();
            });
        });
    });

    describe('closing over unsaved work', () => {
        /**
         * Puts the Configure screen on show and gives its mock the two members the prompt reads.
         * The real component is not mounted on purpose — this spec is about what the panel does
         * with the answer, and the dialog's own behaviour has its own spec.
         */
        const configureOnScreen = (hasUnsavedChanges: boolean) => {
            openOn(() => {
                store.open();
                store.showConfigure('exp-1');
            });

            const confirm = vi.fn();
            const configure = spectator.query(DotExperimentsConfigureComponent);
            Object.assign(configure, {
                store: { $hasUnsavedChanges: () => hasUnsavedChanges },
                confirmationService: { confirm }
            });

            return confirm;
        };

        // `clickButton` looks at the document root: `appendTo="body"` teleports the drawer out of
        // the fixture, so a plain query finds nothing.
        const close = () => clickButton('experiments-panel-close');

        it('should ask before closing, and stay open while the answer is pending', async () => {
            const confirm = configureOnScreen(true);

            close();

            await vi.waitFor(() => expect(confirm).toHaveBeenCalledTimes(1));
            expect(store.isOpen()).toBe(true);
        });

        it('should stay open when the editor keeps editing', async () => {
            const confirm = configureOnScreen(true);
            confirm.mockImplementation((options) => options.accept?.());

            close();

            await vi.waitFor(() => expect(confirm).toHaveBeenCalled());
            expect(store.isOpen()).toBe(true);
        });

        it('should close once the editor chooses to discard', async () => {
            const confirm = configureOnScreen(true);
            confirm.mockImplementation((options) => options.reject?.(ConfirmEventType.REJECT));

            close();

            await vi.waitFor(() => expect(store.isOpen()).toBe(false));
        });

        /** A dismissal is not permission to throw the work away. */
        it('should treat a dismissal as staying open', async () => {
            const confirm = configureOnScreen(true);
            confirm.mockImplementation((options) => options.reject?.(ConfirmEventType.CANCEL));

            close();

            await vi.waitFor(() => expect(confirm).toHaveBeenCalled());
            expect(store.isOpen()).toBe(true);
        });

        it('should close without asking when there is nothing unsaved', async () => {
            const confirm = configureOnScreen(false);

            close();

            await vi.waitFor(() => expect(store.isOpen()).toBe(false));
            expect(confirm).not.toHaveBeenCalled();
        });

        /** Only configuration can hold unsaved work; the list has nothing to lose. */
        it('should close straight away from the list', async () => {
            openOn(() => store.open());

            close();

            await vi.waitFor(() => expect(store.isOpen()).toBe(false));
        });
    });

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
         * FR-008, FR-013. The configuration arrives beside the page rather than instead of it —
         * the reason the panel exists at all. Asserted as the component being rendered, not as the
         * store's view, because the store could say `configure` while the template showed nothing.
         */
        it('should render the configuration when the store opens one', () => {
            openOn(() => store.open());
            openOn(() => store.showConfigure('exp-1'));

            expect(
                spectator.query(DotExperimentsConfigureComponent, { root: true })
            ).not.toBeNull();
            expect(surface('experiments-panel-list')).toBeNull();
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

    /**
     * US7 / FR-012, D6. The panel answers "what is running on this page"; this is the way out to
     * "everything", and it must not cost the editor the page they are standing on.
     *
     * Asserted as a real anchor rather than a click handler calling `window.open`. "Opens in a new
     * tab" is a promise to the editor, not to the code: an anchor honours cmd-click and
     * middle-click, survives a popup blocker, and shows its destination on hover. A handler
     * delivers none of that and would still pass a `window.open` spy.
     */
    describe('the way out to the full portlet', () => {
        const wayOut = () => surface('experiments-panel-portlet-link') as HTMLAnchorElement | null;

        it('should offer a link to the portlet', () => {
            openOn(() => store.open());

            expect(wayOut()).not.toBeNull();
            expect(wayOut()?.getAttribute('aria-label')).toBe(
                'experiments.panel.action.open-portlet'
            );
        });

        /**
         * **Unfiltered**, deliberately. D6 replaced #37005's clearable page filter with this: in a
         * page-scoped panel there is no filter to clear, so carrying `pageId` here would narrow the
         * one destination whose whole point is that it does not narrow.
         */
        it('should target the whole portlet, with no page narrowing', () => {
            openOn(() => store.open());

            expect(wayOut()?.getAttribute('href')).toBe('/dotAdmin/#/experiments');
        });

        it('should open in a new tab, leaving the editor on its page', () => {
            openOn(() => store.open());

            expect(wayOut()?.getAttribute('target')).toBe('_blank');
            // Without it the new tab gets a handle on this one through `window.opener`.
            expect(wayOut()?.getAttribute('rel')).toContain('noopener');
        });

        it('should not close the panel or move the editor when it is followed', () => {
            openOn(() => store.open());

            spectator.click(wayOut() as HTMLElement);

            expect(store.isOpen()).toBe(true);
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
