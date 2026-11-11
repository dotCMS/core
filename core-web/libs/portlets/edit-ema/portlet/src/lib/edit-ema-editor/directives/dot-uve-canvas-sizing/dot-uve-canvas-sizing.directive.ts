import { signalMethod } from '@ngrx/signals';

import { AfterViewInit, DestroyRef, Directive, ElementRef, inject } from '@angular/core';

import { EDITOR_STATE } from '../../../shared/enums';
import { UVEStore } from '../../../store/dot-uve.store';
import { measureCanvasAvailableSize } from '../../../utils';

/**
 * Sizes the canvas from the viewport element it sits on. It stores the room the viewport leaves
 * for the iframe and, in responsive mode, keeps the iframe filling that room unless the user set
 * a size of their own.
 */
@Directive({
    selector: '[dotUveCanvasSizing]'
})
export class DotUveCanvasSizingDirective implements AfterViewInit {
    readonly #uveStore = inject(UVEStore);
    readonly #element = inject<ElementRef<HTMLElement>>(ElementRef);
    readonly #destroyRef = inject(DestroyRef);

    readonly #responsiveModeSync = signalMethod<boolean>((isResponsive) => {
        if (!isResponsive) {
            return;
        }

        if (this.#uveStore.editorState() === EDITOR_STATE.RESIZING) {
            return;
        }
        const width = this.#uveStore.viewCanvasAvailableWidth();
        const height = this.#uveStore.viewCanvasAvailableHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        this.#uveStore.viewSetIframeSize({ width, height });
    });

    constructor() {
        this.#responsiveModeSync(this.#uveStore.$viewIsResponsiveMode);
    }

    ngAfterViewInit(): void {
        const el = this.#element.nativeElement;

        const apply = () => {
            const size = measureCanvasAvailableSize(el);
            if (!size) {
                return;
            }

            // Snapshot the previous canvas size *before* the store update so
            // we can tell whether the iframe was auto-filling it.
            const prevCanvasW = this.#uveStore.viewCanvasAvailableWidth();
            const prevCanvasH = this.#uveStore.viewCanvasAvailableHeight();
            // viewSetCanvasAvailableSize flips RESIZING internally when the
            // canvas actually changes size — overlays hide until SET_BOUNDS
            // settles them.
            this.#uveStore.viewSetCanvasAvailableSize(size);

            if (!this.#uveStore.$viewIsResponsiveMode()) {
                return;
            }

            const iframeWidth = this.#uveStore.viewIframeWidth();
            const iframeHeight = this.#uveStore.viewIframeHeight();

            // The iframe was auto-filling the canvas (its dimensions match the
            // previous canvas size), so keep it filling. If the user has
            // explicitly resized it, leave their size alone — but re-apply it
            // through viewSetIframeSize so its built-in canvas-clamp can
            // shrink it when the canvas shrinks (e.g. a side panel opens).
            const wasAutoFilling = iframeWidth === prevCanvasW && iframeHeight === prevCanvasH;

            if (wasAutoFilling) {
                this.#uveStore.viewSetIframeSize(size);
            } else {
                this.#uveStore.viewSetIframeSize({
                    width: iframeWidth,
                    height: iframeHeight
                });
            }
        };

        // Initial sync — runs synchronously in ngAfterViewInit before first paint
        apply();

        const resizeObserver = new ResizeObserver(() => apply());
        resizeObserver.observe(el);

        this.#destroyRef.onDestroy(() => resizeObserver.disconnect());
    }
}
