import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator/jest';

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

describe('DotUveIframeResizeHandlesComponent', () => {
    let spectator: Spectator<DotUveIframeResizeHandlesComponent>;
    let updateEditorResizeState: jest.Mock;
    let updateEditorOnResizeEnd: jest.Mock;
    let viewExitDevicePreset: jest.Mock;
    let viewSetIframeSize: jest.Mock;

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

    beforeEach(() => {
        updateEditorResizeState = jest.fn();
        updateEditorOnResizeEnd = jest.fn();
        viewExitDevicePreset = jest.fn();
        viewSetIframeSize = jest.fn();
        spectator = createComponent();
    });

    /**
     * Stub setPointerCapture / releasePointerCapture / hasPointerCapture on a
     * handle element. jsdom doesn't implement these so we track calls manually.
     */
    function stubPointerCapture(handle: HTMLElement) {
        let captured = false;
        handle.setPointerCapture = jest.fn(() => {
            captured = true;
        });
        handle.releasePointerCapture = jest.fn(() => {
            captured = false;
        });
        handle.hasPointerCapture = jest.fn(() => captured);
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
        handle.getBoundingClientRect = jest.fn(
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
        handle.getBoundingClientRect = jest.fn(
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
