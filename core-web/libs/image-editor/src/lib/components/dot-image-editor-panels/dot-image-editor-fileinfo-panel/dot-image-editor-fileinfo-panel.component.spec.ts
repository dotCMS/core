import { Dispatcher } from '@ngrx/signals/events';
import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';
import { BehaviorSubject } from 'rxjs';

import { signal } from '@angular/core';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { Select } from 'primeng/select';
import { Slider } from 'primeng/slider';

import { DotMessageService, DotPropertiesService } from '@dotcms/data-access';

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
    const assetContext = signal({ naturalWidth: 0, naturalHeight: 0, mimeType: '' });
    const focalPoint = signal({ x: 0.5, y: 0.5 });
    // Drives the libvips config value the panel reads via DotPropertiesService.getKey;
    // push a new value to flip whether the AVIF option is offered.
    const libvips$ = new BehaviorSubject<boolean | string>(false);

    const createComponent = createComponentFactory({
        component: DotImageEditorFileInfoPanelComponent,
        providers: [
            provideNoopAnimations(),
            mockProvider(DotMessageService, { get: jest.fn((key: string) => key) }),
            mockProvider(DotPropertiesService, { getKey: () => libvips$ })
        ],
        componentProviders: [
            Dispatcher,
            mockProvider(ImageEditorStore, { fileInfo, assetContext, focalPoint })
        ]
    });

    beforeEach(() => {
        fileInfo.set(FILE_INFO);
        assetContext.set({ naturalWidth: 0, naturalHeight: 0, mimeType: '' });
        focalPoint.set({ x: 0.5, y: 0.5 });
        libvips$.next(false);
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

        it('should dispatch qualityChanged on the quality slider slide end', () => {
            const slider = spectator.query(Slider);
            slider!.onSlideEnd.emit({ originalEvent: new Event('mouseup'), value: 70 });

            const event = dispatchedEvent(imageEditorFileInfoEvents.qualityChanged.type);
            expect(event).toBeDefined();
            expect(event!.payload).toBe(70);
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
            assetContext.set({ naturalWidth: 3024, naturalHeight: 1964, mimeType: 'image/jpeg' });
            spectator.detectChanges();

            expect(
                spectator.query(byTestId('image-editor-originalsize-value'))!.textContent
            ).toContain('3024 × 1964 px');
        });
    });

    describe('format', () => {
        it('shows the source format from the mime type when not converting', () => {
            assetContext.set({ naturalWidth: 10, naturalHeight: 10, mimeType: 'image/png' });
            spectator.detectChanges();

            expect(spectator.query(byTestId('image-editor-format-value'))!.textContent).toContain(
                'PNG'
            );
        });

        it('shows the chosen compression format', () => {
            fileInfo.set({ ...FILE_INFO, compression: 'webp' });
            spectator.detectChanges();

            expect(spectator.query(byTestId('image-editor-format-value'))!.textContent).toContain(
                'WEBP'
            );
        });

        it('falls back to an em dash when the format is unknown', () => {
            assetContext.set({ naturalWidth: 10, naturalHeight: 10, mimeType: '' });
            spectator.detectChanges();

            expect(spectator.query(byTestId('image-editor-format-value'))!.textContent).toContain(
                '—'
            );
        });
    });

    describe('focal point', () => {
        it('defaults to the centre', () => {
            expect(
                spectator.query(byTestId('image-editor-focalpoint-value'))!.textContent
            ).toContain('0.50, 0.50');
        });

        it('reflects the store value', () => {
            focalPoint.set({ x: 0.19, y: 0.44 });
            spectator.detectChanges();

            expect(
                spectator.query(byTestId('image-editor-focalpoint-value'))!.textContent
            ).toContain('0.19, 0.44');
        });
    });

    describe('AVIF option (libvips-gated)', () => {
        it('hides AVIF when libvips is disabled', () => {
            const values = spectator.component['$compressionOptions']().map(
                (option) => option.value
            );

            expect(values).toEqual(['none', 'auto', 'jpeg', 'webp']);
        });

        it('shows AVIF when libvips is enabled', () => {
            libvips$.next(true);
            spectator.detectChanges();

            expect(
                spectator.component['$compressionOptions']().map((option) => option.value)
            ).toContain('avif');
        });

        it('treats the string "true" as enabled', () => {
            libvips$.next('true');
            spectator.detectChanges();

            expect(
                spectator.component['$compressionOptions']().map((option) => option.value)
            ).toContain('avif');
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
