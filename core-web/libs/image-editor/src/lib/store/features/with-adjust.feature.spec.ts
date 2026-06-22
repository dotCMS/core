import { signalStore, withState } from '@ngrx/signals';
import { Dispatcher, injectDispatch } from '@ngrx/signals/events';

import { Injector, runInInjectionContext } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { withAdjust } from './with-adjust.feature';

import { imageEditorAdjustEvents } from '../image-editor.events';
import { initialImageEditorState } from '../image-editor.state';

const AdjustStore = signalStore(withState(initialImageEditorState), withAdjust());

describe('withAdjust', () => {
    let store: InstanceType<typeof AdjustStore>;
    let adjust: ReturnType<typeof injectDispatch<typeof imageEditorAdjustEvents>>;

    beforeEach(() => {
        TestBed.configureTestingModule({ providers: [AdjustStore, Dispatcher] });
        const injector = TestBed.inject(Injector);
        store = TestBed.inject(AdjustStore);
        runInInjectionContext(injector, () => {
            adjust = injectDispatch(imageEditorAdjustEvents);
        });
    });

    it('clamps brightness to the max and records a history entry', () => {
        adjust.brightnessChanged(150);

        expect(store.adjust().brightness).toBe(100);
        expect(store.history()[0].category).toBe('adjust');
        expect(store.history()[0].label).toBe('Brightness 100');
        expect(store.previewStatus()).toBe('loading');
        expect(store.cacheBust()).toBe(1);
    });

    it('clamps brightness to the min', () => {
        adjust.brightnessChanged(-150);
        expect(store.adjust().brightness).toBe(-100);
    });

    it('sets and clamps hue', () => {
        adjust.hueChanged(50);
        expect(store.adjust().hue).toBe(50);
        expect(store.history()[0].label).toBe('Hue 50');

        adjust.hueChanged(999);
        expect(store.adjust().hue).toBe(100);
    });

    it('sets and clamps saturation', () => {
        adjust.saturationChanged(-30);
        expect(store.adjust().saturation).toBe(-30);
        expect(store.history()[0].label).toBe('Saturation -30');
    });

    it('toggles grayscale on under the grayscale category', () => {
        adjust.grayscaleToggled(true);

        expect(store.adjust().grayscale).toBe(true);
        expect(store.history()[0].category).toBe('grayscale');
        expect(store.history()[0].label).toBe('Grayscale on');
    });

    it('labels grayscale off when toggled false', () => {
        adjust.grayscaleToggled(false);

        expect(store.adjust().grayscale).toBe(false);
        expect(store.history()[0].label).toBe('Grayscale off');
    });
});
