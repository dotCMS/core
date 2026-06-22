import { injectDispatch } from '@ngrx/signals/events';

import {
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    inject,
    input,
    signal
} from '@angular/core';

import { DotMessagePipe } from '@dotcms/ui';

import { imageEditorOverlayEnterLeave } from '../../animations/image-editor.animations';
import {
    CROP_HANDLES,
    CROP_NUDGE_STEP,
    CROP_NUDGE_STEP_LARGE,
    MIN_CROP_SIZE
} from '../../image-editor.constants';
import { HandlePosition, ImageRect, LocalRect } from '../../models/image-editor.models';
import { imageEditorToolEvents } from '../../store/image-editor.events';
import { ImageEditorStore } from '../../store/image-editor.store';
import { clamp } from '../../utils/dimensions.util';

/**
 * Interactive crop overlay rendered on top of the image canvas while the crop
 * tool is active. Presents a draggable/resizable selection rectangle with a
 * rule-of-thirds grid and eight resize handles. The selection is kept in CSS px
 * (local to the rendered image) and converted to natural image pixels only when
 * the user applies the crop. Keyboard control mirrors the pointer interactions:
 * arrows nudge, Enter applies and Escape cancels.
 */
@Component({
    selector: 'dot-image-editor-crop-overlay',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [DotMessagePipe],
    templateUrl: './dot-image-editor-crop-overlay.component.html',
    styleUrl: './dot-image-editor-crop-overlay.component.scss',
    animations: [imageEditorOverlayEnterLeave()],
    host: { '(keydown.escape)': 'onEscape($event)' }
})
export class DotImageEditorCropOverlayComponent {
    /** Bounds of the rendered image within the canvas, in CSS px. */
    imageRect = input<ImageRect>();

    /**
     * Initial crop selection (image-local CSS px) to seed on activation — set when
     * the user switches to crop while zoomed, so the box frames what was in view.
     * When unset the selection defaults to the full image.
     */
    initialRect = input<ImageRect>();

    readonly #store = inject(ImageEditorStore);
    readonly #dispatch = injectDispatch(imageEditorToolEvents);

    /** The resize handles rendered around the crop box. */
    protected readonly handles = CROP_HANDLES;

