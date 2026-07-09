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
    const saveStatus = signal<'idle' | 'saving' | 'error'>('idle');

    const createComponent = createComponentFactory({
        component: DotImageEditorFooterComponent,
        imports: [DotMessagePipe],
        providers: [mockProvider(DotMessageService, { get: jest.fn((key: string) => key) })],
        componentProviders: [Dispatcher, mockProvider(ImageEditorStore, { isBusy, saveStatus })]
    });

    beforeEach(() => {
        isBusy.set(false);
        saveStatus.set('idle');

        spectator = createComponent();
        dispatcher = spectator.inject(Dispatcher, true);
        jest.spyOn(dispatcher, 'dispatch');
    });

    it('should render the cancel, download and save actions', () => {
        expect(spectator.query(byTestId('image-editor-cancel-btn'))).toBeTruthy();
        expect(spectator.query(byTestId('image-editor-download-btn'))).toBeTruthy();
        expect(spectator.query(byTestId('image-editor-save-btn'))).toBeTruthy();
    });

    it('should emit cancel when Cancel is clicked', () => {
        const cancelSpy = jest.spyOn(spectator.component.$cancel, 'emit');

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

    it('should disable Download when isBusy is true', () => {
        isBusy.set(true);
        spectator.detectChanges();

        expect(nativeButton(spectator, 'image-editor-download-btn').disabled).toBe(true);
    });

    it('should dispatch saveRequested when Save is clicked', () => {
        spectator.click(nativeButton(spectator, 'image-editor-save-btn'));

        expect(dispatcher.dispatch).toHaveBeenCalledWith(
            imageEditorLifecycleEvents.saveRequested(),
            { scope: 'self' }
        );
    });

    it('should show a loading spinner and disable Save while a save is in flight', () => {
        saveStatus.set('saving');
        spectator.detectChanges();

        const button = nativeButton(spectator, 'image-editor-save-btn');
        expect(button.disabled).toBe(true);
        expect(button.classList).toContain('p-button-loading');
    });

    it('should show a loading spinner and disable Save while a filter is applying (busy)', () => {
        isBusy.set(true);
        spectator.detectChanges();

        const button = nativeButton(spectator, 'image-editor-save-btn');
        expect(button.disabled).toBe(true);
        expect(button.classList).toContain('p-button-loading');
    });
});
