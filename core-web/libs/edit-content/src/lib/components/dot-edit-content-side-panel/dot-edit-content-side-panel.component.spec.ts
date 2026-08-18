import { createComponentFactory, Spectator, byTestId } from '@openng/spectator/jest';
import { MockComponent, MockPipe } from 'ng-mocks';
import { Subject } from 'rxjs';

import { ButtonModule } from 'primeng/button';
import { Drawer, DrawerModule } from 'primeng/drawer';
import { ZIndexUtils } from 'primeng/utils';

import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotEditContentSidePanelComponent } from './dot-edit-content-side-panel.component';

import { EditContentDialogData } from '../../models/dot-edit-content-dialog.interface';
import { DotSidePanelNavController } from '../../services/dot-side-panel-nav.service';
import { OverlayEditContentHost } from '../../services/host/overlay-edit-content-host';
import { DotEditContentLayoutComponent } from '../dot-edit-content-layout/dot-edit-content.layout.component';

describe('DotEditContentSidePanelComponent', () => {
    let spectator: Spectator<DotEditContentSidePanelComponent>;
    let saved$: Subject<DotCMSContentlet>;
    let mockHost: Pick<OverlayEditContentHost, 'saved$'>;

    const EDIT_DATA: EditContentDialogData = {
        mode: 'edit',
        contentletInode: 'inode-1',
        identifier: 'id-1',
        title: 'My Content'
    };

    const createComponent = createComponentFactory({
        component: DotEditContentSidePanelComponent,
        // Swap the heavy editor for a stub; feed a mock host so we control `saved$`.
        overrideComponents: [
            [
                DotEditContentSidePanelComponent,
                {
                    set: {
                        imports: [
                            DrawerModule,
                            ButtonModule,
                            MockComponent(DotEditContentLayoutComponent),
                            MockPipe(DotMessagePipe, (key: string) => key)
                        ],
                        providers: [{ provide: OverlayEditContentHost, useValue: undefined }]
                    }
                }
            ]
        ]
    });

    beforeEach(() => {
        // Isolate the persisted expanded preference between tests.
        localStorage.clear();
        saved$ = new Subject<DotCMSContentlet>();
        mockHost = { saved$: saved$.asObservable() };

        spectator = createComponent({
            providers: [
                { provide: OverlayEditContentHost, useValue: mockHost },
                // Stub so the component doesn't pull the real controller (and GlobalStore) in tests.
                {
                    provide: DotSidePanelNavController,
                    useValue: {
                        acquire: jest.fn(),
                        release: jest.fn(),
                        isTop: jest.fn().mockReturnValue(true)
                    }
                }
            ],
            detectChanges: false
        });
    });

    it('should create', () => {
        spectator.detectChanges();
        expect(spectator.component).toBeTruthy();
    });

    it('should render the content title in the header', () => {
        spectator.setInput('data', EDIT_DATA);
        spectator.detectChanges();

        // `appendTo="body"` teleports the drawer content out of the fixture into `document.body`,
        // so byTestId (a DOM/CSS query) must search from the document root.
        expect(
            spectator.query(byTestId('side-panel-title'), { root: true })?.textContent?.trim()
        ).toBe('My Content');
    });

    it('should render the editor only when data is set', () => {
        spectator.setInput('data', null);
        spectator.detectChanges();
        expect(spectator.query(DotEditContentLayoutComponent)).toBeNull();

        spectator.setInput('data', EDIT_DATA);
        spectator.detectChanges();
        expect(spectator.query(DotEditContentLayoutComponent)).not.toBeNull();
    });

    /**
     * PrimeNG `p-button` renders its clickable `<button>` inside the host. The `{ root: true }`
     * search is needed because `appendTo="body"` teleports the drawer (and its header buttons)
     * out of the fixture element and into `document.body`.
     */
    const clickButton = (testId: string): void => {
        const button = spectator.query(byTestId(testId), { root: true })?.querySelector('button');
        spectator.click(button as HTMLElement);
    };

    /**
     * Reads the expand button's icon glyph — the user-visible reflection of `$expanded`
     * (`open_in_full` when collapsed, `close_fullscreen` when expanded). Asserting on this instead
     * of the protected signal covers what the user actually sees. `{ root: true }` because
     * `appendTo="body"` teleports the header out of the fixture.
     */
    const expandIcon = (): string | undefined =>
        spectator
            .query(byTestId('side-panel-expand'), { root: true })
            ?.querySelector('i')
            ?.textContent?.trim();

    /**
     * Expand button aria-label (i18n key via MockPipe) — user-facing expanded/collapsed cue.
     *
     * Read from the DESCENDANT native `<button>`, not the `p-button` host: the accessible name has
     * to land on the focusable control. Reading the host would also pass with `[attr.aria-label]`,
     * which leaves the real button unnamed (its only content is an icon glyph).
     */
    const expandAriaLabel = (): string | null | undefined =>
        spectator
            .query(byTestId('side-panel-expand'), { root: true })
            ?.querySelector('button')
            ?.getAttribute('aria-label');

    /**
     * Drawer width from the `pt.root.style` binding (`80%` collapsed / `100%` expanded). The drawer
     * is teleported to `document.body`, so query from the document root.
     */
    const drawerWidth = (): string =>
        (spectator.query('.p-drawer', { root: true }) as HTMLElement | null)?.style?.width ?? '';

    it('should toggle expanded state with the expand button and persist it', () => {
        spectator.setInput('data', EDIT_DATA);
        spectator.detectChanges();

        expect(expandIcon()).toBe('open_in_full');
        expect(expandAriaLabel()).toBe('edit.content.side-panel.expand');
        expect(drawerWidth()).toBe('80%');

        clickButton('side-panel-expand');
        spectator.detectChanges();
        expect(expandIcon()).toBe('close_fullscreen');
        expect(expandAriaLabel()).toBe('edit.content.side-panel.collapse');
        expect(drawerWidth()).toBe('100%');
        expect(localStorage.getItem('dot-edit-content-side-panel-expanded')).toBe('true');

        clickButton('side-panel-expand');
        spectator.detectChanges();
        expect(expandIcon()).toBe('open_in_full');
        expect(expandAriaLabel()).toBe('edit.content.side-panel.expand');
        expect(drawerWidth()).toBe('80%');
        expect(localStorage.getItem('dot-edit-content-side-panel-expanded')).toBe('false');
    });

    it('should route close through the editor guard and emit `closed` when it proceeds', () => {
        spectator.setInput('data', EDIT_DATA);
        spectator.detectChanges();

        const layout = spectator.query(DotEditContentLayoutComponent);
        const confirmClose = jest
            .spyOn(layout, 'confirmClose')
            .mockImplementation((onProceed: () => void) => onProceed());

        const closedSpy = jest.fn();
        spectator.output('closed').subscribe(closedSpy);

        clickButton('side-panel-close');

        expect(confirmClose).toHaveBeenCalledWith(expect.any(Function));
        expect(closedSpy).toHaveBeenCalledTimes(1);
    });

    it('should close through the editor guard on Escape (document keydown)', () => {
        spectator.setInput('data', EDIT_DATA);
        spectator.detectChanges();

        const layout = spectator.query(DotEditContentLayoutComponent);
        const confirmClose = jest
            .spyOn(layout, 'confirmClose')
            .mockImplementation((onProceed: () => void) => onProceed());

        const closedSpy = jest.fn();
        spectator.output('closed').subscribe(closedSpy);

        document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));

        expect(confirmClose).toHaveBeenCalledWith(expect.any(Function));
        expect(closedSpy).toHaveBeenCalledTimes(1);
    });

    it('should ignore Escape while another PrimeNG overlay is stacked above the panel', () => {
        spectator.setInput('data', EDIT_DATA);
        spectator.detectChanges();

        const layout = spectator.query(DotEditContentLayoutComponent);
        const confirmClose = jest.spyOn(layout, 'confirmClose');
        const closedSpy = jest.fn();
        spectator.output('closed').subscribe(closedSpy);

        // Stand in for the image editor dialog / a confirm popup opened from inside the panel:
        // every PrimeNG overlay registers itself in this shared stack when it opens.
        const stackedOverlay = document.createElement('div');
        ZIndexUtils.set('modal', stackedOverlay, 2000);

        document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));

        // That overlay owns the ESC — the panel underneath must not consume it.
        expect(confirmClose).not.toHaveBeenCalled();
        expect(closedSpy).not.toHaveBeenCalled();

        // Shared global stack: leaving the entry behind would leak into every later test.
        ZIndexUtils.clear(stackedOverlay);
    });

    it('should ignore Escape when not the frontmost stacked panel (isTop === false)', () => {
        (spectator.inject(DotSidePanelNavController).isTop as jest.Mock).mockReturnValue(false);
        spectator.setInput('data', EDIT_DATA);
        spectator.detectChanges();

        const layout = spectator.query(DotEditContentLayoutComponent);
        const confirmClose = jest.spyOn(layout, 'confirmClose');
        const closedSpy = jest.fn();
        spectator.output('closed').subscribe(closedSpy);

        document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));

        // A panel beneath the top one must not react to the shared document-level ESC.
        expect(confirmClose).not.toHaveBeenCalled();
        expect(closedSpy).not.toHaveBeenCalled();
    });

    /**
     * Builds a stand-in for a drawer's modal mask and dispatches a bubbling click from it, as the
     * browser would. PrimeNG creates the real mask imperatively during the show animation, which
     * jsdom never runs — hence the stand-in.
     *
     * `ownedByPanel` decides whose mask it is: `true` assigns it to this panel's drawer instance
     * (what the handler compares against), `false` leaves it as a foreign drawer's mask, which must
     * be ignored.
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

    it('should close through the editor guard when clicking outside the panel', () => {
        spectator.setInput('data', EDIT_DATA);
        spectator.detectChanges();

        const layout = spectator.query(DotEditContentLayoutComponent);
        const confirmClose = jest
            .spyOn(layout, 'confirmClose')
            .mockImplementation((onProceed: () => void) => onProceed());

        const closedSpy = jest.fn();
        spectator.output('closed').subscribe(closedSpy);

        clickMask({ ownedByPanel: true });

        // Same contract as ESC / the X button: the guard runs first, close only follows it.
        expect(confirmClose).toHaveBeenCalledWith(expect.any(Function));
        expect(closedSpy).toHaveBeenCalledTimes(1);
    });

    it('should NOT emit `closed` when the guard cancels a click-outside (unsaved changes kept)', () => {
        spectator.setInput('data', EDIT_DATA);
        spectator.detectChanges();

        const layout = spectator.query(DotEditContentLayoutComponent);
        jest.spyOn(layout, 'confirmClose').mockImplementation(() => {
            /* user chose "Keep editing" → never calls onProceed */
        });

        const closedSpy = jest.fn();
        spectator.output('closed').subscribe(closedSpy);

        clickMask({ ownedByPanel: true });

        expect(closedSpy).not.toHaveBeenCalled();
    });

    it('should ignore a click outside when not the frontmost stacked panel (isTop === false)', () => {
        (spectator.inject(DotSidePanelNavController).isTop as jest.Mock).mockReturnValue(false);
        spectator.setInput('data', EDIT_DATA);
        spectator.detectChanges();

        const layout = spectator.query(DotEditContentLayoutComponent);
        const confirmClose = jest.spyOn(layout, 'confirmClose');
        const closedSpy = jest.fn();
        spectator.output('closed').subscribe(closedSpy);

        clickMask({ ownedByPanel: true });

        // A panel beneath the top one must not react to the shared document-level click.
        expect(confirmClose).not.toHaveBeenCalled();
        expect(closedSpy).not.toHaveBeenCalled();
    });

    it('should NOT close on a click inside the panel', () => {
        spectator.setInput('data', EDIT_DATA);
        spectator.detectChanges();

        const layout = spectator.query(DotEditContentLayoutComponent);
        const confirmClose = jest.spyOn(layout, 'confirmClose');
        const closedSpy = jest.fn();
        spectator.output('closed').subscribe(closedSpy);

        // Asserted before dispatching on purpose: with an optional chain, a markup rename would
        // silently skip the click and leave the two negative assertions below passing anyway.
        const inside = spectator.query(byTestId('side-panel-title'), { root: true })!;
        expect(inside).toBeTruthy();

        // Bubbles up to the same document listener, but its target is not the mask.
        inside.dispatchEvent(new MouseEvent('click', { bubbles: true }));

        expect(confirmClose).not.toHaveBeenCalled();
        expect(closedSpy).not.toHaveBeenCalled();
    });

    it('should NOT close on a click on ANOTHER drawer mask', () => {
        spectator.setInput('data', EDIT_DATA);
        spectator.detectChanges();

        const layout = spectator.query(DotEditContentLayoutComponent);
        const confirmClose = jest.spyOn(layout, 'confirmClose');
        const closedSpy = jest.fn();
        spectator.output('closed').subscribe(closedSpy);

        // `p-drawer-mask` is shared by every modal drawer in the app (the UVE block editor sidebar
        // is one, mounted as a sibling of this panel). Matching on the class instead of the mask's
        // identity would let a foreign drawer close this panel and pop the unsaved-changes prompt.
        clickMask({ ownedByPanel: false });

        expect(confirmClose).not.toHaveBeenCalled();
        expect(closedSpy).not.toHaveBeenCalled();
    });

    it('should NOT emit `closed` when the editor guard cancels (unsaved changes kept)', () => {
        spectator.setInput('data', EDIT_DATA);
        spectator.detectChanges();

        const layout = spectator.query(DotEditContentLayoutComponent);
        jest.spyOn(layout, 'confirmClose').mockImplementation(() => {
            /* user chose "Keep editing" → never calls onProceed */
        });

        const closedSpy = jest.fn();
        spectator.output('closed').subscribe(closedSpy);

        clickButton('side-panel-close');

        expect(closedSpy).not.toHaveBeenCalled();
    });

    it('should forward the host `saved$` stream to the `saved` output', async () => {
        spectator.setInput('data', EDIT_DATA);
        spectator.detectChanges();
        // `saved$` is subscribed in afterNextRender — wait for it to run.
        await spectator.fixture.whenStable();

        const savedSpy = jest.fn();
        spectator.output('saved').subscribe(savedSpy);

        const contentlet = { inode: 'inode-1' } as DotCMSContentlet;
        saved$.next(contentlet);

        expect(savedSpy).toHaveBeenCalledWith(contentlet);
    });

    it('should fire data.onContentSaved (last save) and data.onCancel on close', async () => {
        const onContentSaved = jest.fn();
        const onCancel = jest.fn();
        spectator.setInput('data', { ...EDIT_DATA, onContentSaved, onCancel });
        spectator.detectChanges();
        await spectator.fixture.whenStable();

        const contentlet = { inode: 'inode-2' } as DotCMSContentlet;
        saved$.next(contentlet);

        const layout = spectator.query(DotEditContentLayoutComponent);
        jest.spyOn(layout, 'confirmClose').mockImplementation((onProceed: () => void) =>
            onProceed()
        );

        clickButton('side-panel-close');

        expect(onContentSaved).toHaveBeenCalledWith(contentlet);
        expect(onCancel).toHaveBeenCalledTimes(1);
    });

    it('should not fire data.onContentSaved on close when nothing was saved', () => {
        const onContentSaved = jest.fn();
        const onCancel = jest.fn();
        spectator.setInput('data', { ...EDIT_DATA, onContentSaved, onCancel });
        spectator.detectChanges();

        const layout = spectator.query(DotEditContentLayoutComponent);
        jest.spyOn(layout, 'confirmClose').mockImplementation((onProceed: () => void) =>
            onProceed()
        );

        clickButton('side-panel-close');

        expect(onContentSaved).not.toHaveBeenCalled();
        expect(onCancel).toHaveBeenCalledTimes(1);
    });
});

