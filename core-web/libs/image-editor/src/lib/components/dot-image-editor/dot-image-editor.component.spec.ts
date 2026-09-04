import { Dispatcher } from '@ngrx/signals/events';
import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@openng/spectator/jest';
import { MockComponent } from 'ng-mocks';
import { of } from 'rxjs';

import { signal } from '@angular/core';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { Confirmation, ConfirmationService, ConfirmEventType, MessageService } from 'primeng/api';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService, DotPropertiesService } from '@dotcms/data-access';
import { DotCMSTempFile } from '@dotcms/dotcms-models';

import { DotImageEditorComponent } from './dot-image-editor.component';

import { AssetMeta, ImageEditorOpenParams } from '../../models/image-editor.models';
import { DotImageEditorService } from '../../services/dot-image-editor.service';
import {
    imageEditorHistoryEvents,
    imageEditorLifecycleEvents
} from '../../store/image-editor.events';
import { ImageEditorStore } from '../../store/image-editor.store';
import { DotImageEditorCanvasComponent } from '../dot-image-editor-canvas/dot-image-editor-canvas.component';
import { DotImageEditorFooterComponent } from '../dot-image-editor-footer/dot-image-editor-footer.component';
import { DotImageEditorFullscreenToggleComponent } from '../dot-image-editor-fullscreen-toggle/dot-image-editor-fullscreen-toggle.component';
import { DotImageEditorPanelsComponent } from '../dot-image-editor-panels/dot-image-editor-panels.component';

