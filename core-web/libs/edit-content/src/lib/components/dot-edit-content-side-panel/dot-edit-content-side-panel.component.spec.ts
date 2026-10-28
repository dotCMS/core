import { createComponentFactory, mockProvider, Spectator } from '@openng/spectator/vitest';
import { MockComponent } from 'ng-mocks';
import { Subject } from 'rxjs';
import { Mock, vi } from 'vitest';

import { signal } from '@angular/core';

import { DialogService, DynamicDialogConfig } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotSidePanelRef } from '@dotcms/ui';
import { createFakeContentlet } from '@dotcms/utils-testing';

import { DotEditContentSidePanelComponent } from './dot-edit-content-side-panel.component';

import {
    AngularImageEditorLauncher,
    IMAGE_EDITOR_LAUNCHER
} from '../../fields/shared/image-editor-launcher';
import { EditContentDialogData } from '../../models/dot-edit-content-dialog.interface';
import { DotEditContentRequest } from '../../models/dot-edit-content-request.model';
import { OverlayEditContentHost } from '../../services/host/overlay-edit-content-host';
import { DotEditContentLayoutComponent } from '../dot-edit-content-layout/dot-edit-content.layout.component';

/** The panel's ref, reduced to what the content uses. */
const panelRef = () => ({
    close: vi.fn(),
    contentStyleClass: signal('p-0!'),
    closed: new Subject<void>()
});

