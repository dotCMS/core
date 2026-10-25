import { SpectatorDirective, createDirectiveFactory } from '@openng/spectator/vitest';
import { Mock, vi } from 'vitest';

import { WritableSignal, signal } from '@angular/core';

import { DotUveCanvasSizingDirective } from './dot-uve-canvas-sizing.directive';

import { EDITOR_STATE } from '../../../shared/enums';
import { UVEStore } from '../../../store/dot-uve.store';

/**
 * Stands in for the browser's ResizeObserver. jsdom has no layout, so nothing would ever report
 * that the viewport changed size; a test reports it through `notify`.
 */
class ViewportResizeObserver {
    static last: ViewportResizeObserver | null = null;

    readonly observe = vi.fn();
    readonly unobserve = vi.fn();
    readonly disconnect = vi.fn();

    readonly #callback: ResizeObserverCallback;

    constructor(callback: ResizeObserverCallback) {
        this.#callback = callback;
        ViewportResizeObserver.last = this;
    }

    notify(): void {
        this.#callback([], this as unknown as ResizeObserver);
    }
}

describe('DotUveCanvasSizingDirective', () => {
    let spectator: SpectatorDirective<DotUveCanvasSizingDirective>;
    let viewCanvasAvailableWidth: WritableSignal<number>;
    let viewCanvasAvailableHeight: WritableSignal<number>;
    let viewIframeWidth: WritableSignal<number>;
    let viewIframeHeight: WritableSignal<number>;
    let $viewIsResponsiveMode: WritableSignal<boolean>;
    let editorState: WritableSignal<EDITOR_STATE>;
    let viewSetCanvasAvailableSize: Mock;
    let viewSetIframeSize: Mock;
    let originalResizeObserver: typeof ResizeObserver;

    // The viewport's client box, padding included. jsdom lays nothing out, so every element
    // reports this size and the directive's element is the only one measured.
    const viewport = { width: 0, height: 0 };

    // The editor's viewport padding: 1rem on the top and left, 1.5rem on the right and bottom
    // for the resize handles. 40px on each axis, so a 1240×840 viewport leaves a 1200×800 canvas.
    const createDirective = createDirectiveFactory({
        directive: DotUveCanvasSizingDirective,
        template: `<div dotUveCanvasSizing style="padding: 16px 24px 24px 16px"></div>`,
        providers: [
            {
                provide: UVEStore,
                useFactory: () => ({
                    viewCanvasAvailableWidth,
                    viewCanvasAvailableHeight,
                    viewIframeWidth,
                    viewIframeHeight,
                    $viewIsResponsiveMode,
                    editorState,
                    viewSetCanvasAvailableSize,
                    viewSetIframeSize
                })
            }
        ]
    });

    /** Resizes the viewport, lets the browser report it, and runs change detection as the app would. */
    const resizeViewport = (width: number, height: number) => {
        viewport.width = width;
        viewport.height = height;
        ViewportResizeObserver.last?.notify();
        spectator.detectChanges();
    };

    /** The user drags the resize handles to a size of their own. */
    const userResizesIframe = (width: number, height: number) => {
        viewIframeWidth.set(width);
        viewIframeHeight.set(height);
        viewSetIframeSize.mockClear();
    };

    beforeEach(() => {
        originalResizeObserver = window.ResizeObserver;
        window.ResizeObserver = ViewportResizeObserver as unknown as typeof ResizeObserver;
        ViewportResizeObserver.last = null;

        viewport.width = 1240;
        viewport.height = 840;
        vi.spyOn(HTMLElement.prototype, 'clientWidth', 'get').mockImplementation(
            () => viewport.width
        );
        vi.spyOn(HTMLElement.prototype, 'clientHeight', 'get').mockImplementation(
            () => viewport.height
        );

        viewCanvasAvailableWidth = signal(0);
        viewCanvasAvailableHeight = signal(0);
        viewIframeWidth = signal(0);
        viewIframeHeight = signal(0);
        $viewIsResponsiveMode = signal(true);
        editorState = signal(EDITOR_STATE.IDLE);
        // The store's setters, without the clamping and RESIZING flip the real store adds.
        viewSetCanvasAvailableSize = vi.fn(({ width, height }) => {
            viewCanvasAvailableWidth.set(width);
            viewCanvasAvailableHeight.set(height);
        });
        viewSetIframeSize = vi.fn(({ width, height }) => {
            viewIframeWidth.set(width);
            viewIframeHeight.set(height);
        });
    });

    afterEach(() => {
        window.ResizeObserver = originalResizeObserver;
        vi.restoreAllMocks();
    });

    describe('canvas size', () => {
        it('should store the viewport size without its padding as the canvas size', () => {
            spectator = createDirective();

            expect(viewSetCanvasAvailableSize).toHaveBeenCalledWith({ width: 1200, height: 800 });
        });

        it('should store the new canvas size when the viewport resizes', () => {
            spectator = createDirective();

            resizeViewport(1040, 640);

            expect(viewSetCanvasAvailableSize).toHaveBeenLastCalledWith({
                width: 1000,
                height: 600
            });
        });

        it('should not store the size of a viewport with no room inside its padding', () => {
            viewport.width = 40;
            viewport.height = 40;

            spectator = createDirective();

            expect(viewSetCanvasAvailableSize).not.toHaveBeenCalled();
        });

        it('should stop following the viewport once destroyed', () => {
            spectator = createDirective();

            spectator.fixture.destroy();

            expect(ViewportResizeObserver.last?.disconnect).toHaveBeenCalled();
        });
    });

    describe('in responsive mode', () => {
        it('should size the iframe to the canvas on the first measure', () => {
            spectator = createDirective();

            expect(viewSetIframeSize).toHaveBeenLastCalledWith({ width: 1200, height: 800 });
            expect(viewIframeWidth()).toBe(1200);
            expect(viewIframeHeight()).toBe(800);
        });

        it('should keep the iframe filling the canvas when the canvas grows', () => {
            spectator = createDirective();

            resizeViewport(1440, 1040);

            expect(viewSetIframeSize).toHaveBeenLastCalledWith({ width: 1400, height: 1000 });
        });

        // A side panel opening, for instance.
        it('should keep the iframe filling the canvas when the canvas shrinks', () => {
            spectator = createDirective();

            resizeViewport(840, 840);

            expect(viewSetIframeSize).toHaveBeenLastCalledWith({ width: 800, height: 800 });
        });

        // The user's size goes back through the store rather than being skipped, so the store
        // can clamp it when the canvas shrinks below it.
        it.each([
            { change: 'grows', width: 1440, height: 1040 },
            { change: 'shrinks', width: 840, height: 640 }
        ])('should keep a size the user set when the canvas $change', ({ width, height }) => {
            spectator = createDirective();
            userResizesIframe(1000, 700);

            resizeViewport(width, height);

            expect(viewSetIframeSize).toHaveBeenCalledTimes(1);
            expect(viewSetIframeSize).toHaveBeenCalledWith({ width: 1000, height: 700 });
        });

        // The store outlives the editor, which is created again on every EDIT↔PREVIEW switch.
        it('should size the iframe to the canvas when created again, even after the user resized it', () => {
            viewCanvasAvailableWidth.set(1200);
            viewCanvasAvailableHeight.set(800);
            viewIframeWidth.set(600);
            viewIframeHeight.set(400);

            spectator = createDirective();

            expect(viewIframeWidth()).toBe(1200);
            expect(viewIframeHeight()).toBe(800);
        });
    });

    describe('in device mode', () => {
        beforeEach(() => {
            $viewIsResponsiveMode.set(false);
        });

        it('should store the canvas size and leave the iframe at the device size', () => {
            spectator = createDirective();

            resizeViewport(1440, 1040);

            expect(viewSetCanvasAvailableSize).toHaveBeenLastCalledWith({
                width: 1400,
                height: 1000
            });
            expect(viewSetIframeSize).not.toHaveBeenCalled();
        });
    });

    describe('when the view switches from a device to responsive mode', () => {
        beforeEach(() => {
            $viewIsResponsiveMode.set(false);
        });

        it('should size the iframe to the canvas', () => {
            spectator = createDirective();

            $viewIsResponsiveMode.set(true);
            spectator.detectChanges();

            expect(viewSetIframeSize).toHaveBeenCalledWith({ width: 1200, height: 800 });
        });

        // Dragging a handle on a device exits the device first, and the iframe must not jump to
        // the canvas size under the pointer.
        it('should leave the iframe alone while the user drags a resize handle', () => {
            spectator = createDirective();

            editorState.set(EDITOR_STATE.RESIZING);
            $viewIsResponsiveMode.set(true);
            spectator.detectChanges();

            expect(viewSetIframeSize).not.toHaveBeenCalled();
        });

        it('should leave the iframe alone before the canvas is measured', () => {
            viewport.width = 0;
            viewport.height = 0;
            spectator = createDirective();

            $viewIsResponsiveMode.set(true);
            spectator.detectChanges();

            expect(viewSetIframeSize).not.toHaveBeenCalled();
        });
    });
});
