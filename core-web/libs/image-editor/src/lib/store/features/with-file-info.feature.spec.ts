import { signalStore, withState } from '@ngrx/signals';
import { Dispatcher, injectDispatch } from '@ngrx/signals/events';

import { Injector, runInInjectionContext } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { withFileInfo } from './with-file-info.feature';

import { imageEditorFileInfoEvents } from '../image-editor.events';
import { initialImageEditorState } from '../image-editor.state';

const FileInfoStore = signalStore(withState(initialImageEditorState), withFileInfo());

describe('withFileInfo', () => {
    let store: InstanceType<typeof FileInfoStore>;
    let fileInfo: ReturnType<typeof injectDispatch<typeof imageEditorFileInfoEvents>>;

    beforeEach(() => {
        TestBed.configureTestingModule({ providers: [FileInfoStore, Dispatcher] });
        const injector = TestBed.inject(Injector);
        store = TestBed.inject(FileInfoStore);
        runInInjectionContext(injector, () => {
            fileInfo = injectDispatch(imageEditorFileInfoEvents);
        });
    });

    it('sets the compression mode under the compression category', () => {
        fileInfo.compressionChanged('jpeg');
        expect(store.fileInfo().compression).toBe('jpeg');
        expect(store.history()[0].category).toBe('compression');
        expect(store.history()[0].label).toBe('Compression JPEG');
    });

    it('sets and labels quality', () => {
        fileInfo.qualityChanged(50);
        expect(store.fileInfo().quality).toBe(50);
        expect(store.history()[0].label).toBe('Quality 50');
    });

    it('clamps quality into 0..100', () => {
        fileInfo.qualityChanged(150);
        expect(store.fileInfo().quality).toBe(100);

        fileInfo.qualityChanged(-50);
        expect(store.fileInfo().quality).toBe(0);
    });
});
