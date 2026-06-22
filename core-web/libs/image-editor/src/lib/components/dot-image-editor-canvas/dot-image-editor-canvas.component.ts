import { injectDispatch } from '@ngrx/signals/events';
import { EMPTY } from 'rxjs';

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
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';

import { ButtonModule } from 'primeng/button';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { SkeletonModule } from 'primeng/skeleton';

import { catchError, map, switchMap } from 'rxjs/operators';

import { DotMessagePipe } from '@dotcms/ui';

import { DotImageEditorService } from '../../services/dot-image-editor.service';
import { imageEditorLifecycleEvents, imageEditorToolEvents } from '../../store/image-editor.events';
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
    readonly #toolDispatch = injectDispatch(imageEditorToolEvents);
    readonly #destroyRef = inject(DestroyRef);
    readonly #service = inject(DotImageEditorService);

    /** Aspect-ratio presets for the focal-point-centered crop, shown in the focal bar. */
    protected readonly aspectPresets = [
        { key: 'square', label: '1:1', aspect: 1 },
        { key: 'wide', label: '16:9', aspect: 16 / 9 },
        { key: 'standard', label: '4:3', aspect: 4 / 3 }
    ];

    /** The aspect preset currently selected in the focal crop bar. */
    protected readonly selectedAspect = signal(this.aspectPresets[0]);

    /** The image stage, used as the origin for the rendered image rect. */
    protected readonly stage = viewChild<ElementRef<HTMLElement>>('stage');
    /** The currently displayed image, observed to recompute its rendered rect. */
    protected readonly displayImg = viewChild<ElementRef<HTMLImageElement>>('displayImg');

    /** The crop overlay, so the footer can apply or cancel the active crop. */
    protected readonly cropOverlay = viewChild(DotImageEditorCropOverlayComponent);
    /** The focal overlay, so the footer can set or cancel the active focal point. */
    protected readonly focalOverlay = viewChild(DotImageEditorFocalOverlayComponent);

    /** Filter URL of the last successfully loaded preview (the bottom layer's identity). */
    protected readonly displayedUrl = signal<string>('');

    /** Object URL rendered on the bottom (displayed) layer — verified, complete bytes. */
    protected readonly displayedSrc = signal<string>('');

    /**
     * Filter URL queued for loading on the top layer: the store's current preview
     * when it differs from what is already displayed, otherwise empty. This is the
     * remote URL we fetch as a blob; the layer renders the resulting object URL.
     */
    protected readonly pendingUrl = computed(() => {
        const next = this.store.previewUrl();

        return next && next !== this.displayedUrl() ? next : '';
    });

    /** Object URL of the verified pending blob, rendered on the top layer once ready. */
    protected readonly pendingSrc = signal<string>('');

    /** The verified-but-not-yet-promoted pending preview (filter URL + its object URL). */
    #pending: { filterUrl: string; objectUrl: string } | null = null;

    /** Object URL currently shown on the displayed layer, retained for revocation. */
    #displayedObjectUrl: string | null = null;

    /** Rendered bounds of the displayed image within the stage, in CSS px. */
    protected readonly imageRect = signal<ImageRect | undefined>(undefined);

    /** Display-only zoom percentage applied as a CSS transform to the stage. */
    protected readonly zoomLevel = signal<number>(ZOOM_DEFAULT);

    /** CSS scale derived from the current zoom percentage. */
    protected readonly stageScale = computed(() => `scale(${this.zoomLevel() / 100})`);

    /** Observes the displayed image so overlay rects track resize and layout. */
    #resizeObserver: ResizeObserver | null = null;

    constructor() {
        this.#destroyRef.onDestroy(() => {
            this.#resizeObserver?.disconnect();
            this.#revoke(this.#displayedObjectUrl);
            this.#revoke(this.#pending?.objectUrl ?? null);
        });

        // Fetch each queued preview as a complete, verified blob before it is ever
        // rendered. `switchMap` cancels a superseded in-flight request (a newer edit
        // wins); a fetch failure — including a truncated / partially-generated
        // response — reports to the store, which owns the silent-retry policy.
        toObservable(this.pendingUrl)
            .pipe(
                switchMap((filterUrl) => {
                    // A new target supersedes any verified-but-unpromoted pending blob.
                    this.#discardPending();

                    if (!filterUrl) {
                        return EMPTY;
                    }

                    return this.#service.loadPreviewImage(filterUrl).pipe(
                        map((objectUrl) => ({ filterUrl, objectUrl })),
                        catchError(() => {
                            this.#dispatch.previewErrored();

                            return EMPTY;
                        })
                    );
                }),
                takeUntilDestroyed()
            )
            .subscribe(({ filterUrl, objectUrl }) => {
                this.#pending = { filterUrl, objectUrl };
                this.pendingSrc.set(objectUrl);
            });
    }

    /**
     * Promotes the verified pending blob to the displayed layer and reports the
     * successful preview to the store. The pending image already holds complete,
     * fetched bytes (rendered from a local object URL), so it cannot paint a
     * truncated frame; `decode()` is a final paint-ready gate and we also require
     * real dimensions. The crossfade keeps the previous frame visible until this
     * one is promoted, so the canvas is never blanked.
     */
    protected onPendingLoaded(event: Event): void {
        const pending = this.#pending;
        const img = event.target as HTMLImageElement;

        // Guard against a stale load event after the pending blob was superseded.
        if (!pending) {
            return;
        }

        img.decode()
            .then(() => {
                if (!img.naturalWidth || !img.naturalHeight) {
                    this.#dispatch.previewErrored();

                    return;
                }

                // The pending blob becomes the displayed frame; release the one it replaces.
                this.#revoke(this.#displayedObjectUrl);
                this.#displayedObjectUrl = pending.objectUrl;
                this.displayedSrc.set(pending.objectUrl);
                this.displayedUrl.set(pending.filterUrl);
                this.#pending = null;
                this.pendingSrc.set('');

                this.#measureImageRect();
                this.#dispatch.previewLoaded();
            })
            .catch(() => this.#dispatch.previewErrored());
    }

    /**
     * Reports a failed pending load, keeping the last good frame visible. The store
     * owns the retry policy: it silently retries a transient failure before
     * surfacing the error UI (see the `previewErrored` reducer).
     */
    protected onPendingError(): void {
        this.#dispatch.previewErrored();
    }

    /** Drops any verified-but-unpromoted pending blob, revoking its object URL. */
    #discardPending(): void {
        this.#revoke(this.#pending?.objectUrl ?? null);
        this.#pending = null;
        this.pendingSrc.set('');
    }

    /** Releases an object URL created for a preview blob, if any. */
    #revoke(objectUrl: string | null): void {
        if (objectUrl) {
            URL.revokeObjectURL(objectUrl);
        }
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

    /** Crops to the selected aspect, centered on the focal point (then exits the tool). */
    protected applyFocalCrop(): void {
        const { aspect, label } = this.selectedAspect();
        this.#toolDispatch.aspectCropApplied({ aspect, label });
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
