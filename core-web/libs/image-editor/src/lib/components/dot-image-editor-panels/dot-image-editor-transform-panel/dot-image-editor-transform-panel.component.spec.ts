import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { Dispatcher } from '@ngrx/signals/events';

import { signal } from '@angular/core';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { Slider } from 'primeng/slider';

import { DotMessageService } from '@dotcms/data-access';

import { DotImageEditorTransformPanelComponent } from './dot-image-editor-transform-panel.component';

import { TransformState } from '../../../models/image-editor.models';
import { imageEditorPanelEvents } from '../../../store/image-editor.events';
import { ImageEditorStore } from '../../../store/image-editor.store';

const TRANSFORM: TransformState = {
    scale: 100,
    rotateDeg: 0,
    flipH: false,
    flipV: false,
    outputWidth: null,
    outputHeight: null,
    lockAspectRatio: true
};

describe('DotImageEditorTransformPanelComponent', () => {
    let spectator: Spectator<DotImageEditorTransformPanelComponent>;
    let dispatcher: Dispatcher;

    const transform = signal<TransformState>(TRANSFORM);
    const outputDimensions = signal<{ width: number; height: number }>({ width: 800, height: 600 });

    const createComponent = createComponentFactory({
        component: DotImageEditorTransformPanelComponent,
        providers: [
            provideNoopAnimations(),
            mockProvider(DotMessageService, { get: jest.fn((key: string) => key) })
        ],
        componentProviders: [
            Dispatcher,
            mockProvider(ImageEditorStore, { transform, outputDimensions })
        ]
    });

    beforeEach(() => {
        transform.set(TRANSFORM);
        outputDimensions.set({ width: 800, height: 600 });
        spectator = createComponent();
        dispatcher = spectator.inject(Dispatcher, true);
        jest.spyOn(dispatcher, 'dispatch');
    });

    it('should dispatch scaleChanged on the scale slider slide end', () => {
        const slider = sliderAt('image-editor-scale-slider');
        slider.onSlideEnd.emit({ originalEvent: new Event('mouseup'), value: 250 });

        const event = dispatchedEvent(imageEditorPanelEvents.scaleChanged.type);
        expect(event).toBeDefined();
        expect(event!.payload).toBe(250);
    });

    it('should dispatch rotateChanged on the rotate slider slide end', () => {
        const slider = sliderAt('image-editor-rotate-slider');
        slider.onSlideEnd.emit({ originalEvent: new Event('mouseup'), value: -90 });

        const event = dispatchedEvent(imageEditorPanelEvents.rotateChanged.type);
        expect(event).toBeDefined();
        expect(event!.payload).toBe(-90);
    });

    it('should dispatch flipHToggled when toggling horizontal flip', () => {
        spectator.triggerEventHandler(byTestId('image-editor-flip-horizontal-btn'), 'onChange', {
            checked: true,
            originalEvent: new Event('click')
        });

        expect(dispatchedEvent(imageEditorPanelEvents.flipHToggled.type)).toBeDefined();
    });

    it('should dispatch flipVToggled when toggling vertical flip', () => {
        spectator.triggerEventHandler(byTestId('image-editor-flip-vertical-btn'), 'onChange', {
            checked: true,
            originalEvent: new Event('click')
        });

        expect(dispatchedEvent(imageEditorPanelEvents.flipVToggled.type)).toBeDefined();
    });

    it('should render the width input and dispatch outputDimsChanged with the new width', () => {
        expect(spectator.query(byTestId('image-editor-output-width-input'))).toExist();
        spectator.component['outputWidthChanged'](1024);

        const event = dispatchedEvent(imageEditorPanelEvents.outputDimsChanged.type);
        expect(event).toBeDefined();
        expect(event!.payload).toEqual({ width: 1024, height: null });
    });

    it('should dispatch outputDimsChanged with the new height on height input', () => {
        spectator.component['outputHeightChanged'](768);

        const event = dispatchedEvent(imageEditorPanelEvents.outputDimsChanged.type);
        expect(event).toBeDefined();
        expect(event!.payload).toEqual({ width: null, height: 768 });
    });

    it('should show the min error and dispatch a null dimension when width is below 1', () => {
        spectator.component['outputWidthChanged'](0);
        spectator.detectChanges();

        expect(spectator.query(byTestId('image-editor-output-width-error'))).toExist();
        expect(dispatchedEvent(imageEditorPanelEvents.outputDimsChanged.type)!.payload).toEqual({
            width: null,
            height: null
        });
    });

    /** Resolves the PrimeNG Slider instance rendered under the given test id. */
    function sliderAt(testId: string): Slider {
        const debugEl = spectator.fixture.debugElement.query(
            (el) => el.nativeElement.getAttribute?.('data-testid') === testId
        );

        return debugEl.componentInstance as Slider;
    }

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
