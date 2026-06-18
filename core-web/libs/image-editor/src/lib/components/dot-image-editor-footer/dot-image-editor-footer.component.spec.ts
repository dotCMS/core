import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@ngneat/spectator/jest';
import { Dispatcher } from '@ngrx/signals/events';

import { signal } from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { DotImageEditorFooterComponent } from './dot-image-editor-footer.component';

import { imageEditorLifecycleEvents } from '../../store/image-editor.events';
import { ImageEditorStore } from '../../store/image-editor.store';

/** Resolves the inner native button for a PrimeNG control by its testid. */
function nativeButton(spectator: Spectator<DotImageEditorFooterComponent>, testId: string) {
    return spectator.query(byTestId(testId))?.querySelector('button') as HTMLElement;
}

describe('DotImageEditorFooterComponent', () => {
    let spectator: Spectator<DotImageEditorFooterComponent>;
    let dispatcher: SpyObject<Dispatcher>;

    const isBusy = signal(false);
    const canSave = signal(true);
    const saveStatus = signal<'idle' | 'saving' | 'saved' | 'error'>('idle');

    const createComponent = createComponentFactory({
        component: DotImageEditorFooterComponent,
        imports: [DotMessagePipe],
        providers: [mockProvider(DotMessageService, { get: jest.fn((key: string) => key) })],
        componentProviders: [
            Dispatcher,
            mockProvider(ImageEditorStore, {
                isBusy,
                canSave,
                saveStatus,
                assetContext: () => ({ fileName: 'x.jpg' })
            })
        ]
    });

    beforeEach(() => {
        isBusy.set(false);
        canSave.set(true);
        saveStatus.set('idle');

        spectator = createComponent();
        dispatcher = spectator.inject(Dispatcher, true);
        jest.spyOn(dispatcher, 'dispatch');
    });

    it('should render the action buttons', () => {
        expect(spectator.query(byTestId('image-editor-cancel-btn'))).toBeTruthy();
        expect(spectator.query(byTestId('image-editor-download-btn'))).toBeTruthy();
        expect(spectator.query(byTestId('image-editor-save-btn'))).toBeTruthy();
    });

    it('should emit cancel when Cancel is clicked', () => {
        const cancelSpy = jest.spyOn(spectator.component.cancel, 'emit');

        spectator.click(nativeButton(spectator, 'image-editor-cancel-btn'));

        expect(cancelSpy).toHaveBeenCalledTimes(1);
    });

    it('should dispatch downloadRequested when Download is clicked', () => {
        spectator.click(nativeButton(spectator, 'image-editor-download-btn'));

        expect(dispatcher.dispatch).toHaveBeenCalledWith(
            imageEditorLifecycleEvents.downloadRequested(),
            { scope: 'self' }
        );
    });

    it('should dispatch saveRequested when Save is clicked', () => {
        spectator.click(nativeButton(spectator, 'image-editor-save-btn'));

        expect(dispatcher.dispatch).toHaveBeenCalledWith(
            imageEditorLifecycleEvents.saveRequested(),
            {
                scope: 'self'
            }
        );
    });

    it('should expose a "Save as…" menu item that dispatches saveAsRequested', () => {
        const saveAsItem = spectator.component['saveMenuItems'][0];

        expect(saveAsItem.label).toBe('edit.content.image-editor.footer.save-as');

        saveAsItem.command?.({});

        expect(dispatcher.dispatch).toHaveBeenCalledWith(
            imageEditorLifecycleEvents.saveAsRequested({ fileName: 'x.jpg' }),
            { scope: 'self' }
        );
    });

    describe('disabled states', () => {
        it('should disable Save when canSave is false', () => {
            canSave.set(false);
            spectator.detectChanges();

            expect(nativeButton(spectator, 'image-editor-save-btn').disabled).toBe(true);
        });

        it('should disable Download when isBusy is true', () => {
            isBusy.set(true);
            spectator.detectChanges();

            expect(nativeButton(spectator, 'image-editor-download-btn').disabled).toBe(true);
        });
    });
});
