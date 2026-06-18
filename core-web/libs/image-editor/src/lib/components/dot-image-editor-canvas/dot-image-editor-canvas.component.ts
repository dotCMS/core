import { injectDispatch } from '@ngrx/signals/events';

import {
    ChangeDetectionStrategy,
    Component,
    computed,
    DestroyRef,
    ElementRef,
    inject,
    signal,
    viewChild
} from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { SkeletonModule } from 'primeng/skeleton';

import { DotMessagePipe } from '@dotcms/ui';

import { imageEditorLifecycleEvents } from '../../store/image-editor.events';
import { ImageEditorStore } from '../../store/image-editor.store';
import { DotImageEditorAddressBarComponent } from '../dot-image-editor-address-bar/dot-image-editor-address-bar.component';
import {
    DotImageEditorCropOverlayComponent,
    ImageRect
} from '../dot-image-editor-crop-overlay/dot-image-editor-crop-overlay.component';
import { DotImageEditorFocalOverlayComponent } from '../dot-image-editor-focal-overlay/dot-image-editor-focal-overlay.component';
import { DotImageEditorToolRailComponent } from '../dot-image-editor-tool-rail/dot-image-editor-tool-rail.component';

/** Smallest zoom percentage the canvas allows. */
const ZOOM_MIN = 10;
/** Largest zoom percentage the canvas allows. */
const ZOOM_MAX = 400;
/** Step applied per zoom-in / zoom-out request. */
const ZOOM_STEP = 25;
/** Default (fit) zoom percentage. */
const ZOOM_DEFAULT = 100;

/**
 * Dark stage that renders the live image preview at the center of the editor.
 * Hosts the top address sub-bar, the floating tool rail, the crop/focal overlays,
 * and a persistent bottom footer band that mirrors the address bar and surfaces
 * the active tool's actions (apply/cancel for crop, set/cancel for focal). Owns
 * three pieces of local UI state the store does not: a two-layer image crossfade
 * between successive previews, the rendered image's bounding rect (measured for
 * the overlays), and the display-only zoom level. Preview loading outcomes are
 * reported back to the store via {@link imageEditorLifecycleEvents}.
 */
