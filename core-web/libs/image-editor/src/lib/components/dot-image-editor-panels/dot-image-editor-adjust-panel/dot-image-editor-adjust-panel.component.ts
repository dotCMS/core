import { injectDispatch } from '@ngrx/signals/events';

import { ChangeDetectionStrategy, Component, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { CheckboxChangeEvent, CheckboxModule } from 'primeng/checkbox';
import { SliderChangeEvent, SliderModule, SliderSlideEndEvent } from 'primeng/slider';

import { DotMessagePipe } from '@dotcms/ui';

import { imageEditorPanelEvents } from '../../../store/image-editor.events';
import { ImageEditorStore } from '../../../store/image-editor.store';

/**
 * Color & light adjustment panel. Binds brightness, hue and saturation sliders
 * plus a grayscale checkbox to the {@link ImageEditorStore} `adjust` slice and
 * dispatches the matching {@link imageEditorPanelEvents} on user input. Sliders
 * update their displayed value optimistically on `onChange` and dispatch the
 * committed value on `onSlideEnd`, letting the store own debouncing of the
 * resulting preview.
 */
@Component({
    selector: 'dot-image-editor-adjust-panel',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [FormsModule, SliderModule, CheckboxModule, DotMessagePipe],
    templateUrl: './dot-image-editor-adjust-panel.component.html',
    styleUrl: './dot-image-editor-adjust-panel.component.scss'
})
export class DotImageEditorAdjustPanelComponent {
    /** Image editor state store, provided by the owning dialog component. */
    protected readonly store = inject(ImageEditorStore);

    /** Panel event dispatcher for color adjustment changes. */
    protected readonly dispatch = injectDispatch(imageEditorPanelEvents);

    /** Optimistic brightness value shown while the slider is being dragged. */
    protected readonly brightness = signal(0);
    /** Optimistic hue value shown while the slider is being dragged. */
    protected readonly hue = signal(0);
    /** Optimistic saturation value shown while the slider is being dragged. */
    protected readonly saturation = signal(0);

    constructor() {
        // Keep the optimistic slider values in sync with committed store state
        // (e.g. after undo/redo, reset or removing a history entry).
        effect(() => {
            const adjust = this.store.adjust();
            this.brightness.set(adjust.brightness);
            this.hue.set(adjust.hue);
            this.saturation.set(adjust.saturation);
        });
    }

    /** Updates the optimistic brightness label as the slider moves. */
    protected onBrightnessChange(event: SliderChangeEvent): void {
        this.brightness.set(this.singleValue(event.value));
    }

    /** Dispatches the final brightness value once the slider drag ends. */
    protected onBrightnessSlideEnd(event: SliderSlideEndEvent): void {
        this.dispatch.brightnessChanged(event.value ?? 0);
    }

    /** Updates the optimistic hue label as the slider moves. */
    protected onHueChange(event: SliderChangeEvent): void {
        this.hue.set(this.singleValue(event.value));
    }

    /** Dispatches the final hue value once the slider drag ends. */
    protected onHueSlideEnd(event: SliderSlideEndEvent): void {
        this.dispatch.hueChanged(event.value ?? 0);
    }

    /** Updates the optimistic saturation label as the slider moves. */
    protected onSaturationChange(event: SliderChangeEvent): void {
        this.saturation.set(this.singleValue(event.value));
    }

    /** Dispatches the final saturation value once the slider drag ends. */
    protected onSaturationSlideEnd(event: SliderSlideEndEvent): void {
        this.dispatch.saturationChanged(event.value ?? 0);
    }

    /** Dispatches the grayscale toggle state. */
    protected onGrayscaleToggle(event: CheckboxChangeEvent): void {
        this.dispatch.grayscaleToggled(Boolean(event.checked));
    }

    /** Narrows the slider's number-or-range value to a single number. */
    private singleValue(value: SliderChangeEvent['value']): number {
        return Array.isArray(value) ? (value[0] ?? 0) : (value ?? 0);
    }
}
