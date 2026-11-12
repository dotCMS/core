import { createServiceFactory, SpectatorService } from '@openng/spectator/vitest';
import { Mock, vi } from 'vitest';

import { ElementRef, signal } from '@angular/core';

import { __DOTCMS_UVE_EVENT__ } from '@dotcms/types/internal';
import { WINDOW } from '@dotcms/utils';

import { DotUveDragDropService, DragDropHandlers } from './dot-uve-drag-drop.service';

import { EDITOR_STATE } from '../../shared/enums';
import { EMA_DRAG_ITEM_CONTENTLET_MOCK, getBoundsMock, ACTION_MOCK } from '../../shared/mocks';
import { UVEStore } from '../../store/dot-uve.store';
import {
    IframeMessage,
    UveIframeMessengerService
} from '../iframe-messenger/uve-iframe-messenger.service';

/** The new editor → page message (#38575); the event name does not exist in `@dotcms/types` yet. */
const SCROLL_VELOCITY = 'uve-scroll-velocity';

/** The fastest the editor asks the page to scroll, in px per frame (contract: clamped to 40). */
const MAX_VELOCITY = 40;

/** The iframe sits at the top left of the admin window: 1000px wide, 800px tall. */
const IFRAME_RECT = { left: 0, right: 1000, top: 0, bottom: 800, width: 1000, height: 800 };

/** The part of `UVEStore` the drag-drop service reads and writes. */
function createUVEStoreMock() {
    const editorBounds = signal(getBoundsMock(ACTION_MOCK));

    return {
        editorDragItem: signal(EMA_DRAG_ITEM_CONTENTLET_MOCK),
        editorBounds,
        setEditorState: vi.fn(),
        setEditorDragItem: vi.fn(),
        // Like the store's: the editor hides the drop targets until the page sends fresh ones.
        updateEditorScrollDragState: vi.fn(() => editorBounds.set([]))
    };
}