/**
 * Construction-time seeding of `$expanded` from the persisted preference. Kept in its own describe
 * with a dedicated factory because it needs a component built AFTER localStorage is seeded — the
 * main describe's shared beforeEach constructs the panel with storage already cleared.
 */
describe('DotEditContentSidePanelComponent — persisted expanded preference', () => {
    let spectator: Spectator<DotEditContentSidePanelComponent>;

    const EDIT_DATA: EditContentDialogData = {
        mode: 'edit',
        contentletInode: 'inode-1',
        identifier: 'id-1',
        title: 'My Content'
    };

    const createComponent = createComponentFactory({
        component: DotEditContentSidePanelComponent,
        overrideComponents: [
            [
                DotEditContentSidePanelComponent,
                {
                    set: {
                        imports: [
                            DrawerModule,
                            ButtonModule,
                            MockComponent(DotEditContentLayoutComponent),
                            MockPipe(DotMessagePipe, (key: string) => key)
                        ],
                        providers: [{ provide: OverlayEditContentHost, useValue: undefined }]
                    }
                }
            ]
        ]
    });

    afterEach(() => {
        localStorage.clear();
    });

    const buildAndAssertOpenState = (expected: {
        icon: string;
        ariaLabel: string;
        width: string;
    }) => {
        spectator = createComponent({
            providers: [
                {
                    provide: OverlayEditContentHost,
                    useValue: { saved$: new Subject<DotCMSContentlet>().asObservable() }
                },
                {
                    provide: DotSidePanelNavController,
                    useValue: {
                        acquire: jest.fn(),
                        release: jest.fn(),
                        isTop: jest.fn().mockReturnValue(true)
                    }
                }
            ],
            detectChanges: false
        });
        spectator.setInput('data', EDIT_DATA);
        spectator.detectChanges();

        expect(
            spectator
                .query(byTestId('side-panel-expand'), { root: true })
                ?.querySelector('i')
                ?.textContent?.trim()
        ).toBe(expected.icon);
        // Descendant native <button>, not the p-button host — see `expandAriaLabel` above.
        expect(
            spectator
                .query(byTestId('side-panel-expand'), { root: true })
                ?.querySelector('button')
                ?.getAttribute('aria-label')
        ).toBe(expected.ariaLabel);
        // Width is the user-visible outcome of reading `$expanded` on construction (`pt.root.style`).
        expect(
            (spectator.query('.p-drawer', { root: true }) as HTMLElement | null)?.style?.width
        ).toBe(expected.width);
    };

    it('opens expanded when the persisted preference is `true`', () => {
        localStorage.setItem('dot-edit-content-side-panel-expanded', 'true');
        buildAndAssertOpenState({
            icon: 'close_fullscreen',
            ariaLabel: 'edit.content.side-panel.collapse',
            width: '100%'
        });
    });

    it('opens collapsed when there is no persisted preference', () => {
        localStorage.removeItem('dot-edit-content-side-panel-expanded');
        buildAndAssertOpenState({
            icon: 'open_in_full',
            ariaLabel: 'edit.content.side-panel.expand',
            width: '80%'
        });
    });
});
