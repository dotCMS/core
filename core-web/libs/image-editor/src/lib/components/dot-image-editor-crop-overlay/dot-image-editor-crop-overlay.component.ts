import { injectDispatch } from '@ngrx/signals/events';

import {
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    inject,
    input,
    NgZone,
    signal,
    untracked
} from '@angular/core';

import { DotMessagePipe } from '@dotcms/ui';

import { imageEditorOverlayEnterLeave } from '../../animations/image-editor.animations';
import {
    CROP_HANDLES,
    CROP_NUDGE_STEP,
    CROP_NUDGE_STEP_LARGE,
    MIN_CROP_SIZE
} from '../../image-editor.constants';
import { Dimensions, HandlePosition, ImageRect, LocalRect } from '../../models/image-editor.models';
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

    /**
     * Locked aspect ratio (width / height) for the crop box, or `null` for
     * free-form. When a non-null ratio is set the box is reshaped to it (centered
     * and maximized to fit the rendered image) and subsequent handle-resizes keep
     * the ratio; `null` restores unconstrained, Shift-to-lock free-form behavior.
     */
    aspect = input<number | null>(null);

    /**
     * Intrinsic pixel size of the DISPLAYED image — i.e. the source after the
     * rotate/flip/resize currently in effect, not the original asset. Crop boxes are
     * drawn on this displayed image, so its dimensions are the space the box must be
     * converted into; using the original asset size here would mis-scale a crop made
     * on a rotated preview. `0×0` until the image is measured (no conversion).
     */
    naturalSize = input<Dimensions>({ width: 0, height: 0 });

    readonly #store = inject(ImageEditorStore);
    readonly #dispatch = injectDispatch(imageEditorToolEvents);
    readonly #zone = inject(NgZone);

    /** The resize handles rendered around the crop box. */
    protected readonly handles = CROP_HANDLES;

    /** Whether the crop tool is the active canvas tool. */
    protected readonly isActive = computed(() => this.#store.activeTool() === 'crop');

    /** Crop selection in CSS px, local to the rendered image origin. */
    protected readonly cropRect = signal<LocalRect>({ x: 0, y: 0, width: 0, height: 0 });

    /**
     * The current crop box size in natural image pixels, derived by scaling the
     * rendered CSS-px box up to the source resolution. Surfaced so the canvas
     * footer can show (and edit) the crop size as real pixels; `0×0` when there
     * is no rendered image to scale against. Updates live as the box is dragged
     * or resized.
     */
    readonly naturalCropSize = computed<Dimensions>(
        () => {
            const rect = this.imageRect();
            const crop = this.cropRect();

            const { width: naturalWidth, height: naturalHeight } = this.naturalSize();

            if (!rect || rect.width === 0 || rect.height === 0 || !naturalWidth || !naturalHeight) {
                return { width: 0, height: 0 };
            }

            return {
                width: Math.round((crop.width * naturalWidth) / rect.width),
                height: Math.round((crop.height * naturalHeight) / rect.height)
            };
        },
        // Compare by value, not reference: a box MOVE produces a fresh object every
        // frame but with identical width/height, so this stops the (size-only) footer
        // inputs from re-rendering while dragging the box around. Only an actual
        // size change (a resize) notifies and updates the inputs.
        { equal: (a, b) => a.width === b.width && a.height === b.height }
    );

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

    /**
     * Edges of the crop box (CSS px, overlay-local) used to lay out the four dim
     * panels that darken everything outside the selection. We dim with solid panels
     * rather than the box's `box-shadow` because a viewport-sized shadow re-rasterizes
     * the whole dimmed area on every frame — the dominant cost while dragging — whereas
     * four solid fills driven by this signal are cheap. The panels stretch to the
     * overlay edges via `right:0`/`bottom:0`, so only the box edges are needed here.
     */
    protected readonly maskBounds = computed(() => {
        const rect = this.imageRect();
        const crop = this.cropRect();
        const left = (rect?.x ?? 0) + crop.x;
        const top = (rect?.y ?? 0) + crop.y;

        return {
            left,
            top,
            right: left + crop.width,
            bottom: top + crop.height,
            height: crop.height
        };
    });

    constructor() {
        // Seed the selection whenever the tool activates or the rendered bounds
        // change while cropping: to the captured visible region when the user
        // switched to crop while zoomed, otherwise to the full rendered image.
        // Reads `aspect()` untracked so this fires only on activation/bounds
        // changes — the dedicated reshape effect below owns reacting to the ratio.
        effect(() => {
            const rect = this.imageRect();

            if (this.isActive() && rect) {
                const ratio = untracked(this.aspect);
                if (ratio) {
                    this.cropRect.set(this.#aspectFittedRect(ratio, rect));

                    return;
                }

                const initial = this.initialRect();
                this.cropRect.set(
                    initial ?? { x: 0, y: 0, width: rect.width, height: rect.height }
                );
            }
        });

        // Reshape the box whenever the locked ratio changes while cropping: a
        // non-null ratio fits a centered, maximized box of that ratio inside the
        // rendered image. `null` (Free) leaves the current box untouched.
        effect(() => {
            const ratio = this.aspect();
            const rect = untracked(this.imageRect);

            if (untracked(this.isActive) && rect && ratio) {
                this.cropRect.set(this.#aspectFittedRect(ratio, rect));
            }
        });
    }

    /**
     * The largest box of the given aspect ratio (width / height) that fits the
     * rendered image, centered within it — in image-local CSS px.
     */
    #aspectFittedRect(aspect: number, rect: ImageRect): LocalRect {
        const rectAspect = rect.width / rect.height;

        let width: number;
        let height: number;
        if (aspect > rectAspect) {
            width = rect.width;
            height = width / aspect;
        } else {
            height = rect.height;
            width = height * aspect;
        }

        return {
            x: (rect.width - width) / 2,
            y: (rect.height - height) / 2,
            width,
            height
        };
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
        const { width: naturalWidth, height: naturalHeight } = this.naturalSize();

        if (!rect || rect.width === 0 || rect.height === 0 || !naturalWidth || !naturalHeight) {
            return;
        }

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
     * Resizes the crop box to the given size in natural image pixels, keeping it
     * centered on its current center and clamped to the rendered image. Invoked by
     * the canvas footer's width/height inputs so typing a pixel size drives the
     * on-canvas box. When a ratio is locked the natural size is expected to already
     * honor it; the resulting CSS-px box is clamped to the image, which may shrink
     * it proportionally if it would overflow. No-op without a rendered image.
     */
    setNaturalCropSize(width: number, height: number): void {
        const rect = this.imageRect();
        const { width: naturalWidth, height: naturalHeight } = this.naturalSize();

        if (
            !rect ||
            rect.width === 0 ||
            rect.height === 0 ||
            !naturalWidth ||
            !naturalHeight ||
            width <= 0 ||
            height <= 0
        ) {
            return;
        }

        const scaleX = rect.width / naturalWidth;
        const scaleY = rect.height / naturalHeight;

        // Convert to rendered CSS px and clamp to the image, never below the
        // minimum usable size.
        const cssWidth = clamp(width * scaleX, MIN_CROP_SIZE, rect.width);
        const cssHeight = clamp(height * scaleY, MIN_CROP_SIZE, rect.height);

        // Keep the box centered on its current center, then clamp the origin so it
        // stays fully inside the image.
        const current = this.cropRect();
        const centerX = current.x + current.width / 2;
        const centerY = current.y + current.height / 2;

        this.cropRect.set({
            x: clamp(centerX - cssWidth / 2, 0, rect.width - cssWidth),
            y: clamp(centerY - cssHeight / 2, 0, rect.height - cssHeight),
            width: cssWidth,
            height: cssHeight
        });
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
     * Resizes the box from a handle, constrained within the rendered image. The
     * selection is locked to a fixed aspect ratio when one is set via the `aspect`
     * input (a preset is active) or while Shift is held over a corner (the common
     * "shift to keep proportions" behavior). The locked ratio comes from the
     * `aspect` input when present, otherwise from the box's starting proportions;
     * edge handles and unmodified free-form drags resize freely.
     */
    #resize(
        start: LocalRect,
        position: HandlePosition,
        dx: number,
        dy: number,
        shiftKey = false
    ): void {
        const rect = this.imageRect();

        if (!rect) {
            return;
        }

        // Corner handles carry both an horizontal and a vertical edge ('tl', etc.).
        const isCorner = position.length === 2;
        const locked = this.aspect();
        const lockAspect = locked != null || shiftKey;

        if (lockAspect && isCorner && start.height > 0) {
            const ratio = locked ?? start.width / start.height;
            this.cropRect.set(this.#resizeLockedAspect(start, position, dx, dy, rect, ratio));

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
     * Corner resize that preserves a fixed aspect ratio. The corner opposite the
     * dragged handle stays anchored; the dominant pointer axis drives the size and
     * the other dimension follows the locked ratio. When a box edge is reached,
     * both dimensions scale down together so the ratio holds.
     */
    #resizeLockedAspect(
        start: LocalRect,
        position: HandlePosition,
        dx: number,
        dy: number,
        rect: ImageRect,
        aspect: number
    ): LocalRect {
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
     *
     * Performance: high-frequency pointing devices (120–240 Hz trackpads/mice) fire
     * `pointermove` far faster than the screen refreshes, and each move mutates the
     * crop box — which now also re-renders the footer's width/height inputs. To keep
     * dragging smooth we (a) listen OUTSIDE Angular so a raw move never triggers a
     * change-detection tick, and (b) coalesce moves to a single state update per
     * animation frame, re-entering the zone only in that rAF flush.
     */
    #trackPointer(
        start: PointerEvent,
        onMove: (dx: number, dy: number, shiftKey: boolean) => void
    ): void {
        const originX = start.clientX;
        const originY = start.clientY;
        let frame = 0;
        let latest = { dx: 0, dy: 0, shiftKey: start.shiftKey };

        // Apply the latest pointer position inside Angular (so OnPush re-renders the
        // box and the size inputs), at most once per frame.
        const apply = () => {
            frame = 0;
            this.#zone.run(() => onMove(latest.dx, latest.dy, latest.shiftKey));
        };
        const move = (event: PointerEvent) => {
            latest = {
                dx: event.clientX - originX,
                dy: event.clientY - originY,
                shiftKey: event.shiftKey
            };

            if (!frame) {
                frame = requestAnimationFrame(apply);
            }
        };
        const up = () => {
            window.removeEventListener('pointermove', move);
            window.removeEventListener('pointerup', up);

            if (frame) {
                cancelAnimationFrame(frame);
            }
            // Flush the final position synchronously so the box settles exactly
            // where the pointer was released.
            apply();
        };

        this.#zone.runOutsideAngular(() => {
            window.addEventListener('pointermove', move);
            window.addEventListener('pointerup', up);
        });
    }
}
