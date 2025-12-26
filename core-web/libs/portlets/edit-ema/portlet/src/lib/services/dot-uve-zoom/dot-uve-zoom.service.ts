import { fromEvent } from 'rxjs';

import { Injectable, signal, computed, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { WINDOW } from '@dotcms/utils';

export interface ZoomCanvasStyles {
    outer: {
        width: string;
        height: string;
    };
    inner: {
        width: string;
        height: string;
        transform: string;
        transformOrigin: string;
    };
}

@Injectable()
export class DotUveZoomService {
    private readonly window = inject(WINDOW);
    private readonly destroyRef = inject(DestroyRef);

    readonly $zoomLevel = signal<number>(1);
    readonly $isZoomMode = signal<boolean>(false);
    readonly $iframeDocHeight = signal<number>(0);

    #zoomModeResetTimeout: ReturnType<typeof setTimeout> | null = null;
    #gestureStartZoom = 1;

    readonly $canvasOuterStyles = computed<ZoomCanvasStyles['outer']>(() => {
        const zoom = this.$zoomLevel();
        const height = this.$iframeDocHeight() || 800;
        return {
            width: `${1520 * zoom}px`,
            height: `${height * zoom}px`
        };
    });

    readonly $canvasInnerStyles = computed<ZoomCanvasStyles['inner']>(() => {
        const zoom = this.$zoomLevel();
        const height = this.$iframeDocHeight() || 800;
        return {
            width: `1520px`,
            height: `${height}px`,
            transform: `scale(${zoom})`,
            transformOrigin: 'top left'
        };
    });

    zoomIn(): void {
        this.$zoomLevel.set(Math.max(0.1, Math.min(3, this.$zoomLevel() + 0.1)));
    }

    zoomOut(): void {
        this.$zoomLevel.set(Math.max(0.1, Math.min(3, this.$zoomLevel() - 0.1)));
    }

    resetZoom(): void {
        this.$zoomLevel.set(1);
    }

    zoomLabel(): string {
        return `${Math.round(this.$zoomLevel() * 100)}%`;
    }

    setupZoomInteractions(
        zoomContainer: HTMLElement,
        editorContent: HTMLElement,
        onClampScroll: () => void
    ): void {
        type GestureLikeEvent = Event & {
            scale?: number;
            clientX: number;
            clientY: number;
            preventDefault: () => void;
        };

        const applyZoomFromWheel = (event: WheelEvent) => {
            if (!event.ctrlKey && !event.metaKey) {
                return;
            }

            const rect = editorContent.getBoundingClientRect();
            const insideEditor =
                event.clientX >= rect.left &&
                event.clientX <= rect.right &&
                event.clientY >= rect.top &&
                event.clientY <= rect.bottom;

            if (!insideEditor) {
                return;
            }

            event.preventDefault();

            this.$isZoomMode.set(true);
            if (this.#zoomModeResetTimeout) {
                clearTimeout(this.#zoomModeResetTimeout);
            }
            this.#zoomModeResetTimeout = setTimeout(() => this.$isZoomMode.set(false), 150);

            const delta = event.deltaY > 0 ? -0.1 : 0.1;
            const newZoom = Math.max(0.1, Math.min(3, this.$zoomLevel() + delta));
            this.$zoomLevel.set(newZoom);
            onClampScroll();
        };

        // Zoom with mouse wheel on the canvas container
        fromEvent<WheelEvent>(zoomContainer, 'wheel', { passive: false })
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(applyZoomFromWheel);

        // Zoom with mouse wheel on the whole editor content area
        fromEvent<WheelEvent>(editorContent, 'wheel', { passive: false })
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(applyZoomFromWheel);

        // Trackpad pinch can still trigger browser zoom if the event originates from the iframe
        fromEvent<WheelEvent>(this.window, 'wheel', { passive: false, capture: true } as AddEventListenerOptions)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(applyZoomFromWheel);

        // Safari trackpad pinch uses non-standard GestureEvents
        fromEvent<GestureLikeEvent>(
            this.window,
            'gesturestart',
            { passive: false, capture: true } as AddEventListenerOptions
        )
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe((event) => {
                const rect = editorContent.getBoundingClientRect();
                const insideEditor =
                    event.clientX >= rect.left &&
                    event.clientX <= rect.right &&
                    event.clientY >= rect.top &&
                    event.clientY <= rect.bottom;

                if (!insideEditor) {
                    return;
                }

                event.preventDefault();
                this.$isZoomMode.set(true);
                this.#gestureStartZoom = this.$zoomLevel();
            });

        fromEvent<GestureLikeEvent>(
            this.window,
            'gesturechange',
            { passive: false, capture: true } as AddEventListenerOptions
        )
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe((event) => {
                const rect = editorContent.getBoundingClientRect();
                const insideEditor =
                    event.clientX >= rect.left &&
                    event.clientX <= rect.right &&
                    event.clientY >= rect.top &&
                    event.clientY <= rect.bottom;

                if (!insideEditor) {
                    return;
                }

                event.preventDefault();
                const scale = typeof event.scale === 'number' ? event.scale : 1;
                const newZoom = Math.max(0.1, Math.min(3, this.#gestureStartZoom * scale));
                this.$zoomLevel.set(newZoom);
            });

        fromEvent<GestureLikeEvent>(
            this.window,
            'gestureend',
            { passive: false, capture: true } as AddEventListenerOptions
        )
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe((event) => {
                event.preventDefault();
                this.$isZoomMode.set(false);
            });
    }

    setIframeDocHeight(height: number): void {
        this.$iframeDocHeight.set(height);
    }
}