@Component({
    selector: 'dot-image-editor-canvas',
    templateUrl: './dot-image-editor-canvas.component.html',
    styleUrl: './dot-image-editor-canvas.component.scss',
    imports: [
        ButtonModule,
        ProgressSpinnerModule,
        SkeletonModule,
        DotMessagePipe,
        DotImageEditorAddressBarComponent,
        DotImageEditorToolRailComponent,
        DotImageEditorCropOverlayComponent,
        DotImageEditorFocalOverlayComponent
    ],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotImageEditorCanvasComponent {
    protected readonly store = inject(ImageEditorStore);
    readonly #dispatch = injectDispatch(imageEditorLifecycleEvents);
    readonly #destroyRef = inject(DestroyRef);

    /** The image stage, used as the origin for the rendered image rect. */
    protected readonly stage = viewChild<ElementRef<HTMLElement>>('stage');
    /** The currently displayed image, observed to recompute its rendered rect. */
    protected readonly displayImg = viewChild<ElementRef<HTMLImageElement>>('displayImg');

    /** The crop overlay, so the footer can apply or cancel the active crop. */
    protected readonly cropOverlay = viewChild(DotImageEditorCropOverlayComponent);
    /** The focal overlay, so the footer can set or cancel the active focal point. */
    protected readonly focalOverlay = viewChild(DotImageEditorFocalOverlayComponent);

    /** URL of the last successfully loaded preview, shown on the bottom layer. */
    protected readonly displayedUrl = signal<string>('');

    /**
     * URL queued for loading on the top layer: the store's current preview when
     * it differs from what is already displayed, otherwise empty.
     */
    protected readonly pendingUrl = computed(() => {
        const next = this.store.previewUrl();

        return next && next !== this.displayedUrl() ? next : '';
    });

    /** Rendered bounds of the displayed image within the stage, in CSS px. */
    protected readonly imageRect = signal<ImageRect | undefined>(undefined);

    /** Display-only zoom percentage applied as a CSS transform to the stage. */
    protected readonly zoomLevel = signal<number>(ZOOM_DEFAULT);

    /** CSS scale derived from the current zoom percentage. */
    protected readonly stageScale = computed(() => `scale(${this.zoomLevel() / 100})`);

    /** Observes the displayed image so overlay rects track resize and layout. */
    #resizeObserver: ResizeObserver | null = null;

    constructor() {
        this.#destroyRef.onDestroy(() => this.#resizeObserver?.disconnect());
    }

    /**
     * Promotes a freshly loaded pending image to the displayed layer and reports
     * the successful preview to the store. The crossfade is keyed on URL identity
     * so the visible frame is never blanked while the next one loads.
     *
     * Promotes the URL that actually finished loading — not the live store value,
     * which may have advanced under rapid edits. When a newer preview already
     * exists, the `pendingUrl` computed keeps Layer B mounted for that newer URL.
     * @param loadedUrl - The URL of the pending image that finished loading
     */
    protected onPendingLoaded(loadedUrl: string): void {
        this.displayedUrl.set(loadedUrl);
        this.#measureImageRect();
        this.#dispatch.previewLoaded();
    }

    /** Reports a failed pending load, keeping the last good frame visible. */
    protected onPendingError(): void {
        this.#dispatch.previewErrored();
    }

    /** Recomputes the rendered image rect once the displayed image lays out. */
    protected onDisplayLoaded(): void {
        this.#observeDisplayImg();
        this.#measureImageRect();
    }

    /** Requests a fresh preview after a render error. */
    protected retry(): void {
        this.#dispatch.retryRequested();
    }

    /** Applies the active crop via the crop overlay from the footer action. */
    protected applyCrop(): void {
        this.cropOverlay()?.applyCrop();
    }

    /** Cancels the active crop via the crop overlay from the footer action. */
    protected cancelCrop(): void {
        this.cropOverlay()?.cancelCrop();
    }

    /** Sets the active focal point via the focal overlay from the footer action. */
    protected setFocalPoint(): void {
        this.focalOverlay()?.setFocalPoint();
    }

    /** Cancels the active focal point via the focal overlay from the footer action. */
    protected cancelFocalPoint(): void {
        this.focalOverlay()?.cancelFocalPoint();
    }

    /** Increases the zoom by one step, clamped to the maximum. */
    protected zoomIn(): void {
        this.zoomLevel.update((level) => Math.min(ZOOM_MAX, level + ZOOM_STEP));
    }

    /** Decreases the zoom by one step, clamped to the minimum. */
    protected zoomOut(): void {
        this.zoomLevel.update((level) => Math.max(ZOOM_MIN, level - ZOOM_STEP));
    }

    /** Resets the zoom so the image fits the stage. */
    protected fit(): void {
        this.zoomLevel.set(ZOOM_DEFAULT);
    }

    /** Lazily attaches a single ResizeObserver to the displayed image element. */
    #observeDisplayImg(): void {
        const img = this.displayImg()?.nativeElement;

        if (!img || this.#resizeObserver) {
            return;
        }

        this.#resizeObserver = new ResizeObserver(() => this.#measureImageRect());
        this.#resizeObserver.observe(img);
    }

    /**
     * Measures the displayed image relative to the stage origin so the crop and
     * focal overlays can position themselves over the rendered pixels.
     */
    #measureImageRect(): void {
        const stage = this.stage()?.nativeElement;
        const img = this.displayImg()?.nativeElement;

        if (!stage || !img) {
            return;
        }

        const stageBox = stage.getBoundingClientRect();
        const imgBox = img.getBoundingClientRect();

        this.imageRect.set({
            x: imgBox.left - stageBox.left,
            y: imgBox.top - stageBox.top,
            width: imgBox.width,
            height: imgBox.height
        });
    }
}
