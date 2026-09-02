import { signalStore, withState } from '@ngrx/signals';
import { Dispatcher, injectDispatch } from '@ngrx/signals/events';

import { Injector, runInInjectionContext } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { withFocalPoint } from './with-focal-point.feature';

import { imageEditorToolEvents } from '../image-editor.events';
import { initialImageEditorState } from '../image-editor.state';

const FocalPointStore = signalStore(withState(initialImageEditorState), withFocalPoint());

describe('withFocalPoint', () => {
    let store: InstanceType<typeof FocalPointStore>;
    let tools: ReturnType<typeof injectDispatch<typeof imageEditorToolEvents>>;

    beforeEach(() => {
        TestBed.configureTestingModule({ providers: [FocalPointStore, Dispatcher] });
        const injector = TestBed.inject(Injector);
        store = TestBed.inject(FocalPointStore);
        runInInjectionContext(injector, () => {
            tools = injectDispatch(imageEditorToolEvents);
        });
    });

    it('sets the focal point from focalPointSet', () => {
        tools.focalPointSet({ x: 0.3, y: 0.7 });

        expect(store.focalPoint()).toEqual({ x: 0.3, y: 0.7 });
    });

    it('clamps the focal point into 0..1', () => {
        tools.focalPointSet({ x: 1.5, y: -0.2 });

        expect(store.focalPoint()).toEqual({ x: 1, y: 0 });
    });

    it('does not record a history entry (focal point is not a preview edit)', () => {
        tools.focalPointSet({ x: 0.42, y: 0.31 });

        expect(store.history()).toEqual([]);
    });
});
