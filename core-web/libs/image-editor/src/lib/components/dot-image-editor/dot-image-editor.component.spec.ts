import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@ngneat/spectator/jest';
import { Dispatcher } from '@ngrx/signals/events';
import { MockComponent } from 'ng-mocks';

import { signal } from '@angular/core';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { Confirmation, ConfirmationService } from 'primeng/api';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSTempFile } from '@dotcms/dotcms-models';

import { DotImageEditorComponent } from './dot-image-editor.component';

import { ImageEditorOpenParams } from '../../models/image-editor.models';
import { imageEditorLifecycleEvents } from '../../store/image-editor.events';
import { ImageEditorStore } from '../../store/image-editor.store';
import { DotImageEditorCanvasComponent } from '../dot-image-editor-canvas/dot-image-editor-canvas.component';
import { DotImageEditorFooterComponent } from '../dot-image-editor-footer/dot-image-editor-footer.component';
import { DotImageEditorHeaderComponent } from '../dot-image-editor-header/dot-image-editor-header.component';
import { DotImageEditorPanelsComponent } from '../dot-image-editor-panels/dot-image-editor-panels.component';

const TEMP_FILE: DotCMSTempFile = {
    id: 'temp_saved',
    fileName: 'edited.jpg',
    length: 1024,
    folder: '',
    image: true,
    mimeType: 'image/jpeg',
    referenceUrl: '',
    thumbnailUrl: ''
};

/** Builds the suite for a given set of open params, asserting the shared shell behavior. */
function describeWith(label: string, data: ImageEditorOpenParams): void {
    describe(`DotImageEditorComponent (${label})`, () => {
        let spectator: Spectator<DotImageEditorComponent>;
        let dispatcher: SpyObject<Dispatcher>;
        let dialogRef: SpyObject<DynamicDialogRef>;
        let confirmationService: SpyObject<ConfirmationService>;

        const savedTempFile = signal<DotCMSTempFile | null>(null);
        const isDirty = signal(false);

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
            // `<p-confirmDialog />` subscribes to it; we spy on `confirm`) while
            // mocking only the store.
            componentProviders: [
                ConfirmationService,
                mockProvider(ImageEditorStore, { savedTempFile, isDirty })
            ],
            // Isolate the shell from the children's own store/dispatch wiring.
            overrideComponents: [
                [
                    DotImageEditorComponent,
                    {
                        remove: {
                            imports: [
                                DotImageEditorHeaderComponent,
                                DotImageEditorCanvasComponent,
                                DotImageEditorPanelsComponent,
                                DotImageEditorFooterComponent
                            ]
                        },
                        add: {
                            imports: [
                                MockComponent(DotImageEditorHeaderComponent),
                                MockComponent(DotImageEditorCanvasComponent),
                                MockComponent(DotImageEditorPanelsComponent),
                                MockComponent(DotImageEditorFooterComponent)
                            ]
                        }
                    }
                ]
            ]
        });

        beforeEach(() => {
            // The DynamicDialogRef `close` mock is shared across tests in this
            // describe; clear it so prior tests' close() calls don't leak into the
            // "not closed" assertions.
            jest.clearAllMocks();
            savedTempFile.set(null);
            isDirty.set(false);

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

        it('should render the root and the four child components', () => {
            expect(spectator.query(byTestId('image-editor-root'))).toExist();
            expect(spectator.query('dot-image-editor-header')).toExist();
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

        it('should close with the saved temp file once a save succeeds', () => {
            savedTempFile.set(TEMP_FILE);
            spectator.detectChanges();

            expect(dialogRef.close).toHaveBeenCalledWith(TEMP_FILE);
        });

        it('should close with null when the header close is triggered and not dirty', () => {
            const header = spectator.query(DotImageEditorHeaderComponent)!;
            header.close.emit();

            expect(dialogRef.close).toHaveBeenCalledWith(null);
            expect(confirmationService.confirm).not.toHaveBeenCalled();
        });

        it('should close with null when the footer cancel is triggered and not dirty', () => {
            const footer = spectator.query(DotImageEditorFooterComponent)!;
            footer.cancel.emit();

            expect(dialogRef.close).toHaveBeenCalledWith(null);
            expect(confirmationService.confirm).not.toHaveBeenCalled();
        });

        it('should confirm before closing when there are unsaved edits', () => {
            isDirty.set(true);

            spectator.query(DotImageEditorHeaderComponent)!.close.emit();

            expect(confirmationService.confirm).toHaveBeenCalledTimes(1);
            expect(dialogRef.close).not.toHaveBeenCalled();
        });

        it('should close with null when the discard confirmation is accepted', () => {
            isDirty.set(true);

            spectator.query(DotImageEditorFooterComponent)!.cancel.emit();

            const confirmation = (confirmationService.confirm as jest.Mock).mock
                .calls[0][0] as Confirmation;
            confirmation.accept?.();

            expect(dialogRef.close).toHaveBeenCalledWith(null);
        });
    });
}

describeWith('inode', { inode: 'i1', variable: 'fileAsset', fieldName: 'fileAsset' });
describeWith('tempId', { tempId: 'temp_x', variable: 'fileAsset', fieldName: 'fileAsset' });