/** Builds the suite for a given set of open params, asserting the shared shell behavior. */
function describeWith(label: string, data: ImageEditorOpenParams): void {
    describe(`DotImageEditorComponent (${label})`, () => {
        let spectator: Spectator<DotImageEditorComponent>;
        let dispatcher: SpyObject<Dispatcher>;
        let dialogRef: SpyObject<DynamicDialogRef>;
        let confirmationService: SpyObject<ConfirmationService>;

        const isDirty = signal(false);
        const canUndo = signal(false);
        const canRedo = signal(false);
        const isFullscreen = signal(false);
        const saveStatus = signal<'idle' | 'saving' | 'error'>('idle');
        const saveError = signal<string | null>(null);

        const createComponent = createComponentFactory({
            component: DotImageEditorComponent,
            providers: [
                provideNoopAnimations(),
                Dispatcher,
                mockProvider(DotMessageService, { get: jest.fn((key: string) => key) }),
                mockProvider(DynamicDialogRef, { close: jest.fn() }),
                { provide: DynamicDialogConfig, useValue: { data } }
            ],
            // `componentProviders` overrides the component's own `providers`, so
            // re-supply the real ConfirmationService (the template's
            // `<p-confirmDialog />` subscribes to it; we spy on `confirm`) and the
            // MessageService backing the template's `<dot-toast />` — while mocking
            // only the store. Host-independence of that provider is covered by the
            // un-mocked suite at the bottom of this file, not here.
            componentProviders: [
                ConfirmationService,
                MessageService,
                mockProvider(ImageEditorStore, {
                    isDirty,
                    canUndo,
                    canRedo,
                    isFullscreen,
                    saveStatus,
                    saveError
                })
            ],
            // Isolate the editor from the children's own store/dispatch wiring. The shared
            // `DotDialog*` shell is deliberately left real: the close button lives in it, and
            // stubbing it would drop everything projected into its slots.
            overrideComponents: [
                [
                    DotImageEditorComponent,
                    {
                        remove: {
                            imports: [
                                DotImageEditorFullscreenToggleComponent,
                                DotImageEditorCanvasComponent,
                                DotImageEditorPanelsComponent,
                                DotImageEditorFooterComponent
                            ]
                        },
                        add: {
                            imports: [
                                MockComponent(DotImageEditorFullscreenToggleComponent),
                                MockComponent(DotImageEditorCanvasComponent),
                                MockComponent(DotImageEditorPanelsComponent),
                                MockComponent(DotImageEditorFooterComponent)
                            ]
                        }
                    }
                ]
            ]
        });

        /**
         * The ✕ now belongs to the shared shell rather than to an editor-owned header component,
         * so the close path is driven through the rendered button instead of an output.
         */
        const clickHeaderClose = () => {
            const button = spectator.query(byTestId('dialog-close-btn'))?.querySelector('button');
            spectator.click(button as HTMLElement);
        };

        beforeEach(() => {
            // The DynamicDialogRef `close` mock is shared across tests in this
            // describe; clear it so prior tests' close() calls don't leak into the
            // "not closed" assertions.
            jest.clearAllMocks();
            isDirty.set(false);
            canUndo.set(false);
            canRedo.set(false);
            isFullscreen.set(false);
            saveStatus.set('idle');
            saveError.set(null);

            // Spy before creation so the constructor's assetRequested dispatch is
            // captured (injectDispatch dispatches through Dispatcher.prototype).
            jest.spyOn(Dispatcher.prototype, 'dispatch');

            spectator = createComponent();
            dispatcher = spectator.inject(Dispatcher, true);
            dialogRef = spectator.inject(DynamicDialogRef, true);
            // Resolve the component-scoped instance and spy on its confirm method.
            confirmationService = spectator.inject(ConfirmationService, true);
            jest.spyOn(confirmationService, 'confirm');
        });

        afterEach(() => {
            // Restore the `Dispatcher.prototype.dispatch` and `confirm` spies (clearAllMocks
            // only resets call records, not the spied methods) so a prototype-level spy
            // never leaks across these suites.
            jest.restoreAllMocks();
        });

        it('should render the root and the four child components', () => {
            expect(spectator.query(byTestId('image-editor-root'))).toExist();
            expect(spectator.query('dot-image-editor-fullscreen-toggle')).toExist();
            expect(spectator.query('dot-image-editor-canvas')).toExist();
            expect(spectator.query('dot-image-editor-panels')).toExist();
            expect(spectator.query('dot-image-editor-footer')).toExist();
        });

        it('should dispatch assetRequested with the dialog data on init', () => {
            expect(dispatcher.dispatch).toHaveBeenCalledWith(
                imageEditorLifecycleEvents.assetRequested(data),
                { scope: 'self' }
            );
        });

        it('should close with null when the header close is triggered and not dirty', () => {
            clickHeaderClose();

            expect(dialogRef.close).toHaveBeenCalledWith(null);
            expect(confirmationService.confirm).not.toHaveBeenCalled();
        });

        it('should close with null when the footer cancel is triggered and not dirty', () => {
            const footer = spectator.query(DotImageEditorFooterComponent)!;
            footer.$cancel.emit();

            expect(dialogRef.close).toHaveBeenCalledWith(null);
            expect(confirmationService.confirm).not.toHaveBeenCalled();
        });

        it('should confirm before closing when there are unsaved edits', () => {
            isDirty.set(true);

            clickHeaderClose();

            expect(confirmationService.confirm).toHaveBeenCalledTimes(1);
            expect(dialogRef.close).not.toHaveBeenCalled();
        });

        it('should close with null when the secondary "Discard" button is clicked (reject/REJECT)', () => {
            isDirty.set(true);

            spectator.query(DotImageEditorFooterComponent)!.$cancel.emit();

            const confirmation = (confirmationService.confirm as jest.Mock).mock
                .calls[0][0] as Confirmation;
            confirmation.reject?.(ConfirmEventType.REJECT);

            expect(dialogRef.close).toHaveBeenCalledWith(null);
        });

        it('should NOT close when the primary "Keep editing" button is clicked (accept)', () => {
            isDirty.set(true);

            spectator.query(DotImageEditorFooterComponent)!.$cancel.emit();

            const confirmation = (confirmationService.confirm as jest.Mock).mock
                .calls[0][0] as Confirmation;
            confirmation.accept?.();

            expect(dialogRef.close).not.toHaveBeenCalled();
        });

        it('should NOT close when the prompt is dismissed via X/ESC (reject/CANCEL)', () => {
            isDirty.set(true);

            spectator.query(DotImageEditorFooterComponent)!.$cancel.emit();

            const confirmation = (confirmationService.confirm as jest.Mock).mock
                .calls[0][0] as Confirmation;
            confirmation.reject?.(ConfirmEventType.CANCEL);

            expect(dialogRef.close).not.toHaveBeenCalled();
        });

        it('should close with null on Escape when there are no unsaved edits', () => {
            document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));

            expect(dialogRef.close).toHaveBeenCalledWith(null);
            expect(confirmationService.confirm).not.toHaveBeenCalled();
        });

        it('should confirm (not close) on Escape when there are unsaved edits', () => {
            isDirty.set(true);

            document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));

            expect(confirmationService.confirm).toHaveBeenCalledTimes(1);
            expect(dialogRef.close).not.toHaveBeenCalled();
        });

        it('should not re-open the discard prompt on a repeated Escape', () => {
            isDirty.set(true);

            document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
            document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));

            expect(confirmationService.confirm).toHaveBeenCalledTimes(1);
        });

        describe('save lifecycle', () => {
            const tempFile: DotCMSTempFile = {
                fileName: 'edited.png',
                folder: 'shared',
                id: 'temp_578026b7cc',
                image: true,
                length: 1024,
                mimeType: 'image/png',
                referenceUrl: '/dA/temp_578026b7cc',
                thumbnailUrl: '/dA/temp_578026b7cc/thumb'
            };

            it('should close with the temp file when a save succeeds', () => {
                dispatcher.dispatch(imageEditorLifecycleEvents.saveSucceeded(tempFile));

                expect(dialogRef.close).toHaveBeenCalledWith(tempFile);
            });

            it('should NOT close when a save fails', () => {
                dispatcher.dispatch(imageEditorLifecycleEvents.saveFailed(new Error('boom')));

                expect(dialogRef.close).not.toHaveBeenCalled();
            });

            it('should surface the save error when saveStatus is error', () => {
                saveStatus.set('error');
                saveError.set('Failed to save image');
                spectator.detectChanges();

                const error = spectator.query(byTestId('image-editor-save-error'));
                expect(error).toExist();
                expect(error?.textContent?.trim()).toBe('Failed to save image');
            });

            it('should not render the save error when saveStatus is idle', () => {
                saveStatus.set('idle');
                spectator.detectChanges();

                expect(spectator.query(byTestId('image-editor-save-error'))).not.toExist();
            });
        });

        describe('undo/redo shortcuts', () => {
            it('should dispatch undoRequested on Ctrl/Cmd+Z when undo is available', () => {
                canUndo.set(true);

                document.dispatchEvent(new KeyboardEvent('keydown', { key: 'z', ctrlKey: true }));

                expect(dispatcher.dispatch).toHaveBeenCalledWith(
                    imageEditorHistoryEvents.undoRequested(),
                    { scope: 'self' }
                );
            });

            it('should dispatch redoRequested on Ctrl/Cmd+Shift+Z when redo is available', () => {
                canRedo.set(true);

                document.dispatchEvent(
                    new KeyboardEvent('keydown', { key: 'z', metaKey: true, shiftKey: true })
                );

                expect(dispatcher.dispatch).toHaveBeenCalledWith(
                    imageEditorHistoryEvents.redoRequested(),
                    { scope: 'self' }
                );
            });

            it('should dispatch redoRequested on Ctrl+Y when redo is available', () => {
                canRedo.set(true);

                document.dispatchEvent(new KeyboardEvent('keydown', { key: 'y', ctrlKey: true }));

                expect(dispatcher.dispatch).toHaveBeenCalledWith(
                    imageEditorHistoryEvents.redoRequested(),
                    { scope: 'self' }
                );
            });

            it('should not dispatch undo when there is nothing to undo', () => {
                canUndo.set(false);

                document.dispatchEvent(new KeyboardEvent('keydown', { key: 'z', ctrlKey: true }));

                expect(dispatcher.dispatch).not.toHaveBeenCalledWith(
                    imageEditorHistoryEvents.undoRequested(),
                    { scope: 'self' }
                );
            });

            it('should ignore the shortcut while a text field has focus', () => {
                canUndo.set(true);

                const input = document.createElement('input');
                spectator.element.appendChild(input);
                input.dispatchEvent(
                    new KeyboardEvent('keydown', { key: 'z', ctrlKey: true, bubbles: true })
                );

                expect(dispatcher.dispatch).not.toHaveBeenCalledWith(
                    imageEditorHistoryEvents.undoRequested(),
                    { scope: 'self' }
                );
            });
        });
    });
}