describe('DotEditContentSidePanelComponent', () => {
    let spectator: Spectator<DotEditContentSidePanelComponent>;
    let saved$: Subject<DotCMSContentlet>;
    let left$: Subject<void>;
    let languageChanged$: Subject<number>;
    let panel: { close: Mock; closed: Subject<void> };

    const EDIT_DATA: EditContentDialogData = {
        mode: 'edit',
        contentletInode: 'inode-1',
        identifier: 'id-1',
        title: 'My Content'
    };

    const EDIT_REQUEST: DotEditContentRequest = {
        mode: 'edit',
        inode: 'inode-1',
        identifier: 'id-1',
        contentType: 'Blog',
        title: 'My Content',
        variantName: 'variant-a'
    };

    const createComponent = createComponentFactory({
        component: DotEditContentSidePanelComponent,
        // Swap the heavy editor for a stub; feed a mock host so we control `saved$`.
        overrideComponents: [
            [
                DotEditContentSidePanelComponent,
                {
                    set: {
                        imports: [MockComponent(DotEditContentLayoutComponent)],
                        providers: [{ provide: OverlayEditContentHost, useValue: undefined }]
                    }
                }
            ]
        ],
        providers: [mockProvider(DotMessageService, { get: (key: string) => key })]
    });

    beforeEach(() => {
        saved$ = new Subject<DotCMSContentlet>();
        left$ = new Subject<void>();
        languageChanged$ = new Subject<number>();
        panel = panelRef();

        spectator = createComponent({
            providers: [
                {
                    provide: OverlayEditContentHost,
                    useValue: {
                        saved$: saved$.asObservable(),
                        left$: left$.asObservable(),
                        languageChanged$: languageChanged$.asObservable()
                    }
                },
                { provide: DotSidePanelRef, useValue: panel }
            ],
            detectChanges: false
        });
    });

    /** Opens the panel on `data` and lets the host subscriptions (afterNextRender) run. */
    const open = async (data: EditContentDialogData = EDIT_DATA) => {
        spectator.setInput('data', data);
        spectator.detectChanges();
        await spectator.fixture.whenStable();
    };

    /** Opens the panel on a request (the new contract) and lets the host subscriptions run. */
    const openRequest = async (request: DotEditContentRequest = EDIT_REQUEST) => {
        spectator.setInput('request', request);
        spectator.detectChanges();
        await spectator.fixture.whenStable();
    };

    /** Makes the editor's unsaved-changes guard answer as the author would. */
    const guardAnswers = (answer: 'proceed' | 'keep') =>
        vi
            .spyOn(spectator.query(DotEditContentLayoutComponent)!, 'confirmClose')
            .mockImplementation((onProceed: () => void) => {
                if (answer === 'proceed') {
                    onProceed();
                }
            });

    it('should render the editor only when data is set', () => {
        spectator.setInput('data', null);
        spectator.detectChanges();
        expect(spectator.query(DotEditContentLayoutComponent)).toBeNull();

        spectator.setInput('data', EDIT_DATA);
        spectator.detectChanges();
        expect(spectator.query(DotEditContentLayoutComponent)).not.toBeNull();
    });

    it('should route a close request through the editor guard and close the panel when it proceeds', async () => {
        await open();
        const confirmClose = guardAnswers('proceed');

        spectator.component.requestClose();

        expect(confirmClose).toHaveBeenCalledWith(expect.any(Function));
        expect(panel.close).toHaveBeenCalledTimes(1);
    });

    it('should keep the panel open when the editor guard cancels (unsaved changes kept)', async () => {
        await open();
        guardAnswers('keep');

        spectator.component.requestClose();

        expect(panel.close).not.toHaveBeenCalled();
    });

    it('should forward the host `saved$` stream to the `saved` output', async () => {
        await open();
        const savedSpy = vi.fn();
        spectator.output('saved').subscribe(savedSpy);

        const contentlet = { inode: 'inode-1' } as DotCMSContentlet;
        saved$.next(contentlet);

        expect(savedSpy).toHaveBeenCalledWith(contentlet);
    });

    it('should forward the host language switch to the `languageChanged` output', async () => {
        await open();
        const languageSpy = vi.fn();
        spectator.output('languageChanged').subscribe(languageSpy);

        languageChanged$.next(2);

        expect(languageSpy).toHaveBeenCalledWith(2);
    });

    it('should fire data.onContentSaved (last save) and data.onCancel on close', async () => {
        const onContentSaved = vi.fn();
        const onCancel = vi.fn();
        await open({ ...EDIT_DATA, onContentSaved, onCancel });

        const contentlet = { inode: 'inode-2' } as DotCMSContentlet;
        saved$.next(contentlet);
        guardAnswers('proceed');

        spectator.component.requestClose();

        expect(onContentSaved).toHaveBeenCalledWith(contentlet);
        expect(onCancel).toHaveBeenCalledTimes(1);
    });

    it('should not fire data.onContentSaved on close when nothing was saved', async () => {
        const onContentSaved = vi.fn();
        const onCancel = vi.fn();
        await open({ ...EDIT_DATA, onContentSaved, onCancel });
        guardAnswers('proceed');

        spectator.component.requestClose();

        expect(onContentSaved).not.toHaveBeenCalled();
        expect(onCancel).toHaveBeenCalledTimes(1);
    });

    it('should close without the guard or a save report when the host says the content was deleted', async () => {
        const onContentSaved = vi.fn();
        const onCancel = vi.fn();
        await open({ ...EDIT_DATA, onContentSaved, onCancel });

        // An earlier save of the same content must not reach the opener once it is deleted.
        saved$.next({ inode: 'inode-2' } as DotCMSContentlet);
        const confirmClose = vi.spyOn(
            spectator.query(DotEditContentLayoutComponent)!,
            'confirmClose'
        );

        left$.next();

        expect(panel.close).toHaveBeenCalledTimes(1);
        expect(confirmClose).not.toHaveBeenCalled();
        expect(onContentSaved).not.toHaveBeenCalled();
        expect(onCancel).toHaveBeenCalledTimes(1);
    });

    /**
     * The one-request contract (#38303). The panel takes a `DotEditContentRequest`, reports a
     * delete and a close as events, and hands the overlay host an identity built from the request.
     */
    describe('with a request', () => {
        it('renders the editor once the request is set', async () => {
            spectator.setInput('request', null);
            spectator.detectChanges();
            expect(spectator.query(DotEditContentLayoutComponent)).toBeNull();

            await openRequest();

            expect(spectator.query(DotEditContentLayoutComponent)).not.toBeNull();
        });

        it('gives the overlay host the identity the request names, variant included', async () => {
            await openRequest();

            const config = spectator.inject(DynamicDialogConfig, true);

            expect(config.data).toEqual(
                expect.objectContaining({
                    contentletInode: 'inode-1',
                    contentTypeId: 'Blog',
                    languageId: undefined,
                    variantName: 'variant-a'
                })
            );
        });

        it('maps a create request to the content type the editor starts from', async () => {
            await openRequest({
                mode: 'new',
                contentType: 'Blog',
                folderPath: '/news/',
                languageId: 2
            });

            const config = spectator.inject(DynamicDialogConfig, true);

            expect(config.data).toEqual(
                expect.objectContaining({
                    contentletInode: undefined,
                    contentTypeId: 'Blog',
                    folderPath: '/news/',
                    languageId: 2
                })
            );
        });

        it('reports deleted and then closes, without the guard, when the host says the content left', async () => {
            await openRequest();
            const deleted = vi.fn();
            const saved = vi.fn();
            spectator.output('deleted').subscribe(deleted);
            spectator.output('saved').subscribe(saved);
            const confirmClose = vi.spyOn(
                spectator.query(DotEditContentLayoutComponent)!,
                'confirmClose'
            );

            left$.next();

            expect(deleted).toHaveBeenCalledTimes(1);
            expect(saved).not.toHaveBeenCalled();
            expect(confirmClose).not.toHaveBeenCalled();
            expect(panel.close).toHaveBeenCalledTimes(1);
        });

        it('reports closed once when the panel closes, for any reason', async () => {
            await openRequest();
            const closed = vi.fn();
            spectator.output('closed').subscribe(closed);

            panel.closed.next();
            panel.closed.complete();

            expect(closed).toHaveBeenCalledTimes(1);
        });

        it('reports every save, so the opener refreshes while the panel stays open', async () => {
            await openRequest();
            const saved = vi.fn();
            spectator.output('saved').subscribe(saved);

            saved$.next(createFakeContentlet({ inode: 'inode-2' }));
            saved$.next(createFakeContentlet({ inode: 'inode-3' }));

            expect(saved).toHaveBeenCalledTimes(2);
        });

        it('keeps the panel open after a save', async () => {
            await openRequest();

            saved$.next(createFakeContentlet({ inode: 'inode-2' }));

            expect(panel.close).not.toHaveBeenCalled();
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
                { provide: DotSidePanelRef, useValue: panelRef() }
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
