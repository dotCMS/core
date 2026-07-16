import { Dispatcher } from '@ngrx/signals/events';
import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';

import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { DotMessageService } from '@dotcms/data-access';

import { DotImageEditorAdjustPanelComponent } from './dot-image-editor-adjust-panel.component';

import { ImageEditorStore } from '../../../store/image-editor.store';

const ADJUST = { brightness: 0, hue: 0, saturation: 0, grayscale: false };

describe('DotImageEditorAdjustPanelComponent', () => {
    let spectator: Spectator<DotImageEditorAdjustPanelComponent>;
    let dispatcher: Dispatcher;

    const createComponent = createComponentFactory({
        component: DotImageEditorAdjustPanelComponent,
        providers: [
            provideNoopAnimations(),
            Dispatcher,
            mockProvider(DotMessageService, { get: jest.fn((key: string) => key) })
        ],
        componentProviders: [
            mockProvider(ImageEditorStore, {
                adjust: () => ADJUST
            })
        ]
    });

    beforeEach(() => {
        spectator = createComponent({ detectChanges: false });
        dispatcher = spectator.inject(Dispatcher, true);
        jest.spyOn(dispatcher, 'dispatch');
        spectator.detectChanges();
    });

    it('should render the brightness, hue, saturation sliders and grayscale checkbox', () => {
        expect(spectator.query(byTestId('image-editor-brightness-slider'))).toExist();
        expect(spectator.query(byTestId('image-editor-hue-slider'))).toExist();
        expect(spectator.query(byTestId('image-editor-saturation-slider'))).toExist();
        expect(spectator.query(byTestId('image-editor-grayscale-checkbox'))).toExist();
    });

    it('should dispatch brightnessChanged with the committed value on slide end', () => {
        spectator.triggerEventHandler(
            '[data-testid="image-editor-brightness-slider"]',
            'onSlideEnd',
            {
                originalEvent: new MouseEvent('mouseup'),
                value: 42
            }
        );

        const event = dispatchedEvent('brightnessChanged');
        expect(event).toBeDefined();
        expect(event!.payload).toBe(42);
    });

    it('should dispatch hueChanged with the committed value on slide end', () => {
        spectator.triggerEventHandler('[data-testid="image-editor-hue-slider"]', 'onSlideEnd', {
            originalEvent: new MouseEvent('mouseup'),
            value: -30
        });

        const event = dispatchedEvent('hueChanged');
        expect(event).toBeDefined();
        expect(event!.payload).toBe(-30);
    });

    it('should dispatch saturationChanged with the committed value on slide end', () => {
        spectator.triggerEventHandler(
            '[data-testid="image-editor-saturation-slider"]',
            'onSlideEnd',
            {
                originalEvent: new MouseEvent('mouseup'),
                value: 75
            }
        );

        const event = dispatchedEvent('saturationChanged');
        expect(event).toBeDefined();
        expect(event!.payload).toBe(75);
    });

    it('should dispatch brightnessChanged with the value typed into the number field', () => {
        const input = spectator.query<HTMLInputElement>(byTestId('image-editor-brightness-value'))!;
        input.value = '55';
        spectator.dispatchFakeEvent(input, 'change');

        const event = dispatchedEvent('brightnessChanged');
        expect(event).toBeDefined();
        expect(event!.payload).toBe(55);
    });

    it('should clamp a typed value to the slider range', () => {
        const input = spectator.query<HTMLInputElement>(byTestId('image-editor-hue-value'))!;
        input.value = '150';
        spectator.dispatchFakeEvent(input, 'change');

        const event = dispatchedEvent('hueChanged');
        expect(event).toBeDefined();
        expect(event!.payload).toBe(100);
        // The field is corrected to the clamped value.
        expect(input.value).toBe('100');
    });

    it('should dispatch grayscaleToggled when the checkbox changes', () => {
        spectator.triggerEventHandler(
            '[data-testid="image-editor-grayscale-checkbox"]',
            'onChange',
            {
                originalEvent: new Event('change'),
                checked: true
            }
        );

        const event = dispatchedEvent('grayscaleToggled');
        expect(event).toBeDefined();
        expect(event!.payload).toBe(true);
    });

    /** Finds the first dispatched event whose type matches the given suffix. */
    function dispatchedEvent(typeSuffix: string): { type: string; payload?: unknown } | undefined {
        const call = (dispatcher.dispatch as jest.Mock).mock.calls.find(([dispatched]) =>
            dispatched.type.includes(typeSuffix)
        );

        return call?.[0];
    }
});
