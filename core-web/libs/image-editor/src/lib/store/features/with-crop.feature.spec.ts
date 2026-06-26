import { signalStore, withState } from '@ngrx/signals';
import { Dispatcher, injectDispatch } from '@ngrx/signals/events';

import { Injector, runInInjectionContext } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { withCrop } from './with-crop.feature';

import { CropState } from '../../models/image-editor.models';
import { imageEditorToolEvents } from '../image-editor.events';
import { initialImageEditorState } from '../image-editor.state';

// Seed a pending resize so applying a crop can be shown to clear it.
const RESIZED = {
    ...initialImageEditorState,
    transform: { ...initialImageEditorState.transform, scale: 50, outputWidth: 500 }
};
const ACTIVE_CROP: CropState = { x: 10, y: 10, w: 200, h: 150, active: true, aspect: null };

const CropStore = signalStore(withState(RESIZED), withCrop());
const CropStoreActive = signalStore(
    withState({ ...initialImageEditorState, crop: ACTIVE_CROP, activeTool: 'crop' as const }),
    withCrop()
);

describe('withCrop', () => {
    describe('cropApplied', () => {
        let store: InstanceType<typeof CropStore>;
        let tool: ReturnType<typeof injectDispatch<typeof imageEditorToolEvents>>;

        beforeEach(() => {
            TestBed.configureTestingModule({ providers: [CropStore, Dispatcher] });
            const injector = TestBed.inject(Injector);
            store = TestBed.inject(CropStore);
            runInInjectionContext(injector, () => (tool = injectDispatch(imageEditorToolEvents)));
        });

        it('applies a crop, clears resize, returns to move and adds a crop entry', () => {
            tool.cropApplied({ x: 10, y: 10, w: 200, h: 150, active: false, aspect: null });

            expect(store.crop()).toEqual({
                x: 10,
                y: 10,
                w: 200,
                h: 150,
                active: true,
                aspect: null
            });
            expect(store.transform().scale).toBe(100);
            expect(store.transform().outputWidth).toBeNull();
            expect(store.activeTool()).toBe('move');
            expect(store.history().at(-1)?.category).toBe('crop');
            expect(store.previewStatus()).toBe('loading');
            expect(store.cacheBust()).toBe(1);
        });
    });

    describe('cropCancelled', () => {
        let store: InstanceType<typeof CropStoreActive>;
        let tool: ReturnType<typeof injectDispatch<typeof imageEditorToolEvents>>;

        beforeEach(() => {
            TestBed.configureTestingModule({ providers: [CropStoreActive, Dispatcher] });
            const injector = TestBed.inject(Injector);
            store = TestBed.inject(CropStoreActive);
            runInInjectionContext(injector, () => (tool = injectDispatch(imageEditorToolEvents)));
        });

        it('cancels a crop back to inactive and the move tool', () => {
            tool.cropCancelled();

            expect(store.crop().active).toBe(false);
            expect(store.activeTool()).toBe('move');
        });
    });
});
