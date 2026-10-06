import { injectDispatch } from '@ngrx/signals/events';

import { ChangeDetectionStrategy, Component, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { CheckboxChangeEvent, CheckboxModule } from 'primeng/checkbox';
import { SliderChangeEvent, SliderModule, SliderSlideEndEvent } from 'primeng/slider';

import { DotMessagePipe } from '@dotcms/ui';

import { RANGES } from '../../../image-editor.constants';
import { imageEditorAdjustEvents } from '../../../store/image-editor.events';
import { ImageEditorStore } from '../../../store/image-editor.store';
import { clamp } from '../../../utils/dimensions.util';

// Brightness, hue and saturation share the same inclusive range, so the inline
// number fields all clamp to RANGES.brightness.

/**
 * Color & light adjustment panel. Binds brightness, hue and saturation sliders
 * plus a grayscale checkbox to the {@link ImageEditorStore} `adjust` slice and
 * dispatches the matching {@link imageEditorAdjustEvents} on user input. Sliders
 * update their displayed value optimistically on `onChange` and dispatch the
 * committed value on `onSlideEnd`, letting the store own debouncing of the
 * resulting preview. Each value also doubles as an inline number field: typing a
 * value commits it (clamped to the slider range) just like releasing the slider.
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
    protected readonly dispatch = injectDispatch(imageEditorAdjustEvents);

    /** Optimistic brightness value shown while the slider is being dragged. */
    protected readonly $brightness = signal(0);
    /** Optimistic hue value shown while the slider is being dragged. */
    protected readonly $hue = signal(0);
    /** Optimistic saturation value shown while the slider is being dragged. */
    protected readonly $saturation = signal(0);

    constructor() {
        // Keep the optimistic slider values in sync with committed store state
        // (e.g. after undo/redo, reset or removing a history entry).
        effect(() => {
            const adjust = this.store.adjust();
            this.$brightness.set(adjust.brightness);
            this.$hue.set(adjust.hue);
            this.$saturation.set(adjust.saturation);
        });
    }

    /** Updates the optimistic brightness label as the slider moves. */
    protected onBrightnessChange(event: SliderChangeEvent): void {
        this.$brightness.set(this.#singleValue(event.value));
    }

    /** Dispatches the final brightness value once the slider drag ends. */
    protected onBrightnessSlideEnd(event: SliderSlideEndEvent): void {
        this.dispatch.brightnessChanged(event.value ?? 0);
    }

    /** Commits a brightness value typed into the inline number field. */
    protected onBrightnessInput(event: Event): void {
        const value = this.#commitTypedValue(event, this.$brightness());
        this.$brightness.set(value);
        this.dispatch.brightnessChanged(value);
    }

    /** Updates the optimistic hue label as the slider moves. */
    protected onHueChange(event: SliderChangeEvent): void {
        this.$hue.set(this.#singleValue(event.value));
    }

    /** Dispatches the final hue value once the slider drag ends. */
    protected onHueSlideEnd(event: SliderSlideEndEvent): void {
        this.dispatch.hueChanged(event.value ?? 0);
    }

    /** Commits a hue value typed into the inline number field. */
    protected onHueInput(event: Event): void {
        const value = this.#commitTypedValue(event, this.$hue());
        this.$hue.set(value);
        this.dispatch.hueChanged(value);
    }

    /** Updates the optimistic saturation label as the slider moves. */
    protected onSaturationChange(event: SliderChangeEvent): void {
        this.$saturation.set(this.#singleValue(event.value));
    }

    /** Dispatches the final saturation value once the slider drag ends. */
    protected onSaturationSlideEnd(event: SliderSlideEndEvent): void {
        this.dispatch.saturationChanged(event.value ?? 0);
    }

    /** Commits a saturation value typed into the inline number field. */
    protected onSaturationInput(event: Event): void {
        const value = this.#commitTypedValue(event, this.$saturation());
        this.$saturation.set(value);
        this.dispatch.saturationChanged(value);
    }

    /** Dispatches the grayscale toggle state. */
    protected onGrayscaleToggle(event: CheckboxChangeEvent): void {
        this.dispatch.grayscaleToggled(Boolean(event.checked));
    }

    /** Narrows the slider's number-or-range value to a single number. */
    #singleValue(value: SliderChangeEvent['value']): number {
        return Array.isArray(value) ? (value[0] ?? 0) : (value ?? 0);
    }

    /**
     * Parses a value typed into an inline number field: reverts to `fallback`
     * when empty/invalid, rounds, and clamps to the slider range. Writes the
     * resolved value back to the field so an out-of-range entry shows corrected.
     */
    #commitTypedValue(event: Event, fallback: number): number {
        const input = event.target as HTMLInputElement;
        const raw = input.valueAsNumber;
        const value = clamp(
            Math.round(Number.isFinite(raw) ? raw : fallback),
            RANGES.brightness.min,
            RANGES.brightness.max
        );
        input.value = String(value);

        return value;
    }
}
