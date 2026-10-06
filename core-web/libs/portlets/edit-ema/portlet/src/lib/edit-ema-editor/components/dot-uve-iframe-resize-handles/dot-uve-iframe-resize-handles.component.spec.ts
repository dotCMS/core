import { Spectator, byTestId, createComponentFactory } from '@openng/spectator/vitest';
import { Mock, vi } from 'vitest';

import { signal } from '@angular/core';

import { DotUveIframeResizeHandlesComponent } from './dot-uve-iframe-resize-handles.component';

import { UVEStore } from '../../../store/dot-uve.store';

/**
 * Build a synthetic PointerEvent. jsdom's PointerEvent constructor exists but
 * lacks the spec-required properties; spread our overrides on top.
 */
function makePointerEvent(type: string, props: Partial<PointerEvent> = {}): PointerEvent {
    const event = new Event(type, { bubbles: true, cancelable: true }) as unknown as PointerEvent;
    Object.assign(event, {
        clientX: 0,
        clientY: 0,
        pointerId: 1,
        ...props
    });
    return event;
}

/**
 * A jsdom-native AbortController for this file only.
 *
 * The component passes `{ signal }` to `addEventListener`, and zone.js handles that
 * option by registering an `abort` listener ON THE SIGNAL using the jsdom
 * `EventTarget.prototype.addEventListener` it captured at patch time. Under Vitest the
 * global AbortSignal is Node's — `signal instanceof window.EventTarget` is false — so
 * jsdom's brand check rejects it with "'addEventListener' called on an object that is
 * not a valid instance of EventTarget", the drag listeners are never attached, and both
 * pointermove and pointerup do nothing. Jest's older jsdom accepted it.
 *
 * Scoped to this spec rather than the project setup: it swaps a global, and this is the
 * only component in the workspace that hands an AbortSignal to addEventListener.
 */
class JsdomAbortSignal extends EventTarget {
    aborted = false;
    reason: unknown = undefined;
    onabort: ((this: AbortSignal, event: Event) => void) | null = null;

    throwIfAborted(): void {
        if (this.aborted) {
            throw this.reason;
        }
    }
}

class JsdomAbortController {
    readonly signal = new JsdomAbortSignal();

    abort(reason?: unknown): void {
        if (this.signal.aborted) {
            return;
        }
        this.signal.aborted = true;
        this.signal.reason = reason ?? new Error('AbortError');
        this.signal.dispatchEvent(new Event('abort'));
    }
}

describe('DotUveIframeResizeHandlesComponent', () => {
    let spectator: Spectator<DotUveIframeResizeHandlesComponent>;
    let updateEditorResizeState: Mock;
    let updateEditorOnResizeEnd: Mock;
    let viewExitDevicePreset: Mock;
    let viewSetIframeSize: Mock;

    const createComponent = createComponentFactory({
        component: DotUveIframeResizeHandlesComponent,
        providers: [
            {
                provide: UVEStore,
                useFactory: () => ({
                    viewIframeWidth: signal(800),
                    viewIframeHeight: signal(600),
                    updateEditorResizeState,
                    updateEditorOnResizeEnd,
                    viewExitDevicePreset,
                    viewSetIframeSize
                })
            }
        ]
    });

    let originalAbortController: typeof AbortController;

    beforeEach(() => {
        originalAbortController = globalThis.AbortController;
        globalThis.AbortController = JsdomAbortController as unknown as typeof AbortController;
        updateEditorResizeState = vi.fn();
        updateEditorOnResizeEnd = vi.fn();
        viewExitDevicePreset = vi.fn();
        viewSetIframeSize = vi.fn();
        spectator = createComponent();
    });

    afterEach(() => {
        globalThis.AbortController = originalAbortController;
    });

    /**
     * Stub setPointerCapture / releasePointerCapture / hasPointerCapture on a
     * handle element. jsdom doesn't implement these so we track calls manually.
     */
    function stubPointerCapture(handle: HTMLElement) {
        let captured = false;
        handle.setPointerCapture = vi.fn(() => {
            captured = true;
        });
        handle.releasePointerCapture = vi.fn(() => {
            captured = false;
        });
        handle.hasPointerCapture = vi.fn(() => captured);
        return {
            isCaptured: () => captured
        };
    }

    it('starts the drag on pointerdown: sets resize state and exits device preset', () => {
        const handle = spectator.query(byTestId('resize-handle-right')) as HTMLElement;
        stubPointerCapture(handle);

        handle.dispatchEvent(makePointerEvent('pointerdown'));

        expect(updateEditorResizeState).toHaveBeenCalledTimes(1);
        expect(viewExitDevicePreset).toHaveBeenCalledTimes(1);
        // Resize state must be set BEFORE exiting the device preset so the
        // responsive-mode sync effect skips its canvas-snap.
        expect(updateEditorResizeState.mock.invocationCallOrder[0]).toBeLessThan(
            viewExitDevicePreset.mock.invocationCallOrder[0]
        );
    });

    it('updates iframe size on pointermove relative to the handle position', () => {
        const handle = spectator.query(byTestId('resize-handle-right')) as HTMLElement;
        stubPointerCapture(handle);
        // Pin the handle's bounding rect so the math is deterministic.
        handle.getBoundingClientRect = vi.fn(
            () =>
                ({
                    left: 100,
                    top: 0,
                    width: 16,
                    height: 100,
                    right: 116,
                    bottom: 100,
                    x: 100,
                    y: 0,
                    toJSON: () => ({})
                }) as DOMRect
        );

        handle.dispatchEvent(makePointerEvent('pointerdown'));
        // Cursor at clientX 200; handle center at 108; delta = 92 → width = 800 + 92 = 892.
        handle.dispatchEvent(makePointerEvent('pointermove', { clientX: 200, clientY: 50 }));

        expect(viewSetIframeSize).toHaveBeenCalledWith({ width: 892 });
    });

    it('ends the drag on pointerup and releases pointer capture', () => {
        const handle = spectator.query(byTestId('resize-handle-right')) as HTMLElement;
        const cap = stubPointerCapture(handle);

        handle.dispatchEvent(makePointerEvent('pointerdown'));
        expect(cap.isCaptured()).toBe(true);

        handle.dispatchEvent(makePointerEvent('pointerup'));

        expect(handle.releasePointerCapture).toHaveBeenCalledWith(1);
        expect(updateEditorOnResizeEnd).toHaveBeenCalledTimes(1);
        expect(cap.isCaptured()).toBe(false);
    });

    it('cleans up listeners and resets state when destroyed mid-drag', () => {
        const handle = spectator.query(byTestId('resize-handle-right')) as HTMLElement;
        const cap = stubPointerCapture(handle);
        handle.getBoundingClientRect = vi.fn(
            () => ({ left: 100, top: 0, width: 16, height: 100 }) as DOMRect
        );

        handle.dispatchEvent(makePointerEvent('pointerdown'));
        expect(cap.isCaptured()).toBe(true);
        expect(updateEditorOnResizeEnd).not.toHaveBeenCalled();

        // Tear the component down before the user releases.
        spectator.fixture.destroy();

        // Editor state must flip back to IDLE so the rest of the editor unfreezes.
        expect(updateEditorOnResizeEnd).toHaveBeenCalledTimes(1);

        // After destroy, further pointermove events must NOT touch the store.
        viewSetIframeSize.mockClear();
        handle.dispatchEvent(makePointerEvent('pointermove', { clientX: 500, clientY: 0 }));
        expect(viewSetIframeSize).not.toHaveBeenCalled();
    });
});
