import { injectDispatch } from '@ngrx/signals/events';

import {
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    HostListener,
    inject,
    input,
    signal
} from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotMessagePipe } from '@dotcms/ui';

import { imageEditorOverlayEnterLeave } from '../../animations/image-editor.animations';
import { imageEditorToolEvents } from '../../store/image-editor.events';
import { ImageEditorStore } from '../../store/image-editor.store';
import { clamp } from '../../utils/dimensions.util';

/** Axis-aligned rectangle of the rendered image inside the canvas, in CSS px. */
export interface ImageRect {
    x: number;
    y: number;
    width: number;
    height: number;
}

/** A crop rectangle expressed in CSS px, local to the rendered image origin. */
interface LocalRect {
    x: number;
    y: number;
    width: number;
    height: number;
}

/** Identifiers for the eight resize handles around the crop box. */
type HandlePosition = 'tl' | 't' | 'tr' | 'r' | 'br' | 'b' | 'bl' | 'l';

/** Distance in CSS px nudged per arrow keypress; Shift multiplies this. */
const NUDGE_STEP = 1;
const NUDGE_STEP_LARGE = 10;

/** Smallest allowed crop dimension in CSS px to keep the box usable. */
const MIN_CROP_SIZE = 16;

/** The eight resize handles rendered around the crop box. */
const HANDLES: readonly HandlePosition[] = ['tl', 't', 'tr', 'r', 'br', 'b', 'bl', 'l'] as const;

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
    imports: [ButtonModule, DotMessagePipe],
    templateUrl: './dot-image-editor-crop-overlay.component.html',
    styleUrl: './dot-image-editor-crop-overlay.component.scss',
    animations: [imageEditorOverlayEnterLeave()]
})
export class DotImageEditorCropOverlayComponent {
    /** Bounds of the rendered image within the canvas, in CSS px. */
    imageRect = input<ImageRect>();

    readonly #store = inject(ImageEditorStore);
    readonly #dispatch = injectDispatch(imageEditorToolEvents);

    /** The resize handles rendered around the crop box. */
    protected readonly handles = HANDLES;

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
        // Seed the selection to the full rendered image whenever the tool
        // activates or the rendered bounds change while cropping.
        effect(() => {
            const rect = this.imageRect();

            if (this.isActive() && rect) {
                this.cropRect.set({ x: 0, y: 0, width: rect.width, height: rect.height });
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

        this.#trackPointer(event, (dx, dy) => {
            this.#resize(start, position, dx, dy);
        });
    }

    /** Nudges or applies/cancels the crop in response to keyboard input. */
    protected onBoxKeydown(event: KeyboardEvent): void {
        const step = event.shiftKey ? NUDGE_STEP_LARGE : NUDGE_STEP;
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
                this.apply();
                break;
            default:
                break;
        }
    }

    /** Applies the crop by converting the selection to natural image pixels. */
    protected apply(): void {
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

    /** Cancels cropping and restores the move tool. */
    protected cancel(): void {
        this.#dispatch.cropCancelled();
    }

    /**
     * Intercepts Escape so the host dialog does not close while cropping; the
     * keypress instead cancels the crop selection.
     */
    @HostListener('keydown.escape', ['$event'])
    protected onEscape(event: KeyboardEvent): void {
        if (!this.isActive()) {
            return;
        }

        event.stopPropagation();
        this.cancel();
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

    /** Resizes the box from a handle, constrained within the rendered image. */
    #resize(start: LocalRect, position: HandlePosition, dx: number, dy: number): void {
        const rect = this.imageRect();

        if (!rect) {
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

    /** Tracks pointer movement until release, reporting the delta to `onMove`. */
    #trackPointer(start: PointerEvent, onMove: (dx: number, dy: number) => void): void {
        const originX = start.clientX;
        const originY = start.clientY;

        const move = (event: PointerEvent) =>
            onMove(event.clientX - originX, event.clientY - originY);
        const up = () => {
            window.removeEventListener('pointermove', move);
            window.removeEventListener('pointerup', up);
        };

        window.addEventListener('pointermove', move);
        window.addEventListener('pointerup', up);
    }
}
