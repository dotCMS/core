import { injectDispatch } from '@ngrx/signals/events';
import { EMPTY } from 'rxjs';

import {
    ChangeDetectionStrategy,
    Component,
    computed,
    DestroyRef,
    effect,
    ElementRef,
    inject,
    signal,
    untracked,
    viewChild
} from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';

import { ButtonModule } from 'primeng/button';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { SkeletonModule } from 'primeng/skeleton';

import { catchError, map, switchMap } from 'rxjs/operators';

import { DotMessagePipe } from '@dotcms/ui';

import { ImageRect } from '../../models/image-editor.models';
import { DotImageEditorService } from '../../services/dot-image-editor.service';
import { imageEditorLifecycleEvents, imageEditorToolEvents } from '../../store/image-editor.events';
import { ImageEditorStore } from '../../store/image-editor.store';
import { clamp } from '../../utils/dimensions.util';
import { DotImageEditorAddressBarComponent } from '../dot-image-editor-address-bar/dot-image-editor-address-bar.component';
import { DotImageEditorCropOverlayComponent } from '../dot-image-editor-crop-overlay/dot-image-editor-crop-overlay.component';
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

    /** Pan offset (CSS px) applied to the stage so a zoomed-in image can be dragged. */
    protected readonly panOffset = signal<{ x: number; y: number }>({ x: 0, y: 0 });

    /** Whether a pan drag is in progress (suppresses the transform transition). */
    protected readonly panning = signal(false);

    /** Panning only applies when zoomed past fit with the move tool active. */
    protected readonly canPan = computed(
        () => this.zoomLevel() > ZOOM_DEFAULT && this.store.activeTool() === 'move'
    );

    /** Combined pan + zoom transform applied to the stage. */
    protected readonly stageTransform = computed(() => {
        const { x, y } = this.panOffset();

        return `translate(${x}px, ${y}px) scale(${this.zoomLevel() / 100})`;
    });

    /**
     * The visible image region (image-local CSS px) captured when the crop tool is
     * activated while zoomed in. Seeds the crop overlay so switching to crop frames
     * exactly what the user had in view. `undefined` when not zoomed (full image).
     */
    protected readonly capturedCropRect = signal<ImageRect | undefined>(undefined);

    /** Observes the displayed image so overlay rects track resize and layout. */
    #resizeObserver: ResizeObserver | null = null;

    /** Tears down the active pan drag's window listeners, if any. */
    #panCleanup: (() => void) | null = null;

    constructor() {
        this.#destroyRef.onDestroy(() => {
            this.#resizeObserver?.disconnect();
            this.#detachPan();
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

        // When the crop tool activates while zoomed in, capture the region the user
        // had framed and reset to fit, so the crop box lands on exactly what was in
        // view (crop-to-current-view). `untracked` keeps this firing only on the
        // tool change, not on every zoom/pan tweak.
        effect(() => {
            const tool = this.store.activeTool();

            untracked(() => {
                if (tool !== 'crop') {
                    this.capturedCropRect.set(undefined);

                    return;
                }

                const visible = this.#computeVisibleImageRect();
                this.capturedCropRect.set(visible);

                if (visible) {
                    this.fit();
                }
            });
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
                // `decode()` is async: a newer edit may have superseded (and revoked)
                // this pending blob via #discardPending while it ran. Bail if so —
                // promoting a revoked object URL would render a broken frame.
                if (this.#pending !== pending) {
                    return;
                }

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

    /** Decreases the zoom by one step, clamped to the minimum; recenters at/below fit. */
    protected zoomOut(): void {
        const level = Math.max(ZOOM_MIN, this.zoomLevel() - ZOOM_STEP);
        this.zoomLevel.set(level);

        if (level <= ZOOM_DEFAULT) {
            this.panOffset.set({ x: 0, y: 0 });
        }
    }

    /** Resets the zoom so the image fits the stage and recenters the pan. */
    protected fit(): void {
        this.zoomLevel.set(ZOOM_DEFAULT);
        this.panOffset.set({ x: 0, y: 0 });
    }

    /**
     * Starts a pan drag when zoomed in with the move tool: pointer moves translate
     * the stage by the drag delta. Listeners live on `window` so the drag continues
     * outside the stage and are torn down on pointer-up (and on destroy).
     */
    protected onStagePointerDown(event: PointerEvent): void {
        if (!this.canPan()) {
            return;
        }

        event.preventDefault();
        this.panning.set(true);

        const startX = event.clientX;
        const startY = event.clientY;
        const origin = this.panOffset();

        const move = (moveEvent: PointerEvent) => {
            this.panOffset.set({
                x: origin.x + (moveEvent.clientX - startX),
                y: origin.y + (moveEvent.clientY - startY)
            });
        };
        const up = () => {
            this.panning.set(false);
            this.#detachPan();
        };

        this.#detachPan();
        window.addEventListener('pointermove', move);
        window.addEventListener('pointerup', up);
        this.#panCleanup = () => {
            window.removeEventListener('pointermove', move);
            window.removeEventListener('pointerup', up);
        };
    }

    /** Removes any active pan drag's window listeners. */
    #detachPan(): void {
        this.#panCleanup?.();
        this.#panCleanup = null;
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

        // The stage carries the zoom as a CSS `transform: scale(...)`, so
        // getBoundingClientRect returns painted (scaled) values. Divide back out so
        // the rect is in the stage's logical CSS px — the same space the overlays
        // (children of the scaled stage) position themselves in. Without this the
        // crop/focal markers drift at any zoom other than 100%.
        const scale = this.zoomLevel() / 100;
        const stageBox = stage.getBoundingClientRect();
        const imgBox = img.getBoundingClientRect();

        this.imageRect.set({
            x: (imgBox.left - stageBox.left) / scale,
            y: (imgBox.top - stageBox.top) / scale,
            width: imgBox.width / scale,
            height: imgBox.height / scale
        });
    }

    /**
     * The portion of the image currently visible in the viewport, expressed in the
     * image-local CSS px the crop overlay uses, or `undefined` when not zoomed in
     * (the whole image is already visible). Inverts the stage's
     * `translate(pan) scale(zoom)` transform about its center to find the logical
     * window that maps onto the visible stage box, then clamps it to the image.
     */
    #computeVisibleImageRect(): ImageRect | undefined {
        const rect = this.imageRect();
        const stage = this.stage()?.nativeElement;
        const scale = this.zoomLevel() / 100;

        if (!rect || !stage || scale <= 1) {
            return undefined;
        }

        const { x: panX, y: panY } = this.panOffset();
        const stageWidth = stage.clientWidth;
        const stageHeight = stage.clientHeight;
        const centerX = stageWidth / 2;
        const centerY = stageHeight / 2;

        const visibleLeft = centerX - (centerX + panX) / scale;
        const visibleRight = centerX + (stageWidth - centerX - panX) / scale;
        const visibleTop = centerY - (centerY + panY) / scale;
        const visibleBottom = centerY + (stageHeight - centerY - panY) / scale;

        const left = clamp(visibleLeft - rect.x, 0, rect.width);
        const right = clamp(visibleRight - rect.x, 0, rect.width);
        const top = clamp(visibleTop - rect.y, 0, rect.height);
        const bottom = clamp(visibleBottom - rect.y, 0, rect.height);

        if (right - left < 1 || bottom - top < 1) {
            return undefined;
        }

        return { x: left, y: top, width: right - left, height: bottom - top };
    }
}
