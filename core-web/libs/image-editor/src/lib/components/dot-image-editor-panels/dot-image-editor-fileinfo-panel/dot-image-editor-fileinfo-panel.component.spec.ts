import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { Dispatcher } from '@ngrx/signals/events';

import { signal } from '@angular/core';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { Select } from 'primeng/select';

import { DotMessageService } from '@dotcms/data-access';

import { DotImageEditorFileInfoPanelComponent } from './dot-image-editor-fileinfo-panel.component';

import { FileInfoState } from '../../../models/image-editor.models';
import { imageEditorFileInfoEvents } from '../../../store/image-editor.events';
import { ImageEditorStore } from '../../../store/image-editor.store';

const FILE_INFO: FileInfoState = {
    compression: 'none',
    quality: 85,
    currentBytes: null,
    originalBytes: null
};

describe('DotImageEditorFileInfoPanelComponent', () => {
    let spectator: Spectator<DotImageEditorFileInfoPanelComponent>;
    let dispatcher: Dispatcher;

    const fileInfo = signal<FileInfoState>(FILE_INFO);
    const assetContext = signal({ naturalWidth: 0, naturalHeight: 0 });

    const createComponent = createComponentFactory({
        component: DotImageEditorFileInfoPanelComponent,
        providers: [
            provideNoopAnimations(),
            mockProvider(DotMessageService, { get: jest.fn((key: string) => key) })
        ],
        componentProviders: [Dispatcher, mockProvider(ImageEditorStore, { fileInfo, assetContext })]
    });

    beforeEach(() => {
        fileInfo.set(FILE_INFO);
        assetContext.set({ naturalWidth: 0, naturalHeight: 0 });
        spectator = createComponent();
        dispatcher = spectator.inject(Dispatcher, true);
        jest.spyOn(dispatcher, 'dispatch');
    });

    it('should render the compression select', () => {
        expect(spectator.query(byTestId('image-editor-compression-select'))).toExist();
    });

    it('should dispatch compressionChanged on the select change', () => {
        const select = spectator.query(Select);
        select!.onChange.emit({ originalEvent: new Event('change'), value: 'avif' });

        const event = dispatchedEvent(imageEditorFileInfoEvents.compressionChanged.type);
        expect(event).toBeDefined();
        expect(event!.payload).toBe('avif');
    });

    describe('when compression is none', () => {
        it('should hide the quality slider', () => {
            expect(spectator.query(byTestId('image-editor-quality-slider'))).toBeNull();
        });
    });

    describe('when compression is active', () => {
        beforeEach(() => {
            fileInfo.set({ ...FILE_INFO, compression: 'jpeg' });
            spectator.detectChanges();
        });

        it('should show the quality slider', () => {
            expect(spectator.query(byTestId('image-editor-quality-slider'))).toExist();
        });
    });

    describe('file size', () => {
        it('should render an em dash when the size is unknown', () => {
            expect(spectator.query(byTestId('image-editor-filesize-value'))!.textContent).toContain(
                '—'
            );
        });

        it('should format the current size in KB', () => {
            fileInfo.set({ ...FILE_INFO, currentBytes: 2048 });
            spectator.detectChanges();

            expect(spectator.query(byTestId('image-editor-filesize-value'))!.textContent).toContain(
                'KB'
            );
        });
    });

    describe('original size', () => {
        it('should render an em dash before the asset loads', () => {
            expect(
                spectator.query(byTestId('image-editor-originalsize-value'))!.textContent
            ).toContain('—');
        });

        it('should render the natural dimensions once loaded', () => {
            assetContext.set({ naturalWidth: 3024, naturalHeight: 1964 });
            spectator.detectChanges();

            expect(
                spectator.query(byTestId('image-editor-originalsize-value'))!.textContent
            ).toContain('3024 × 1964 px');
        });
    });

    /**
     * Finds the dispatched event matching the given type. `injectDispatch`
     * forwards a `{ scope: 'self' }` options argument, so the event is read from
     * the first call argument.
     */
    function dispatchedEvent(type: string): { type: string; payload?: unknown } | undefined {
        const call = (dispatcher.dispatch as jest.Mock).mock.calls.find(
            ([dispatched]) => dispatched.type === type
        );

        return call?.[0];
    }
});
