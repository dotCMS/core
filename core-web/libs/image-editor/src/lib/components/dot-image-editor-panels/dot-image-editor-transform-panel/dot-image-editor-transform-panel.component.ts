import { injectDispatch } from '@ngrx/signals/events';

import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { InputNumberModule } from 'primeng/inputnumber';
import { SliderModule, SliderSlideEndEvent } from 'primeng/slider';
import { ToggleButtonModule } from 'primeng/togglebutton';

import { DotMessagePipe } from '@dotcms/ui';

import { imageEditorPanelEvents } from '../../../store/image-editor.events';
import { ImageEditorStore } from '../../../store/image-editor.store';

/**
 * Geometric transform panel. Binds scale and rotate sliders, horizontal/vertical
 * flip toggles and explicit output dimension inputs to the {@link ImageEditorStore}
 * `transform`/`outputDimensions` slices, dispatching the matching
 * {@link imageEditorPanelEvents} on user input. Sliders dispatch their committed
 * value on `onSlideEnd`, letting the store own debouncing of the resulting preview.
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
    protected readonly dispatch = injectDispatch(imageEditorPanelEvents);

    /** Whether the last edited width is below the allowed minimum, for the inline error. */
    protected readonly widthError = signal(false);
    /** Whether the last edited height is below the allowed minimum, for the inline error. */
    protected readonly heightError = signal(false);

    /** Dispatches the final scale value once the slider drag ends. */
    protected scaleChanged(event: SliderSlideEndEvent): void {
        this.dispatch.scaleChanged(event.value ?? 100);
    }

    /** Dispatches the final rotation value once the slider drag ends. */
    protected rotateChanged(event: SliderSlideEndEvent): void {
        this.dispatch.rotateChanged(event.value ?? 0);
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
}
