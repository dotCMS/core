import {
    createComponentFactory,
    mockProvider,
    Spectator,
    byTestId
} from '@openng/spectator/vitest';
import { MockComponent } from 'ng-mocks';
import { Subject } from 'rxjs';
import { Mock, vi } from 'vitest';

import { Drawer } from 'primeng/drawer';
import { DialogService } from 'primeng/dynamicdialog';
import { ZIndexUtils } from 'primeng/utils';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { DotEditContentSidePanelComponent } from './dot-edit-content-side-panel.component';

import {
    AngularImageEditorLauncher,
    IMAGE_EDITOR_LAUNCHER
} from '../../fields/shared/image-editor-launcher';
import { EditContentDialogData } from '../../models/dot-edit-content-dialog.interface';
import { DotSidePanelNavController } from '../../services/dot-side-panel-nav.service';
import { OverlayEditContentHost } from '../../services/host/overlay-edit-content-host';
import { DotEditContentLayoutComponent } from '../dot-edit-content-layout/dot-edit-content.layout.component';
import { DotSidePanelComponent } from '../dot-side-panel/dot-side-panel.component';

describe('DotEditContentSidePanelComponent', () => {
    let spectator: Spectator<DotEditContentSidePanelComponent>;
    let saved$: Subject<DotCMSContentlet>;
    let left$: Subject<void>;
    let languageChanged$: Subject<number>;
    let mockHost: Pick<OverlayEditContentHost, 'saved$' | 'left$' | 'languageChanged$'>;

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
                            DotSidePanelComponent,
                            MockComponent(DotEditContentLayoutComponent)
                        ],
                        providers: [{ provide: OverlayEditContentHost, useValue: undefined }]
                    }
                }
            ]
        ],
        providers: [mockProvider(DotMessageService, { get: (key: string) => key })]
    });

    beforeEach(() => {
        // Isolate the persisted expanded preference between tests.
        localStorage.clear();
        saved$ = new Subject<DotCMSContentlet>();
        left$ = new Subject<void>();
        languageChanged$ = new Subject<number>();
        mockHost = {
            saved$: saved$.asObservable(),
            left$: left$.asObservable(),
            languageChanged$: languageChanged$.asObservable()
        };

        spectator = createComponent({
            providers: [
                { provide: OverlayEditContentHost, useValue: mockHost },
                // Stub so the component doesn't pull the real controller (and GlobalStore) in tests.
                {
                    provide: DotSidePanelNavController,
                    useValue: {
                        acquire: vi.fn(),
                        release: vi.fn(),
                        isTop: vi.fn().mockReturnValue(true)
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

    // The width toggle and its stored preference belong to `DotSidePanelComponent`, and are tested
    // there. What this panel adds is its guard on every way of closing.

    it('should route close through the editor guard and emit `closed` when it proceeds', () => {
        spectator.setInput('data', EDIT_DATA);
        spectator.detectChanges();

        const layout = spectator.query(DotEditContentLayoutComponent);
        const confirmClose = vi
            .spyOn(layout, 'confirmClose')
            .mockImplementation((onProceed: () => void) => onProceed());

        const closedSpy = vi.fn();
        spectator.output('closed').subscribe(closedSpy);

        clickButton('side-panel-close');

        expect(confirmClose).toHaveBeenCalledWith(expect.any(Function));
        expect(closedSpy).toHaveBeenCalledTimes(1);
    });

    it('should close through the editor guard on Escape (document keydown)', () => {
        spectator.setInput('data', EDIT_DATA);
        spectator.detectChanges();

        const layout = spectator.query(DotEditContentLayoutComponent);
        const confirmClose = vi
            .spyOn(layout, 'confirmClose')
            .mockImplementation((onProceed: () => void) => onProceed());

        const closedSpy = vi.fn();
        spectator.output('closed').subscribe(closedSpy);

        document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));

        expect(confirmClose).toHaveBeenCalledWith(expect.any(Function));
        expect(closedSpy).toHaveBeenCalledTimes(1);
    });

    it('should ignore Escape while another PrimeNG overlay is stacked above the panel', () => {
        spectator.setInput('data', EDIT_DATA);
        spectator.detectChanges();

        const layout = spectator.query(DotEditContentLayoutComponent);
        const confirmClose = vi.spyOn(layout!, 'confirmClose');
        const closedSpy = vi.fn();
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
        (spectator.inject(DotSidePanelNavController).isTop as Mock).mockReturnValue(false);
        spectator.setInput('data', EDIT_DATA);
        spectator.detectChanges();

        const layout = spectator.query(DotEditContentLayoutComponent);
        const confirmClose = vi.spyOn(layout!, 'confirmClose');
        const closedSpy = vi.fn();
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
        const confirmClose = vi
            .spyOn(layout, 'confirmClose')
            .mockImplementation((onProceed: () => void) => onProceed());

        const closedSpy = vi.fn();
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
        vi.spyOn(layout!, 'confirmClose').mockImplementation(() => {
            /* user chose "Keep editing" → never calls onProceed */
        });

        const closedSpy = vi.fn();
        spectator.output('closed').subscribe(closedSpy);

        clickMask({ ownedByPanel: true });

        expect(closedSpy).not.toHaveBeenCalled();
    });

    it('should ignore a click outside when not the frontmost stacked panel (isTop === false)', () => {
        (spectator.inject(DotSidePanelNavController).isTop as Mock).mockReturnValue(false);
        spectator.setInput('data', EDIT_DATA);
        spectator.detectChanges();

        const layout = spectator.query(DotEditContentLayoutComponent);
        const confirmClose = vi.spyOn(layout!, 'confirmClose');
        const closedSpy = vi.fn();
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
        const confirmClose = vi.spyOn(layout!, 'confirmClose');
        const closedSpy = vi.fn();
        spectator.output('closed').subscribe(closedSpy);

        // Asserted before dispatching on purpose: with an optional chain, a markup rename would
        // silently skip the click and leave the two negative assertions below passing anyway.
        const inside = spectator.query(byTestId('side-panel-title'), { root: true });
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
        const confirmClose = vi.spyOn(layout!, 'confirmClose');
        const closedSpy = vi.fn();
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
        vi.spyOn(layout!, 'confirmClose').mockImplementation(() => {
            /* user chose "Keep editing" → never calls onProceed */
        });

        const closedSpy = vi.fn();
        spectator.output('closed').subscribe(closedSpy);

        clickButton('side-panel-close');

        expect(closedSpy).not.toHaveBeenCalled();
    });

    it('should forward the host `saved$` stream to the `saved` output', async () => {
        spectator.setInput('data', EDIT_DATA);
        spectator.detectChanges();
        // `saved$` is subscribed in afterNextRender — wait for it to run.
        await spectator.fixture.whenStable();

        const savedSpy = vi.fn();
        spectator.output('saved').subscribe(savedSpy);

        const contentlet = { inode: 'inode-1' } as DotCMSContentlet;
        saved$.next(contentlet);

        expect(savedSpy).toHaveBeenCalledWith(contentlet);
    });

    it('should fire data.onContentSaved (last save) and data.onCancel on close', async () => {
        const onContentSaved = vi.fn();
        const onCancel = vi.fn();
        spectator.setInput('data', { ...EDIT_DATA, onContentSaved, onCancel });
        spectator.detectChanges();
        await spectator.fixture.whenStable();

        const contentlet = { inode: 'inode-2' } as DotCMSContentlet;
        saved$.next(contentlet);

        const layout = spectator.query(DotEditContentLayoutComponent);
        vi.spyOn(layout!, 'confirmClose').mockImplementation((onProceed: () => void) =>
            onProceed()
        );

        clickButton('side-panel-close');

        expect(onContentSaved).toHaveBeenCalledWith(contentlet);
        expect(onCancel).toHaveBeenCalledTimes(1);
    });

    it('should close without the guard or a save report when the host says the content was deleted', async () => {
        const onContentSaved = vi.fn();
        const onCancel = vi.fn();
        spectator.setInput('data', { ...EDIT_DATA, onContentSaved, onCancel });
        spectator.detectChanges();
        await spectator.fixture.whenStable();

        // An earlier save of the same content must not reach the opener once it is deleted.
        saved$.next({ inode: 'inode-2' } as DotCMSContentlet);

        const layout = spectator.query(DotEditContentLayoutComponent);
        const confirmClose = vi.spyOn(layout!, 'confirmClose');
        const closedSpy = vi.fn();
        spectator.output('closed').subscribe(closedSpy);

        left$.next();

        expect(closedSpy).toHaveBeenCalledTimes(1);
        expect(confirmClose).not.toHaveBeenCalled();
        expect(onContentSaved).not.toHaveBeenCalled();
        expect(onCancel).toHaveBeenCalledTimes(1);
    });

    it('should not fire data.onContentSaved on close when nothing was saved', () => {
        const onContentSaved = vi.fn();
        const onCancel = vi.fn();
        spectator.setInput('data', { ...EDIT_DATA, onContentSaved, onCancel });
        spectator.detectChanges();

        const layout = spectator.query(DotEditContentLayoutComponent);
        vi.spyOn(layout!, 'confirmClose').mockImplementation((onProceed: () => void) =>
            onProceed()
        );

        clickButton('side-panel-close');

        expect(onContentSaved).not.toHaveBeenCalled();
        expect(onCancel).toHaveBeenCalledTimes(1);
    });

    // Migrated from a document-level host binding to the shared shortcut registry (issue #32591), so
    // the panel and the portlet behind it can arbitrate instead of both acting on the same key.
    describe('ESC through the shortcut registry', () => {
        const pressEscape = (): KeyboardEvent => {
            const event = new KeyboardEvent('keydown', {
                key: 'Escape',
                bubbles: true,
                cancelable: true
            });
            document.dispatchEvent(event);

            return event;
        };

        // An open panel always has data, and the drawer whose z-index the overlay guard compares
        // against only renders once it does.
        beforeEach(() => {
            spectator.setInput('data', EDIT_DATA);
            spectator.detectChanges();
        });

        // ESC closes through the editor's unsaved-changes guard, never around it. The stubbed layout
        // is told to proceed so the whole path runs; that the guard is consulted at all is the point.
        it('should close the panel through the unsaved-changes guard', () => {
            const layout = spectator.query(DotEditContentLayoutComponent);
            const confirmClose = vi
                .spyOn(layout!, 'confirmClose')
                .mockImplementation((proceed: () => void) => proceed());
            const closed = vi.fn();
            spectator.output('closed').subscribe(closed);

            pressEscape();

            expect(confirmClose).toHaveBeenCalled();
            expect(closed).toHaveBeenCalled();
        });

        it('should consume the key so it cannot reach the portlet behind', () => {
            const event = pressEscape();

            expect(event.defaultPrevented).toBe(true);
        });

        it('should not close while another overlay is stacked above', () => {
            vi.spyOn(ZIndexUtils, 'getCurrent').mockReturnValue(Number.MAX_SAFE_INTEGER);
            const closed = vi.fn();
            spectator.output('closed').subscribe(closed);

            pressEscape();

            expect(closed).not.toHaveBeenCalled();
        });

        it('should still consume the key while an overlay is above, so the portlet stays untouched', () => {
            vi.spyOn(ZIndexUtils, 'getCurrent').mockReturnValue(Number.MAX_SAFE_INTEGER);

            const event = pressEscape();

            expect(event.defaultPrevented).toBe(true);
        });

        it('should release the claim when the panel is destroyed', () => {
            spectator.fixture.destroy();

            const event = pressEscape();

            expect(event.defaultPrevented).toBe(false);
        });
    });
});

/**
 * Regression lock for #37398, as its own top-level suite on purpose.
 *
 * The suite above overrides this component with `set: { providers: [...] }`, which REPLACES the
 * component's own `providers` — so `IMAGE_EDITOR_LAUNCHER` would be absent there for a reason that
 * has nothing to do with the component. This factory overrides only `imports` (stubbing the heavy
 * layout), leaving the real provider list intact, so the assertion is about the component rather
 * than about the test setup.
 *
 * `DotFileFieldComponent` injects the token as `{ optional: true }`, so a host losing this provider
 * does not fail loudly — Image/File fields quietly open the legacy Dojo editor instead of the new
 * one. This turns that into a test failure rather than a user-visible downgrade.
 */
describe('DotEditContentSidePanelComponent — image editor host capability', () => {
    let spectator: Spectator<DotEditContentSidePanelComponent>;

    const createComponent = createComponentFactory({
        component: DotEditContentSidePanelComponent,
        overrideComponents: [
            [
                DotEditContentSidePanelComponent,
                {
                    remove: { imports: [DotEditContentLayoutComponent] },
                    add: { imports: [MockComponent(DotEditContentLayoutComponent)] }
                }
            ]
        ],
        providers: [mockProvider(DotMessageService, { get: (key: string) => key })]
    });

    beforeEach(() => {
        spectator = createComponent({
            providers: [
                {
                    provide: OverlayEditContentHost,
                    useValue: { saved$: new Subject(), left$: new Subject<void>() }
                },
                // Same stub the suites above use: keep the real controller — and the GlobalStore
                // and HTTP services behind it — out of a test that is only about providers.
                {
                    provide: DotSidePanelNavController,
                    useValue: {
                        acquire: vi.fn(),
                        release: vi.fn(),
                        isTop: vi.fn().mockReturnValue(true)
                    }
                }
            ],
            detectChanges: false
        });
    });

    it('should provide IMAGE_EDITOR_LAUNCHER so fields use the new editor, not legacy Dojo', () => {
        const launcher = spectator.inject(IMAGE_EDITOR_LAUNCHER, true);

        expect(launcher).toBeDefined();
        expect(launcher).toBeInstanceOf(AngularImageEditorLauncher);
    });

    it('should provide the DialogService the launcher opens the editor through', () => {
        expect(spectator.inject(DialogService, true)).toBeTruthy();
    });
});