    /** Whether the crop tool is the active canvas tool. */
    protected readonly isActive = computed(() => this.#store.activeTool() === 'crop');

    /** Crop selection in CSS px, local to the rendered image origin. */
    protected readonly cropRect = signal<LocalRect>({ x: 0, y: 0, width: 0, height: 0 });

    /** Absolute CSS-px position of the crop box within the canvas. */
    protected readonly boxStyle = computed(() => {
        const rect = this.imageRect();
        const crop = this.cropRect();

        return {
            left: `${(rect?.x ?? 0) + crop.x}px`,
            top: `${(rect?.y ?? 0) + crop.y}px`,
            width: `${crop.width}px`,
            height: `${crop.height}px`
        };
    });

    constructor() {
        // Seed the selection whenever the tool activates or the rendered bounds
        // change while cropping: to the captured visible region when the user
        // switched to crop while zoomed, otherwise to the full rendered image.
        effect(() => {
            const rect = this.imageRect();

            if (this.isActive() && rect) {
                const initial = this.initialRect();
                this.cropRect.set(
                    initial ?? { x: 0, y: 0, width: rect.width, height: rect.height }
                );
            }
        });
    }

    /** Begins a box move from a pointer press, tracking until release. */
    protected onBoxPointerDown(event: PointerEvent): void {
        event.preventDefault();
        const start = this.cropRect();

        this.#trackPointer(event, (dx, dy) => {
            this.#moveTo(start.x + dx, start.y + dy);
        });
    }

    /** Begins a resize from a handle pointer press, tracking until release. */
    protected onHandlePointerDown(event: PointerEvent, position: HandlePosition): void {
        event.preventDefault();
        event.stopPropagation();
        const start = this.cropRect();

        this.#trackPointer(event, (dx, dy, shiftKey) => {
            this.#resize(start, position, dx, dy, shiftKey);
        });
    }

    /** Nudges or applies/cancels the crop in response to keyboard input. */
    protected onBoxKeydown(event: KeyboardEvent): void {
        const step = event.shiftKey ? CROP_NUDGE_STEP_LARGE : CROP_NUDGE_STEP;
        const current = this.cropRect();

        switch (event.key) {
            case 'ArrowLeft':
                event.preventDefault();
                this.#moveTo(current.x - step, current.y);
                break;
            case 'ArrowRight':
                event.preventDefault();
                this.#moveTo(current.x + step, current.y);
                break;
            case 'ArrowUp':
                event.preventDefault();
                this.#moveTo(current.x, current.y - step);
                break;
            case 'ArrowDown':
                event.preventDefault();
                this.#moveTo(current.x, current.y + step);
                break;
            case 'Enter':
                event.preventDefault();
                this.applyCrop();
                break;
            default:
                break;
        }
    }

    /**
     * Applies the current crop selection, converting it from rendered CSS px to
     * natural image pixels before dispatching. Invoked by the canvas footer's
     * "Apply crop" action and by the Enter key while the box is focused.
     */
    applyCrop(): void {
        const rect = this.imageRect();

        if (!rect || rect.width === 0 || rect.height === 0) {
            return;
        }

        const { naturalWidth, naturalHeight } = this.#store.assetContext();
        const crop = this.cropRect();
        const scaleX = naturalWidth / rect.width;
        const scaleY = naturalHeight / rect.height;

        this.#dispatch.cropApplied({
            x: Math.round(crop.x * scaleX),
            y: Math.round(crop.y * scaleY),
            w: Math.round(crop.width * scaleX),
            h: Math.round(crop.height * scaleY),
            active: true,
            aspect: null
        });
    }

    /**
     * Cancels cropping and restores the move tool. Invoked by the canvas footer's
     * "Cancel" action and by the Escape key.
     */
    cancelCrop(): void {
        this.#dispatch.cropCancelled();
    }

    /**
     * Intercepts Escape so the host dialog does not close while cropping; the
     * keypress instead cancels the crop selection.
     */
    protected onEscape(event: KeyboardEvent): void {
        if (!this.isActive()) {
            return;
        }

        event.stopPropagation();
        this.cancelCrop();
    }

    /** Moves the box to a new local origin, clamped within the rendered image. */
    #moveTo(x: number, y: number): void {
        const rect = this.imageRect();

        if (!rect) {
            return;
        }

        const crop = this.cropRect();
        this.cropRect.set({
            ...crop,
            x: clamp(x, 0, rect.width - crop.width),
            y: clamp(y, 0, rect.height - crop.height)
        });
    }

    /**
     * Resizes the box from a handle, constrained within the rendered image.
     * Holding Shift while dragging a corner locks the selection to its starting
     * aspect ratio (the common "shift to keep proportions" behavior); edge handles
     * and unmodified drags remain free-form.
     */
    #resize(
        start: LocalRect,
        position: HandlePosition,
        dx: number,
        dy: number,
        lockAspect = false
    ): void {
        const rect = this.imageRect();

        if (!rect) {
            return;
        }

        // Corner handles carry both an horizontal and a vertical edge ('tl', etc.).
        const isCorner = position.length === 2;

        if (lockAspect && isCorner && start.height > 0) {
            this.cropRect.set(this.#resizeLockedAspect(start, position, dx, dy, rect));

            return;
        }

        let { x, y, width, height } = start;
        const right = start.x + start.width;
        const bottom = start.y + start.height;

        if (position.includes('l')) {
            x = clamp(start.x + dx, 0, right - MIN_CROP_SIZE);
            width = right - x;
        }

        if (position.includes('r')) {
            width = clamp(start.width + dx, MIN_CROP_SIZE, rect.width - start.x);
        }

        if (position.includes('t')) {
            y = clamp(start.y + dy, 0, bottom - MIN_CROP_SIZE);
            height = bottom - y;
        }

        if (position.includes('b')) {
            height = clamp(start.height + dy, MIN_CROP_SIZE, rect.height - start.y);
        }

        this.cropRect.set({ x, y, width, height });
    }

    /**
     * Corner resize that preserves the selection's starting aspect ratio. The
     * corner opposite the dragged handle stays anchored; the dominant pointer axis
     * drives the size and the other dimension follows the locked ratio. When a box
     * edge is reached, both dimensions scale down together so the ratio holds.
     */
    #resizeLockedAspect(
        start: LocalRect,
        position: HandlePosition,
        dx: number,
        dy: number,
        rect: ImageRect
    ): LocalRect {
        const aspect = start.width / start.height;

        // The dragged handle grows the box left/up (sign -1) or right/down (+1)
        // from the opposite, anchored corner.
        const growLeft = position.includes('l');
        const growUp = position.includes('t');
        const anchorX = growLeft ? start.x + start.width : start.x;
        const anchorY = growUp ? start.y + start.height : start.y;

        // Free-form proposed size from the pointer delta.
        const proposedWidth = Math.max(MIN_CROP_SIZE, start.width + (growLeft ? -dx : dx));
        const proposedHeight = Math.max(MIN_CROP_SIZE, start.height + (growUp ? -dy : dy));

        // Drive by whichever axis moved more, relative to the starting box.
        let width: number;
        let height: number;
        if (proposedWidth / start.width >= proposedHeight / start.height) {
            width = proposedWidth;
            height = width / aspect;
        } else {
            height = proposedHeight;
            width = height * aspect;
        }

        // Keep the box inside the image from the anchor, preserving the ratio.
        const maxWidth = growLeft ? anchorX : rect.width - anchorX;
        const maxHeight = growUp ? anchorY : rect.height - anchorY;

        if (width > maxWidth) {
            width = maxWidth;
            height = width / aspect;
        }

        if (height > maxHeight) {
            height = maxHeight;
            width = height * aspect;
        }

        return {
            x: growLeft ? anchorX - width : anchorX,
            y: growUp ? anchorY - height : anchorY,
            width,
            height
        };
    }

    /**
     * Tracks pointer movement until release, reporting the delta and the live
     * Shift-key state to `onMove` (Shift toggles aspect-ratio locking mid-drag).
     */
    #trackPointer(
        start: PointerEvent,
        onMove: (dx: number, dy: number, shiftKey: boolean) => void
    ): void {
        const originX = start.clientX;
        const originY = start.clientY;

        const move = (event: PointerEvent) =>
            onMove(event.clientX - originX, event.clientY - originY, event.shiftKey);
        const up = () => {
            window.removeEventListener('pointermove', move);
            window.removeEventListener('pointerup', up);
        };

        window.addEventListener('pointermove', move);
        window.addEventListener('pointerup', up);
    }
}