describeWith('inode', { inode: 'i1', variable: 'fileAsset', fieldName: 'fileAsset' });
describeWith('tempId', { tempId: 'temp_x', variable: 'fileAsset', fieldName: 'fileAsset' });

/**
 * Host-independence of the editor's messaging.
 *
 * Unlike the suites above, this one mounts the **real** child tree: no `overrideComponents`, no
 * `MockComponent`, and no `componentProviders` (which would replace the component's own
 * `providers` — including the very `MessageService` under test). That is deliberate and load
 * bearing. `DotImageEditorAddressBarComponent`, nested inside the canvas, injects PrimeNG's
 * `MessageService` non-optionally; every other spec in this library replaces one of the components
 * on that path with a mock, so `inject(MessageService)` never actually runs unprovided and the
 * suite stayed green while the editor was completely broken in two of its three hosts (#37398).
 *
 * No `MessageService` is provided here on purpose: the editor must supply its own, so it renders in
 * any host rather than relying on an ancestor injector happening to have one.
 */
describe('DotImageEditorComponent (no ambient MessageService)', () => {
    let spectator: Spectator<DotImageEditorComponent>;

    const data: ImageEditorOpenParams = {
        inode: 'i1',
        variable: 'fileAsset',
        fieldName: 'fileAsset'
    };

    const assetMeta: AssetMeta = {
        naturalWidth: 800,
        naturalHeight: 600,
        originalBytes: 2048
    };

    const createComponent = createComponentFactory({
        component: DotImageEditorComponent,
        providers: [
            provideNoopAnimations(),
            Dispatcher,
            mockProvider(DotMessageService, { get: jest.fn((key: string) => key) }),
            mockProvider(DynamicDialogRef, { close: jest.fn() }),
            { provide: DynamicDialogConfig, useValue: { data } },
            // Stub the boundaries the real tree reaches — HTTP and server config — so the mount
            // needs no backend. Everything else resolves from the library itself.
            mockProvider(DotImageEditorService, {
                loadAssetMeta: jest.fn(() => of(assetMeta)),
                loadPreviewImage: jest.fn(() => of('blob:preview')),
                getFileSize: jest.fn(() => of(null)),
                triggerDownload: jest.fn(),
                saveEditedImage: jest.fn()
            }),
            mockProvider(DotPropertiesService, { getKey: jest.fn(() => of('false')) })
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should instantiate and render without an ambient MessageService', () => {
        expect(spectator.component).toBeTruthy();
        expect(spectator.query(byTestId('image-editor-root'))).toExist();
    });

    it('should render the real address bar, which is what injects MessageService', () => {
        expect(spectator.query('dot-image-editor-address-bar')).toExist();
    });

    it('should provide its own MessageService rather than resolving one from the host', () => {
        // Resolved through the component injector. Nothing in this factory's `providers`
        // supplies one, so a successful resolution can only have come from the component's own
        // `providers` — which is the property the host chain must no longer be asked for.
        expect(spectator.inject(MessageService, true)).toBeTruthy();
        // And confirm the premise: it is genuinely absent from the ambient/module injector.
        expect(() => spectator.inject(MessageService)).toThrow();
    });

    it('should render exactly one toast outlet inside the dialog', () => {
        expect(spectator.queryAll(byTestId('dot-toast'))).toHaveLength(1);
    });
});
