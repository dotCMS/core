import { injectDispatch } from '@ngrx/signals/events';

import {
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    ElementRef,
    inject,
    input,
    signal
} from '@angular/core';

import { DotMessagePipe } from '@dotcms/ui';

import { focalPointPop } from '../../animations/image-editor.animations';
import { ImageRect } from '../../models/image-editor.models';
import { imageEditorToolEvents } from '../../store/image-editor.events';
import { ImageEditorStore } from '../../store/image-editor.store';
import { clamp } from '../../utils/dimensions.util';

/** A normalized point in the unit square, where {x:0.5, y:0.5} is the center. */
interface NormalizedPoint {
    x: number;
    y: number;
}

/** Fraction of the image moved per arrow keypress; Shift uses the larger step. */
const NUDGE_STEP = 0.01;
const NUDGE_STEP_LARGE = 0.05;

/**
 * Focal point overlay rendered on top of the image canvas while the focal tool
 * is active. Presents a draggable circular target marker positioned from the
 * normalized focal point. Clicking or dragging sets a local normalized point
 * (kept in 0..1) which is dispatched only when the user confirms. Keyboard
 * control mirrors the pointer interactions: arrows move, Enter sets and Escape
 * cancels.
 */
@Component({
    selector: 'dot-image-editor-focal-overlay',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [DotMessagePipe],
    templateUrl: './dot-image-editor-focal-overlay.component.html',
    styleUrl: './dot-image-editor-focal-overlay.component.scss',
    animations: [focalPointPop()],
    host: { '(keydown.escape)': 'onEscape($event)' }
})
export class DotImageEditorFocalOverlayComponent {
    /** Bounds of the rendered image within the canvas, in CSS px. */
    imageRect = input<ImageRect>();

    readonly #store = inject(ImageEditorStore);
    readonly #dispatch = injectDispatch(imageEditorToolEvents);
    readonly #host = inject(ElementRef<HTMLElement>);

    /** Whether the focal tool is the active canvas tool. */
    protected readonly isActive = computed(() => this.#store.activeTool() === 'focal');

    /** Working focal point as normalized 0..1 coordinates. */
    protected readonly point = signal<NormalizedPoint>({ x: 0.5, y: 0.5 });

    /** Absolute CSS-px position of the marker center within the canvas. */
    protected readonly markerStyle = computed(() => {
        const rect = this.imageRect();
        const { x, y } = this.point();

        return {
            left: `${(rect?.x ?? 0) + x * (rect?.width ?? 0)}px`,
            top: `${(rect?.y ?? 0) + y * (rect?.height ?? 0)}px`
        };
    });

    constructor() {
        // Seed the marker from the stored focal point whenever the tool
        // activates so it reflects the persisted position.
        effect(() => {
            if (this.isActive()) {
                const focal = this.#store.focalPoint();
                this.point.set({ x: focal.x, y: focal.y });
            }
        });
    }

    /**
     * Places the focal point at the clicked location and tracks the drag, then
     * commits it on release — the marker IS the focal point, so positioning it
     * sets it (no separate confirm step).
     */
    protected onSurfacePointerDown(event: PointerEvent): void {
        event.preventDefault();
        this.#setFromClient(event.clientX, event.clientY);
        this.#trackPointer(
            (clientX, clientY) => this.#setFromClient(clientX, clientY),
            () => this.setFocalPoint()
        );
    }

    /** Moves (and commits) or finishes the focal point in response to keyboard input. */
    protected onMarkerKeydown(event: KeyboardEvent): void {
        const step = event.shiftKey ? NUDGE_STEP_LARGE : NUDGE_STEP;
        const current = this.point();

        switch (event.key) {
            case 'ArrowLeft':
                event.preventDefault();
                this.#moveTo(current.x - step, current.y);
                this.setFocalPoint();
                break;
            case 'ArrowRight':
                event.preventDefault();
                this.#moveTo(current.x + step, current.y);
                this.setFocalPoint();
                break;
            case 'ArrowUp':
                event.preventDefault();
                this.#moveTo(current.x, current.y - step);
                this.setFocalPoint();
                break;
            case 'ArrowDown':
                event.preventDefault();
                this.#moveTo(current.x, current.y + step);
                this.setFocalPoint();
                break;
            case 'Enter':
                event.preventDefault();
                this.done();
                break;
            default:
                break;
        }
    }

    /** Commits the current marker position as the focal point (normalized 0..1). */
    setFocalPoint(): void {
        const { x, y } = this.point();
        this.#dispatch.focalPointSet({ x: clamp(x, 0, 1), y: clamp(y, 0, 1) });
    }

    /** Leaves the focal tool (back to move), keeping the placed focal point. */
    done(): void {
        this.#dispatch.toolSelected('move');
    }

    /**
     * Intercepts Escape so the host dialog does not close while placing the focal
     * point; the keypress instead leaves the focal tool (the point stays set).
     */
    protected onEscape(event: KeyboardEvent): void {
        if (!this.isActive()) {
            return;
        }

        event.stopPropagation();
        this.done();
    }

    /** Converts a client-space pointer position into a normalized focal point. */
    #setFromClient(clientX: number, clientY: number): void {
        const rect = this.imageRect();

        if (!rect || rect.width === 0 || rect.height === 0) {
            return;
        }

        const host = this.#hostRect();
        const localX = clientX - host.left - rect.x;
        const localY = clientY - host.top - rect.y;
        this.#moveTo(localX / rect.width, localY / rect.height);
    }

    /** Sets the working point from normalized coordinates, clamped to 0..1. */
    #moveTo(x: number, y: number): void {
        this.point.set({ x: clamp(x, 0, 1), y: clamp(y, 0, 1) });
    }

    /** Tracks pointer movement until release, reporting position and a release hook. */
    #trackPointer(onMove: (clientX: number, clientY: number) => void, onUp: () => void): void {
        const move = (event: PointerEvent) => onMove(event.clientX, event.clientY);
        const up = () => {
            window.removeEventListener('pointermove', move);
            window.removeEventListener('pointerup', up);
            onUp();
        };

        window.addEventListener('pointermove', move);
        window.addEventListener('pointerup', up);
    }

    /** The host element's viewport rectangle, used to map client coordinates. */
    #hostRect(): DOMRect {
        return this.#host.nativeElement.getBoundingClientRect();
    }
}
