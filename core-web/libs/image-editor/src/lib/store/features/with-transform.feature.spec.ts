import { signalStore, withState } from '@ngrx/signals';
import { Dispatcher, injectDispatch } from '@ngrx/signals/events';

import { Injector, runInInjectionContext, Type } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { withTransform } from './with-transform.feature';

import { CropState } from '../../models/image-editor.models';
import { imageEditorTransformEvents } from '../image-editor.events';
import { initialImageEditorState } from '../image-editor.state';

const SIZED_CONTEXT = {
    ...initialImageEditorState.assetContext,
    naturalWidth: 1000,
    naturalHeight: 800
};
const ACTIVE_CROP: CropState = { x: 0, y: 0, w: 100, h: 100, active: true, aspect: null };

const TransformStore = signalStore(withState(initialImageEditorState), withTransform());
const TransformStoreSized = signalStore(
    withState({ ...initialImageEditorState, assetContext: SIZED_CONTEXT }),
    withTransform()
);
const TransformStoreCropped = signalStore(
    withState({ ...initialImageEditorState, assetContext: SIZED_CONTEXT, crop: ACTIVE_CROP }),
    withTransform()
);

/** Boots a minimal store for the given seeded class and resolves its dispatcher. */
function setup<T>(StoreClass: Type<T>) {
    TestBed.configureTestingModule({ providers: [StoreClass, Dispatcher] });
    const injector = TestBed.inject(Injector);
    const store = TestBed.inject(StoreClass);
    let transform!: ReturnType<typeof injectDispatch<typeof imageEditorTransformEvents>>;
    runInInjectionContext(injector, () => {
        transform = injectDispatch(imageEditorTransformEvents);
    });

    return { store, transform };
}

describe('withTransform', () => {
    it('clamps scale to the max', () => {
        const { store, transform } = setup(TransformStore);
        transform.scaleChanged(500);
        expect(store.transform().scale).toBe(400);
    });

    it('clears an active crop when scaling away from 100 (resize XOR crop)', () => {
        const { store, transform } = setup(TransformStoreCropped);
        transform.scaleChanged(50);
        expect(store.transform().scale).toBe(50);
        expect(store.crop().active).toBe(false);
    });

    it('keeps the crop when scale stays at 100', () => {
        const { store, transform } = setup(TransformStoreCropped);
        transform.scaleChanged(100);
        expect(store.crop().active).toBe(true);
    });

    it('clamps rotation to both bounds and labels it', () => {
        const { store, transform } = setup(TransformStore);
        transform.rotateChanged(200);
        expect(store.transform().rotateDeg).toBe(180);
        expect(store.history().at(-1)?.label).toBe('Rotate 180°');

        transform.rotateChanged(-200);
        expect(store.transform().rotateDeg).toBe(-180);
    });

    it('toggles horizontal and vertical flip as separate flip-category entries', () => {
        const { store, transform } = setup(TransformStore);
        transform.flipHToggled();
        transform.flipVToggled();
        expect(store.transform().flipH).toBe(true);
        expect(store.transform().flipV).toBe(true);
        // Both are the `flip` category but distinct controls, so they must not coalesce.
        expect(store.history().map((entry) => entry.label)).toEqual([
            'Flip horizontal',
            'Flip vertical'
        ]);
    });

    it('records each flip of the same axis as its own history step (discrete toggle)', () => {
        const { store, transform } = setup(TransformStore);
        transform.flipHToggled();
        transform.flipHToggled();
        // Two clicks net back to the un-flipped state...
        expect(store.transform().flipH).toBe(false);
        // ...but each click is a distinct undoable step — it must NOT coalesce into
        // one silently-updated entry (the QA bug: applied twice, shown once).
        expect(store.history().map((entry) => entry.label)).toEqual([
            'Flip horizontal',
            'Flip horizontal'
        ]);
        expect(store.historyIndex()).toBe(1);
    });

    it('clears an active crop when explicit output dimensions are set', () => {
        const { store, transform } = setup(TransformStoreCropped);
        transform.outputDimsChanged({ width: 500, height: null });
        expect(store.transform().outputWidth).toBe(500);
        expect(store.crop().active).toBe(false);
    });

    it('keeps the crop when output dimensions are cleared (both null)', () => {
        const { store, transform } = setup(TransformStoreCropped);
        transform.outputDimsChanged({ width: null, height: null });
        expect(store.crop().active).toBe(true);
    });

    describe('outputDimensions selector', () => {
        it('reflects the natural size with no edits', () => {
            const { store } = setup(TransformStoreSized);
            expect(store.outputDimensions()).toEqual({ width: 1000, height: 800 });
        });

        it('scales the natural size by the scale percentage', () => {
            const { store, transform } = setup(TransformStoreSized);
            transform.scaleChanged(50);
            expect(store.outputDimensions()).toEqual({ width: 500, height: 400 });
        });

        it('derives the missing dimension from the aspect ratio', () => {
            const { store, transform } = setup(TransformStoreSized);
            transform.outputDimsChanged({ width: 300, height: null });
            // height = round(300 / (1000/800)) = 240
            expect(store.outputDimensions()).toEqual({ width: 300, height: 240 });
        });
    });
});
