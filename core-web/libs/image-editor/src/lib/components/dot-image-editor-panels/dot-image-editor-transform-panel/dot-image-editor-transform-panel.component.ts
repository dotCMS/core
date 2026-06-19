import { injectDispatch } from '@ngrx/signals/events';

import { ChangeDetectionStrategy, Component, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { InputNumberModule } from 'primeng/inputnumber';
import { SliderChangeEvent, SliderModule, SliderSlideEndEvent } from 'primeng/slider';
import { ToggleButtonModule } from 'primeng/togglebutton';

import { DotMessagePipe } from '@dotcms/ui';

import { imageEditorTransformEvents } from '../../../store/image-editor.events';
import { ImageEditorStore } from '../../../store/image-editor.store';
import { clamp } from '../../../utils/dimensions.util';

/** Inclusive range of the scale (%) control, matching its slider. */
const SCALE_MIN = 1;
const SCALE_MAX = 400;
/** Inclusive range of the rotate (°) control, matching its slider. */
const ROTATE_MIN = -180;
const ROTATE_MAX = 180;

/**
 * Geometric transform panel. Binds scale and rotate sliders, horizontal/vertical
 * flip toggles and explicit output dimension inputs to the {@link ImageEditorStore}
 * `transform`/`outputDimensions` slices, dispatching the matching
 * {@link imageEditorTransformEvents} on user input. Sliders dispatch their committed
 * value on `onSlideEnd`; the scale and rotate readouts double as inline number
 * fields that commit (clamped) on input — the same pattern as the Adjust panel.
 */
@Component({
    selector: 'dot-image-editor-transform-panel',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [FormsModule, SliderModule, ToggleButtonModule, InputNumberModule, DotMessagePipe],
    templateUrl: './dot-image-editor-transform-panel.component.html',
    styleUrl: './dot-image-editor-transform-panel.component.scss'
})
export class DotImageEditorTransformPanelComponent {
    /** Image editor state store, provided by the owning dialog component. */
    protected readonly store = inject(ImageEditorStore);

    /** Panel event dispatcher for transform changes. */
    protected readonly dispatch = injectDispatch(imageEditorTransformEvents);

    /** Optimistic scale (%) shown in the field while the slider is dragged. */
    protected readonly scale = signal(100);
    /** Optimistic rotation (°) shown in the field while the slider is dragged. */
    protected readonly rotate = signal(0);

    /** Whether the last edited width is below the allowed minimum, for the inline error. */
    protected readonly widthError = signal(false);
    /** Whether the last edited height is below the allowed minimum, for the inline error. */
    protected readonly heightError = signal(false);

    constructor() {
        // Keep the optimistic field values in sync with committed store state
        // (e.g. after undo/redo, reset or removing a history entry).
        effect(() => {
            const transform = this.store.transform();
            this.scale.set(transform.scale);
            this.rotate.set(transform.rotateDeg);
        });
    }

    /** Updates the optimistic scale field as the slider moves. */
    protected onScaleChange(event: SliderChangeEvent): void {
        this.scale.set(this.singleValue(event.value));
    }

    /** Dispatches the final scale value once the slider drag ends. */
    protected scaleChanged(event: SliderSlideEndEvent): void {
        this.dispatch.scaleChanged(event.value ?? 100);
    }

    /** Commits a scale value typed into the inline number field. */
    protected onScaleInput(event: Event): void {
        const value = this.commitTypedValue(event, this.scale(), SCALE_MIN, SCALE_MAX);
        this.scale.set(value);
        this.dispatch.scaleChanged(value);
    }

    /** Updates the optimistic rotation field as the slider moves. */
    protected onRotateChange(event: SliderChangeEvent): void {
        this.rotate.set(this.singleValue(event.value));
    }

    /** Dispatches the final rotation value once the slider drag ends. */
    protected rotateChanged(event: SliderSlideEndEvent): void {
        this.dispatch.rotateChanged(event.value ?? 0);
    }

    /** Commits a rotation value typed into the inline number field. */
    protected onRotateInput(event: Event): void {
        const value = this.commitTypedValue(event, this.rotate(), ROTATE_MIN, ROTATE_MAX);
        this.rotate.set(value);
        this.dispatch.rotateChanged(value);
    }

    /** Dispatches a horizontal flip toggle. */
    protected flipHToggled(): void {
        this.dispatch.flipHToggled();
    }

    /** Dispatches a vertical flip toggle. */
    protected flipVToggled(): void {
        this.dispatch.flipVToggled();
    }

    /** Dispatches a change to the explicit output width, guarding the minimum. */
    protected outputWidthChanged(value: number | null): void {
        this.widthError.set(this.isBelowMinimum(value));
        this.dispatch.outputDimsChanged({
            width: this.toDimension(value),
            height: this.store.transform().outputHeight
        });
    }

    /** Dispatches a change to the explicit output height, guarding the minimum. */
    protected outputHeightChanged(value: number | null): void {
        this.heightError.set(this.isBelowMinimum(value));
        this.dispatch.outputDimsChanged({
            width: this.store.transform().outputWidth,
            height: this.toDimension(value)
        });
    }

    /** Normalizes a dimension input to a positive integer, or `null` when cleared/invalid. */
    private toDimension(value: number | null): number | null {
        return value != null && value >= 1 ? value : null;
    }

    /** Whether a non-empty dimension is below the allowed minimum of 1. */
    private isBelowMinimum(value: number | null): boolean {
        return value != null && value < 1;
    }

    /** Narrows the slider's number-or-range value to a single number. */
    private singleValue(value: SliderChangeEvent['value']): number {
        return Array.isArray(value) ? (value[0] ?? 0) : (value ?? 0);
    }

    /**
     * Parses a value typed into an inline number field: reverts to `fallback`
     * when empty/invalid, rounds, and clamps to [min, max]. Writes the resolved
     * value back to the field so an out-of-range entry shows corrected.
     */
    private commitTypedValue(event: Event, fallback: number, min: number, max: number): number {
        const input = event.target as HTMLInputElement;
        const raw = input.valueAsNumber;
        const value = clamp(Math.round(Number.isFinite(raw) ? raw : fallback), min, max);
        input.value = String(value);

        return value;
    }
}
