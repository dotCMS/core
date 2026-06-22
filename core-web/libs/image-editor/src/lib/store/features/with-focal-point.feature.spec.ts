import { signalStore, withState } from '@ngrx/signals';
import { Dispatcher, injectDispatch } from '@ngrx/signals/events';

import { Injector, runInInjectionContext, Type } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { withFocalPoint } from './with-focal-point.feature';

import { imageEditorToolEvents } from '../image-editor.events';
import { initialImageEditorState } from '../image-editor.state';

const FocalStore = signalStore(withState(initialImageEditorState), withFocalPoint());
const FocalStoreSized = signalStore(
    withState({
        ...initialImageEditorState,
        assetContext: {
            ...initialImageEditorState.assetContext,
            naturalWidth: 1000,
            naturalHeight: 800
        }
    }),
    withFocalPoint()
);

function setup<T>(StoreClass: Type<T>) {
    TestBed.configureTestingModule({ providers: [StoreClass, Dispatcher] });
    const injector = TestBed.inject(Injector);
    const store = TestBed.inject(StoreClass);
    let tool!: ReturnType<typeof injectDispatch<typeof imageEditorToolEvents>>;
    runInInjectionContext(injector, () => {
        tool = injectDispatch(imageEditorToolEvents);
    });

    return { store, tool };
}

describe('withFocalPoint', () => {
    it('records the focal point WITHOUT reloading the preview', () => {
        const { store, tool } = setup(FocalStore);

        tool.focalPointSet({ x: 0.25, y: 0.75 });

        expect(store.focalPoint()).toEqual({ x: 0.25, y: 0.75, active: true });
        expect(store.history().at(-1)?.category).toBe('focal');
        expect(store.history().at(-1)?.label).toBe('Focal point 0.25, 0.75');
        // It's a save-time anchor: no cache-bust, no loading.
        expect(store.previewStatus()).toBe('idle');
        expect(store.cacheBust()).toBe(0);
    });

    it('clears the focal point back to the centered default', () => {
        const { store, tool } = setup(FocalStore);

        tool.focalPointSet({ x: 0.25, y: 0.75 });
        tool.focalPointCleared();

        expect(store.focalPoint()).toEqual({ x: 0.5, y: 0.5, active: false });
    });

    it('derives a focal-centered aspect crop when natural dimensions are known', () => {
        const { store, tool } = setup(FocalStoreSized);

        tool.focalPointSet({ x: 0.8, y: 0.5 });
        tool.aspectCropApplied({ aspect: 1, label: '1:1' });

        // 1:1 of 1000×800 → 800×800; centered on x=0.8 clamps to the right edge (x=200).
        expect(store.crop()).toEqual({ x: 200, y: 0, w: 800, h: 800, active: true, aspect: 1 });
        expect(store.transform().scale).toBe(100);
        expect(store.activeTool()).toBe('move');
        expect(store.history().at(-1)?.category).toBe('crop');
        expect(store.previewStatus()).toBe('loading');
    });

    it('ignores an aspect crop when natural dimensions are unknown', () => {
        const { store, tool } = setup(FocalStore); // naturalWidth/Height are 0

        tool.aspectCropApplied({ aspect: 1, label: '1:1' });

        expect(store.crop().active).toBe(false);
        expect(store.history()).toHaveLength(0);
        expect(store.previewStatus()).toBe('idle');
    });
});
