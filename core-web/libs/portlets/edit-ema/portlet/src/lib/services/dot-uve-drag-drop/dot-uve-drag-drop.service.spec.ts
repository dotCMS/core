import { createServiceFactory, SpectatorService } from '@openng/spectator/vitest';
import { Mock, vi } from 'vitest';

import { ElementRef, EnvironmentInjector, createEnvironmentInjector, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { __DOTCMS_UVE_EVENT__ } from '@dotcms/types/internal';
import { WINDOW } from '@dotcms/utils';

import { DotUveDragDropService, DragDropHandlers } from './dot-uve-drag-drop.service';

import { EDITOR_STATE } from '../../shared/enums';
import { EMA_DRAG_ITEM_CONTENTLET_MOCK, getBoundsMock, ACTION_MOCK } from '../../shared/mocks';
import { UVEStore } from '../../store/dot-uve.store';
import { TEMPORAL_DRAG_ITEM } from '../../utils';
import {
    IframeMessage,
    UveIframeMessengerService
} from '../iframe-messenger/uve-iframe-messenger.service';

/** The editor → page message (#38575), by its wire name, so a rename of the constant shows up here. */
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
        setEditorDragItem: vi.fn()
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
            .filter(
                (message): message is IframeMessage & { velocity: number } =>
                    message.name === SCROLL_VELOCITY && typeof message.velocity === 'number'
            )
            .map((message) => message.velocity);

    const lastVelocity = (): number | undefined => velocities().at(-1);

    /** The last velocity sent, which the test needs to compare against; none fails the test. */
    const lastSentVelocity = (): number => {
        const velocity = lastVelocity();

        if (velocity === undefined) {
            throw new Error('Expected the editor to have sent a velocity');
        }

        return velocity;
    };

    /** How many legacy one-step scroll requests the editor sent. */
    const legacyScrolls = (): number =>
        posted().filter((message) => message.name === __DOTCMS_UVE_EVENT__.UVE_SCROLL_INSIDE_IFRAME)
            .length;

    /** Has `service` listen to drags over an iframe at `IFRAME_RECT`, reporting to `to`. */
    const setUp = (
        service: DotUveDragDropService,
        to: Record<keyof DragDropHandlers, Mock> = handlers
    ) => {
        const iframe = document.createElement('iframe');
        vi.spyOn(iframe, 'getBoundingClientRect').mockReturnValue(IFRAME_RECT as DOMRect);

        service.setupDragEvents(
            store as unknown as InstanceType<typeof UVEStore>,
            new ElementRef(iframe),
            new ElementRef(document.createElement('div')),
            to
        );
    };

    const createHandlers = (): Record<keyof DragDropHandlers, Mock> => ({
        onDrop: vi.fn(),
        onDragEnter: vi.fn(),
        onDragOver: vi.fn(),
        onDragLeave: vi.fn(),
        onDragEnd: vi.fn(),
        onDragStart: vi.fn()
    });

    /** The pointer drags over the iframe at `clientY`, mid-way across. */
    const dragOver = (clientY: number) => {
        window.dispatchEvent(new MouseEvent('dragover', { clientX: 500, clientY }));
    };

    beforeEach(() => {
        vi.useFakeTimers();

        spectator = createService();
        store = createUVEStoreMock();
        page = { postMessage: vi.fn() };
        handlers = createHandlers();

        spectator.inject(UveIframeMessengerService).setIframeWindow(page as unknown as Window);

        setUp(spectator.service);
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
            const nearInnerBoundary = lastSentVelocity();

            dragOver(10);
            const nearEdge = lastSentVelocity();

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
            {
                when: 'the user drops',
                event: () => new Event('drop'),
                dragItem: EMA_DRAG_ITEM_CONTENTLET_MOCK
            },
            {
                when: 'the drag ends',
                event: () => new Event('dragend'),
                dragItem: EMA_DRAG_ITEM_CONTENTLET_MOCK
            },
            {
                // Only a file from the desktop: an editor drag keeps scrolling outside the
                // window, and no dragend ever comes for a file from outside the browser.
                when: 'a file dragged in from the desktop leaves the window',
                event: () => new MouseEvent('dragleave', { relatedTarget: null }),
                dragItem: TEMPORAL_DRAG_ITEM as typeof EMA_DRAG_ITEM_CONTENTLET_MOCK
            }
        ])('should ask the page to stop when $when', ({ event, dragItem }) => {
            store.editorDragItem.set(dragItem);
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

    // #38575, from the manual check: a scroll that has started keeps going when the pointer
    // leaves the iframe or the app window, and only stops when the pointer comes back to the
    // middle, on drop, or when the drag ends.
    describe('scroll outside the iframe', () => {
        /** Right of the iframe, over the edit panel. */
        const BESIDE = 1200;

        /** The pointer drags over the admin window at (`clientX`, `clientY`). */
        const dragOverAt = (clientX: number, clientY: number) => {
            window.dispatchEvent(new MouseEvent('dragover', { clientX, clientY }));
        };

        /** The pointer leaves the app window: `dragleave` with nowhere it went. */
        const leaveWindow = () => {
            window.dispatchEvent(new MouseEvent('dragleave', { relatedTarget: null }));
        };

        /** The direction of the last one-step scroll request. */
        const lastLegacyDirection = (): 'up' | 'down' | undefined =>
            posted()
                .filter((message) => message.name === __DOTCMS_UVE_EVENT__.UVE_SCROLL_INSIDE_IFRAME)
                .at(-1)?.direction;

        // R1
        describe('past the top or bottom of the iframe', () => {
            it.each([{ clientY: 0 }, { clientY: -20 }])(
                'should scroll up at the maximum speed with the pointer at or above the top (y=$clientY)',
                ({ clientY }) => {
                    dragOver(clientY);

                    expect(lastVelocity()).toBe(-MAX_VELOCITY);
                }
            );

            it.each([{ clientY: 800 }, { clientY: 850 }])(
                'should scroll down at the maximum speed with the pointer at or below the bottom (y=$clientY)',
                ({ clientY }) => {
                    dragOver(clientY);

                    expect(lastVelocity()).toBe(MAX_VELOCITY);
                }
            );

            it('should keep scrolling when the pointer moves from the edge zone past the edge', () => {
                dragOver(750);
                dragOver(850);

                expect(velocities()).not.toContain(0);
                expect(lastVelocity()).toBe(MAX_VELOCITY);
            });
        });

        // R2
        describe('beside the iframe while the page scrolls', () => {
            it('should keep scrolling when the pointer leaves the iframe sideways', () => {
                dragOver(750);
                const insideVelocity = lastVelocity();

                dragOverAt(BESIDE, 750);

                expect(velocities()).not.toContain(0);
                expect(lastVelocity()).toBe(insideVelocity);
            });

            it('should follow the pointer up and down by the same rule as inside the iframe', () => {
                dragOver(750);
                const atZoneMiddle = lastSentVelocity();

                dragOverAt(BESIDE, 790);
                expect(lastVelocity()).toBeGreaterThan(atZoneMiddle);

                dragOverAt(BESIDE, 10);
                expect(lastVelocity()).toBeLessThan(0);
            });

            it('should scroll at the maximum speed when the pointer is beside and past the edge', () => {
                dragOver(750);

                dragOverAt(BESIDE, 850);

                expect(lastVelocity()).toBe(MAX_VELOCITY);
            });

            it('should stop once the pointer, beside the iframe, is back in the middle', () => {
                dragOver(750);

                dragOverAt(BESIDE, 400);

                expect(lastVelocity()).toBe(0);
            });
        });

        // R3
        describe('beside the iframe while the page does not scroll', () => {
            it('should not start scrolling, as when a palette tile near the bottom is picked up', () => {
                dragOverAt(BESIDE, 750);
                dragOverAt(BESIDE, 850);
                dragOverAt(BESIDE, 10);

                expect(velocities().filter((velocity) => velocity !== 0)).toEqual([]);
                expect(legacyScrolls()).toBe(0);
            });

            it('should not start again beside the iframe once it stopped', () => {
                dragOver(750);
                dragOverAt(BESIDE, 400);

                dragOverAt(BESIDE, 750);

                expect(lastVelocity()).toBe(0);
            });
        });

        // R4
        describe('leaving the app window while dragging from the editor', () => {
            it('should keep scrolling at the last velocity', () => {
                dragOver(10);
                const velocity = lastVelocity();

                leaveWindow();

                expect(velocities()).not.toContain(0);
                expect(lastVelocity()).toBe(velocity);
            });

            it('should follow the pointer again at the next dragover', () => {
                dragOver(10);
                const atEdge = lastSentVelocity();

                leaveWindow();
                dragOver(50);

                expect(velocities()).not.toContain(0);
                expect(lastVelocity()).toBeLessThan(0);
                expect(lastVelocity()).toBeGreaterThan(atEdge);
            });

            it.each([
                { when: 'the user drops', event: () => new Event('drop') },
                { when: 'the drag ends', event: () => new Event('dragend') }
            ])('should still stop when $when', ({ event }) => {
                dragOver(10);
                leaveWindow();

                window.dispatchEvent(event());

                expect(lastVelocity()).toBe(0);
            });
        });

        // The canvas resets the editor on onDragLeave, which drops the drag item and the drop
        // targets: only a drag that will never come back may do that.
        describe('telling the canvas the drag left the app window', () => {
            it('should not, while dragging from the editor, so the drag and its drop targets stay', () => {
                leaveWindow();

                expect(handlers.onDragLeave).not.toHaveBeenCalled();
            });

            it('should, while dragging a file from the desktop', () => {
                store.editorDragItem.set(
                    TEMPORAL_DRAG_ITEM as typeof EMA_DRAG_ITEM_CONTENTLET_MOCK
                );

                leaveWindow();

                expect(handlers.onDragLeave).toHaveBeenCalledTimes(1);
            });
        });

        // R5
        it('should stop when a file dragged in from the desktop leaves the app window', () => {
            // No dragend ever reaches the editor for a file from outside the browser.
            store.editorDragItem.set(TEMPORAL_DRAG_ITEM as typeof EMA_DRAG_ITEM_CONTENTLET_MOCK);
            dragOver(10);

            leaveWindow();

            expect(lastVelocity()).toBe(0);
        });

        // R6
        describe('legacy scroll request', () => {
            it.each([
                { clientY: -20, direction: 'up' },
                { clientY: 850, direction: 'down' }
            ])(
                'should send it past the edge of the iframe (y=$clientY)',
                ({ clientY, direction }) => {
                    dragOver(clientY);

                    expect(legacyScrolls()).toBe(1);
                    expect(lastLegacyDirection()).toBe(direction);
                }
            );

            it('should keep sending it, throttled, beside the iframe while the page scrolls', () => {
                dragOver(750);
                vi.advanceTimersByTime(250);

                dragOverAt(BESIDE, 750);

                expect(legacyScrolls()).toBe(2);
                expect(lastLegacyDirection()).toBe('down');
            });

            it('should not start it beside the iframe', () => {
                dragOverAt(BESIDE, 750);

                expect(legacyScrolls()).toBe(0);
            });
        });
    });

    // A move drag starts on the handle in `dot-uve-contentlet-tools`, which is removed as soon as
    // the editor is in DRAGGING. The `dragend` then fires on a node outside the document and never
    // reaches `window`, so Escape or a release outside the window left the drag running.
    describe('the end of a drag that started in the editor', () => {
        const sources: HTMLElement[] = [];

        /** A move handle, as `dot-uve-contentlet-tools` renders one, in the document. */
        const moveHandle = (): HTMLElement => {
            const handle = document.createElement('button');
            handle.draggable = true;
            handle.dataset['type'] = 'contentlet';
            handle.dataset['item'] = JSON.stringify({
                contentlet: { identifier: 'moved', baseType: 'CONTENT', contentType: 'Banner' },
                container: { identifier: 'container', uuid: '1' },
                move: true
            });
            document.body.appendChild(handle);
            sources.push(handle);

            return handle;
        };

        const dragStartOn = (source: HTMLElement) => {
            source.dispatchEvent(new Event('dragstart', { bubbles: true }));
        };

        const dragEndOn = (source: HTMLElement) => {
            source.dispatchEvent(new Event('dragend', { bubbles: true }));
        };

        afterEach(() => {
            sources.splice(0).forEach((source) => source.remove());
        });

        it('should stop the scroll and end the drag when the handle left the page first', () => {
            const handle = moveHandle();
            dragStartOn(handle);
            dragOver(10);
            expect(lastVelocity()).toBeLessThan(0);

            handle.remove();
            dragEndOn(handle);

            expect(lastVelocity()).toBe(0);
            expect(handlers.onDragEnd).toHaveBeenCalledTimes(1);
        });

        it('should end the drag once when the drag source stays in the page', () => {
            const source = moveHandle();
            dragStartOn(source);
            dragOver(10);

            dragEndOn(source);

            expect(handlers.onDragEnd).toHaveBeenCalledTimes(1);
            expect(velocities().filter((velocity) => velocity === 0)).toHaveLength(1);
        });

        it('should stop listening on the drag source when the service goes away mid-drag', () => {
            const injector = createEnvironmentInjector(
                [DotUveDragDropService],
                TestBed.inject(EnvironmentInjector)
            );
            const ownHandlers = createHandlers();
            setUp(injector.get(DotUveDragDropService), ownHandlers);
            const handle = moveHandle();
            dragStartOn(handle);

            injector.destroy();
            handle.remove();
            dragEndOn(handle);

            expect(ownHandlers.onDragEnd).not.toHaveBeenCalled();
        });
    });

    describe('when the drag service goes away', () => {
        // The page scrolls on its own until told to stop, so leaving the editor mid-scroll must
        // stop it, or it runs to the page's limit.
        it('should ask the page to stop scrolling', () => {
            // A service of its own, in an injector the test can destroy, as leaving the canvas
            // destroys the canvas's. The shared one sends the same velocity and stays.
            const injector = createEnvironmentInjector(
                [DotUveDragDropService],
                TestBed.inject(EnvironmentInjector)
            );
            setUp(injector.get(DotUveDragDropService));

            dragOver(10);
            expect(lastVelocity()).toBeLessThan(0);

            injector.destroy();

            expect(lastVelocity()).toBe(0);
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
