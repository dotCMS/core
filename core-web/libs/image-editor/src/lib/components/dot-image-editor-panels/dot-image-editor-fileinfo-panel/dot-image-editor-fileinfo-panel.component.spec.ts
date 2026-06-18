import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { Dispatcher } from '@ngrx/signals/events';

import { signal } from '@angular/core';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { SelectButton } from 'primeng/selectbutton';

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

    const createComponent = createComponentFactory({
        component: DotImageEditorFileInfoPanelComponent,
        providers: [
            provideNoopAnimations(),
            mockProvider(DotMessageService, { get: jest.fn((key: string) => key) })
        ],
        componentProviders: [Dispatcher, mockProvider(ImageEditorStore, { fileInfo })]
    });

    beforeEach(() => {
        fileInfo.set(FILE_INFO);
        spectator = createComponent();
        dispatcher = spectator.inject(Dispatcher, true);
        jest.spyOn(dispatcher, 'dispatch');
    });

    it('should render the compression select', () => {
        expect(spectator.query(byTestId('image-editor-compression-select'))).toExist();
    });

    it('should dispatch compressionChanged on the select change', () => {
        const select = spectator.query(SelectButton);
        select!.onChange.emit({ originalEvent: new Event('click'), value: 'webp' });

        const event = dispatchedEvent(imageEditorFileInfoEvents.compressionChanged.type);
        expect(event).toBeDefined();
        expect(event!.payload).toBe('webp');
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