describe('DotUveDragDropService', () => {
    let spectator: SpectatorService<DotUveDragDropService>;
    let store: ReturnType<typeof createUVEStoreMock>;
    let handlers: Record<keyof DragDropHandlers, Mock>;
    let page: { postMessage: Mock };

    const createService = createServiceFactory({
        service: DotUveDragDropService,
        providers: [{ provide: WINDOW, useValue: window }]
    });

    /** Every message the editor posted to the page. */
    const posted = (): IframeMessage[] => page.postMessage.mock.calls.map(([message]) => message);

    /** The velocities the editor asked the page to scroll at, in order. */
    const velocities = (): number[] =>
        posted()
            .filter((message) => message.name === SCROLL_VELOCITY)
            .map((message) => (message as IframeMessage & { velocity: number }).velocity);

    const lastVelocity = (): number | undefined => velocities().at(-1);

    /** How many legacy one-step scroll requests the editor sent. */
    const legacyScrolls = (): number =>
        posted().filter((message) => message.name === __DOTCMS_UVE_EVENT__.UVE_SCROLL_INSIDE_IFRAME)
            .length;

    /** The pointer drags over the iframe at `clientY`, mid-way across. */
    const dragOver = (clientY: number) => {
        window.dispatchEvent(new MouseEvent('dragover', { clientX: 500, clientY }));
    };

    beforeEach(() => {
        vi.useFakeTimers();

        spectator = createService();
        store = createUVEStoreMock();
        page = { postMessage: vi.fn() };
        handlers = {
            onDrop: vi.fn(),
            onDragEnter: vi.fn(),
            onDragOver: vi.fn(),
            onDragLeave: vi.fn(),
            onDragEnd: vi.fn(),
            onDragStart: vi.fn()
        };

        spectator.inject(UveIframeMessengerService).setIframeWindow(page as unknown as Window);

        const iframe = document.createElement('iframe');
        vi.spyOn(iframe, 'getBoundingClientRect').mockReturnValue(IFRAME_RECT as DOMRect);

        spectator.service.setupDragEvents(
            store as unknown as InstanceType<typeof UVEStore>,
            new ElementRef(iframe),
            new ElementRef(document.createElement('div')),
            handlers
        );
    });

    afterEach(() => {
        vi.useRealTimers();
        vi.restoreAllMocks();
    });

    // #38575: auto-scroll speeds up the deeper the pointer is in the 100px edge zone, and the
    // page runs the scroll itself at the velocity the editor sends.
    describe('scroll velocity', () => {
        it('should not ask the page to scroll while the pointer is away from the edges', () => {
            dragOver(400);

            expect(velocities().filter((velocity) => velocity !== 0)).toEqual([]);
        });

        it('should scroll up near the top edge and down near the bottom edge', () => {
            dragOver(50);
            expect(lastVelocity()).toEqual(expect.any(Number));
            expect(lastVelocity()).toBeLessThan(0);

            dragOver(750);
            expect(lastVelocity()).toBeGreaterThan(0);
        });

        it('should scroll faster the closer the pointer is to the edge', () => {
            dragOver(90);
            const nearInnerBoundary = lastVelocity();

            dragOver(10);
            const nearEdge = lastVelocity();

            expect(velocities()).toHaveLength(2);
            expect(Math.abs(nearEdge)).toBeGreaterThan(Math.abs(nearInnerBoundary));
        });

        it('should scroll slowly, but scroll, just inside the edge zone', () => {
            // The bottom zone starts at 700.
            dragOver(701);

            expect(lastVelocity()).toEqual(expect.any(Number));
            expect(lastVelocity()).toBeGreaterThan(0);
            expect(lastVelocity()).toBeLessThanOrEqual(MAX_VELOCITY / 4);
        });

        it('should scroll at about the maximum speed at the very edge', () => {
            dragOver(1);

            expect(lastVelocity()).toEqual(expect.any(Number));
            expect(lastVelocity()).toBeLessThanOrEqual(-MAX_VELOCITY * 0.9);
            expect(lastVelocity()).toBeGreaterThanOrEqual(-MAX_VELOCITY);
        });

        it('should ask only when the velocity changes, not on every dragover', () => {
            dragOver(10);
            dragOver(10);
            dragOver(10);

            expect(velocities()).toHaveLength(1);
        });

        it('should ask the page to stop when the pointer leaves the edge zone', () => {
            dragOver(10);
            dragOver(400);

            expect(lastVelocity()).toBe(0);
        });

        it.each([
            { when: 'the user drops', event: () => new Event('drop') },
            { when: 'the drag ends', event: () => new Event('dragend') },
            {
                when: 'the drag leaves the window',
                event: () => new MouseEvent('dragleave', { relatedTarget: null })
            }
        ])('should ask the page to stop when $when', ({ event }) => {
            dragOver(10);

            window.dispatchEvent(event());

            expect(lastVelocity()).toBe(0);
        });
    });

    // Pages on an older @dotcms/uve only know the one-step scroll request. The editor keeps
    // sending it, slowly enough that each animated step can finish.
    describe('legacy scroll request', () => {
        it('should send it at most once every 250ms while the pointer stays in the edge zone', () => {
            dragOver(10);
            vi.advanceTimersByTime(50);
            dragOver(10);
            vi.advanceTimersByTime(100);
            dragOver(10);
            vi.advanceTimersByTime(99);
            dragOver(10);

            expect(legacyScrolls()).toBe(1);

            vi.advanceTimersByTime(1);
            dragOver(10);

            expect(legacyScrolls()).toBe(2);
        });

        it('should not send it while the pointer is away from the edges', () => {
            dragOver(400);

            expect(legacyScrolls()).toBe(0);
        });
    });

    describe('drop targets near the edges', () => {
        // At the very top or bottom of the page nothing scrolls, so no fresh bounds would come
        // back: clearing them here left the edge contentlets unable to take a drop.
        it('should keep the drop targets when the pointer moves into the edge zone', () => {
            const bounds = store.editorBounds();

            dragOver(10);

            expect(store.editorBounds()).toEqual(bounds);
            expect(store.setEditorState).not.toHaveBeenCalledWith(EDITOR_STATE.SCROLL_DRAG);
        });
    });
});
