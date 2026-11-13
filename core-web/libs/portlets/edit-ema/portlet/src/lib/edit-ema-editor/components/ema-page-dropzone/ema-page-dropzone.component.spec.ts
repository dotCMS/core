import {
    Spectator,
    SpectatorHost,
    byTestId,
    createComponentFactory,
    createHostFactory
} from '@openng/spectator/vitest';
import { vi } from 'vitest';

import { CommonModule } from '@angular/common';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Component } from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { EmaPageDropzoneComponent } from './ema-page-dropzone.component';
import { Container, ContentletArea, EmaDragItem, EmaDropTarget } from './types';

import {
    ITEM_MOCK,
    BOUNDS_MOCK,
    getBoundsMock,
    ACTION_MOCK,
    BOUNDS_EMPTY_CONTAINER_MOCK
} from '../../../shared/mocks';
import { ActionPayload, ContainerPayload, ContentletPayload } from '../../../shared/models';

/** Places the dropzone the way the canvas does, and keeps every drop target it reports. */
// Spectator declares the host in its testing module, so it cannot be standalone.
@Component({ selector: 'dot-test-dropzone-host', template: '', standalone: false })
class DropzoneHostComponent {
    containers: Container[] = [];
    dragItem: EmaDragItem | null = null;
    targets: EmaDropTarget[] = [];
}

const messageServiceMock = new MockDotMessageService({
    'edit.ema.page.dropzone.invalid.contentlet.type':
        'The contentlet type {0} is not valid for this container',
    'edit.ema.page.dropzone.max.contentlets': 'Container only accepts {0} contentlets',
    'edit.ema.page.dropzone.one.max.contentlet': 'Container only accepts one contentlet'
});

/** `element`, which the test needs on the page; a missing one fails the test by name. */
function rendered<T extends Element>(element: T | null | undefined, what: string): T {
    if (!element) {
        throw new Error(`Expected ${what} to be rendered`);
    }

    return element;
}

describe('EmaPageDropzoneComponent', () => {
    let spectator: Spectator<EmaPageDropzoneComponent>;
    let dotMessageService: DotMessageService;

    const createComponent = createComponentFactory({
        component: EmaPageDropzoneComponent,
        imports: [CommonModule, HttpClientTestingModule],
        providers: [
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                containers: [],
                dragItem: ITEM_MOCK
            }
        });
        dotMessageService = spectator.inject(DotMessageService, true);
    });

    it('should render containers, and contentlets based on input', () => {
        spectator.setInput('containers', BOUNDS_MOCK);
        expect(spectator.queryAll('[data-type="container"]')).toHaveLength(1);
        expect(spectator.queryAll('[data-type="contentlet"]')).toHaveLength(2);
    });

    describe('css', () => {
        it('should apply styles to container correctly', () => {
            spectator.setInput('containers', BOUNDS_MOCK);

            const element = spectator.query('[data-type="container"]');
            const style = getComputedStyle(element);

            expect(style.position).toEqual('absolute');
            expect(style.left).toEqual('10px');
            expect(style.top).toEqual('10px');
            expect(style.width).toEqual('980px');
            expect(style.height).toEqual('180px');
        });

        it('should apply styles to contentlet correctly', () => {
            spectator.setInput('containers', BOUNDS_MOCK);

            const element = spectator.query('[data-type="contentlet"]');
            const style = getComputedStyle(element);

            expect(style.position).toEqual('absolute');
            expect(style.left).toEqual('20px');
            expect(style.top).toEqual('20px');
            expect(style.width).toEqual('940px');
            expect(style.height).toEqual('140px');
        });
    });

    describe('events', () => {
        describe('contentlet', () => {
            it('should show drop zone error when container is full', () => {
                const spyDotMessageSerivice = vi.spyOn(dotMessageService, 'get');

                const NEW_BOUNDS_MOCK = getBoundsMock({
                    ...ACTION_MOCK,
                    container: {
                        ...ACTION_MOCK.container,
                        maxContentlets: 2
                    }
                });

                spectator.setInput('dragItem', ITEM_MOCK);
                spectator.setInput('containers', NEW_BOUNDS_MOCK);
                spectator.detectComponentChanges();

                spectator.triggerEventHandler('div.drop-zone_error', 'drop', {
                    target: {}
                });

                spectator.detectChanges();

                const errorZone = spectator.query('.drop-zone_error') as HTMLElement;
                const errorZoneText = errorZone.querySelector('span').textContent;

                const { left, top, width, height } = errorZone.style;
                const errorZoneReact = {
                    left,
                    top,
                    width,
                    height
                };

                // Check that the error message is displayed
                expect(errorZone).toBeTruthy();
                expect(errorZoneText.trim()).toBe('Container only accepts 2 contentlets');
                expect(errorZoneReact).toEqual({
                    left: '0px',
                    top: '0px',
                    width: '980px',
                    height: '180px'
                });

                // Check that the place event is not emitted
                expect(spyDotMessageSerivice).toHaveBeenCalledWith(
                    'edit.ema.page.dropzone.max.contentlets',
                    '2'
                );
            });

            it('should show one maximum content error when container is full and only allow one', () => {
                const spyDotMessageSerivice = vi.spyOn(dotMessageService, 'get');
                const NEW_BOUNDS_MOCK = getBoundsMock({
                    ...ACTION_MOCK,
                    container: {
                        ...ACTION_MOCK.container,
                        maxContentlets: 1
                    }
                });

                spectator.setInput('dragItem', ITEM_MOCK);
                spectator.setInput('containers', NEW_BOUNDS_MOCK);
                spectator.detectComponentChanges();

                spectator.triggerEventHandler('div.drop-zone_error', 'drop', {
                    target: {}
                });

                spectator.detectChanges();

                const errorZone = spectator.query('.drop-zone_error') as HTMLElement;
                const errorZoneText = errorZone.querySelector('span').textContent;

                const { left, top, width, height } = errorZone.style;
                const errorZoneReact = {
                    left,
                    top,
                    width,
                    height
                };

                // Check that the error message is displayed
                expect(errorZone).toBeTruthy();
                expect(errorZoneText.trim()).toBe('Container only accepts one contentlet');
                expect(errorZoneReact).toEqual({
                    left: '0px',
                    top: '0px',
                    width: '980px',
                    height: '180px'
                });

                expect(spyDotMessageSerivice).toHaveBeenCalledWith(
                    'edit.ema.page.dropzone.one.max.contentlet',
                    '1'
                );
            });

            it('should set pointer on drag over', () => {
                spectator.setInput('dragItem', ITEM_MOCK);
                spectator.setInput('containers', BOUNDS_MOCK);
                spectator.detectComponentChanges();

                // The container sits at y=10 and the first contentlet 20px into it, 140px tall:
                // y=130 is in its bottom half, so the line goes on its bottom edge.
                spectator.dispatchMouseEvent('div[data-type="contentlet"]', 'dragover', 100, 130);
                spectator.detectChanges();

                const { left, opacity, top, width, height } = rendered(
                    spectator.query<HTMLElement>(byTestId('dropzone-insertion-line')),
                    'the insertion line'
                ).style;

                expect({ left, opacity, top, width, height }).toEqual({
                    left: '30px',
                    opacity: '1',
                    top: '170px',
                    width: '940px',
                    height: '3px'
                });
            });
        });

        describe('empty container', () => {
            it('should show drop zone error when the contentType is not accepted in the container', () => {
                const spyDotMessageSerivice = vi.spyOn(dotMessageService, 'get');

                spectator.setInput('dragItem', {
                    ...ITEM_MOCK,
                    contentType: 'NOT_ACCEPTED_CONTENT_TYPE'
                });
                spectator.setInput('containers', BOUNDS_EMPTY_CONTAINER_MOCK);
                spectator.detectChanges();

                const errorZone = spectator.query('.drop-zone_error');
                const errorZoneText = errorZone.querySelector('span').textContent;

                // Check that the error message is displayed
                expect(errorZone).toBeTruthy();
                expect(errorZoneText.trim()).toBe(
                    'The contentlet type NOT_ACCEPTED_CONTENT_TYPE is not valid for this container'
                );

                expect(spyDotMessageSerivice).toHaveBeenCalledWith(
                    'edit.ema.page.dropzone.invalid.contentlet.type',
                    'NOT_ACCEPTED_CONTENT_TYPE'
                );
            });

            it('should set pointer on drag over', () => {
                spectator.setInput('dragItem', ITEM_MOCK);
                spectator.setInput('containers', BOUNDS_EMPTY_CONTAINER_MOCK);
                spectator.detectComponentChanges();

                spectator.dispatchMouseEvent('div[data-type="container"]', 'dragover', 100, 100);
                spectator.detectChanges();

                const { left, opacity, top, width, height } = rendered(
                    spectator.query<HTMLElement>(byTestId('dropzone-insertion-line')),
                    'the insertion line'
                ).style;

                // An empty container is highlighted whole.
                expect({ left, opacity, top, width, height }).toEqual({
                    left: '10px',
                    opacity: '0.1',
                    top: '10px',
                    width: '980px',
                    height: '180px'
                });
            });
        });
    });
});

// #38575: the dropzone is the one place that decides where a drop goes, so the insertion line and
// the drop always agree.
describe('EmaPageDropzoneComponent drop target', () => {
    let spectator: SpectatorHost<EmaPageDropzoneComponent, DropzoneHostComponent>;

    const createHost = createHostFactory({
        component: EmaPageDropzoneComponent,
        host: DropzoneHostComponent,
        imports: [CommonModule, HttpClientTestingModule],
        providers: [{ provide: DotMessageService, useValue: messageServiceMock }]
    });

    const CONTAINER_A: ContainerPayload = {
        identifier: 'container-a',
        uuid: '1',
        acceptTypes: 'Banner',
        maxContentlets: 25
    };

    const CONTAINER_B: ContainerPayload = { ...CONTAINER_A, identifier: 'container-b' };

    const CONTAINER_C: ContainerPayload = { ...CONTAINER_A, identifier: 'container-c' };

    const contentletOf = (identifier: string): ContentletPayload => ({
        identifier,
        inode: `${identifier}-inode`,
        title: identifier,
        contentType: 'Banner'
    });

    /** A full-width contentlet 200px tall, `y` px from the top of its container, as the SDK reports it. */
    const contentletArea = (
        container: ContainerPayload,
        identifier: string,
        y: number
    ): ContentletArea => ({
        x: 0,
        y,
        width: 1000,
        height: 200,
        // The SDK sends the payload as JSON, which the dropzone writes to `data-payload`.
        payload: JSON.stringify({
            container,
            contentlet: contentletOf(identifier)
        }) as unknown as ActionPayload
    });

    /**
     * Container A holds c1 (0–200), c2 (250–450) and c3 (500–700), with gaps between them and
     * 50px of space after c3. Container B, below it, is empty.
     */
    const CONTAINERS: Container[] = [
        {
            x: 0,
            y: 0,
            width: 1000,
            height: 750,
            contentlets: [
                contentletArea(CONTAINER_A, 'c1', 0),
                contentletArea(CONTAINER_A, 'c2', 250),
                contentletArea(CONTAINER_A, 'c3', 500)
            ],
            payload: JSON.stringify({ container: CONTAINER_A })
        },
        {
            x: 0,
            y: 800,
            width: 1000,
            height: 200,
            contentlets: [],
            payload: JSON.stringify({ container: CONTAINER_B })
        }
    ];

    /** Container C, below B, holds the same contentlet c2 that container A holds. */
    const CONTAINER_C_WITH_C2: Container = {
        x: 0,
        y: 1100,
        width: 1000,
        height: 200,
        contentlets: [contentletArea(CONTAINER_C, 'c2', 0)],
        payload: JSON.stringify({ container: CONTAINER_C })
    };

    const PALETTE_ITEM: EmaDragItem = {
        baseType: 'CONTENT',
        contentType: 'Banner',
        draggedPayload: {
            type: 'contentlet',
            move: false,
            item: { contentlet: contentletOf('palette-contentlet') }
        }
    };

    /** c2, picked up from container A to move it. */
    const MOVED_C2: EmaDragItem = {
        baseType: 'CONTENT',
        contentType: 'Banner',
        draggedPayload: {
            type: 'contentlet',
            move: true,
            item: { container: CONTAINER_A, contentlet: contentletOf('c2') }
        }
    };

    const PLACEHOLDER = 'dropzone-source-placeholder';

    const rect = (x: number, y: number, width: number, height: number): DOMRect =>
        ({
            x,
            y,
            left: x,
            top: y,
            width,
            height,
            right: x + width,
            bottom: y + height,
            toJSON: () => ({})
        }) as DOMRect;

    const containerElements = (): HTMLElement[] =>
        spectator.queryAll<HTMLElement>('[data-type="container"]');

    const contentletElements = (): HTMLElement[] =>
        spectator.queryAll<HTMLElement>('[data-type="contentlet"]');

    /**
     * The rendered contentlet whose payload names `identifier`, in the given container; none for
     * the moved contentlet, which is drawn as the placeholder instead.
     */
    const contentletElement = (
        identifier: string,
        container = CONTAINER_A
    ): HTMLElement | undefined =>
        contentletElements().find((element) => {
            const payload = JSON.parse(element.dataset['payload'] ?? '{}');

            return (
                payload.contentlet?.identifier === identifier &&
                payload.container?.identifier === container.identifier
            );
        });

    const insertionLine = (): HTMLElement =>
        rendered(spectator.query<HTMLElement>('.pointer'), 'the insertion line');

    /** The moved contentlet's placeholder; none while nothing on the page is moved. */
    const placeholder = (): HTMLElement | null =>
        spectator.query<HTMLElement>(byTestId(PLACEHOLDER));

    /** jsdom lays nothing out: give each drop target the rectangle the bounds describe, at zoom 1. */
    const layOut = () => {
        const containers = spectator.hostComponent.containers;

        vi.spyOn(spectator.element, 'getBoundingClientRect').mockReturnValue(
            rect(0, 0, 1000, 1400)
        );

        containerElements().forEach((element, index) => {
            const { x, y, width, height } = containers[index];
            vi.spyOn(element, 'getBoundingClientRect').mockReturnValue(rect(x, y, width, height));
        });

        containers.forEach((container) => {
            const { container: containerPayload } = JSON.parse(container.payload as string);

            container.contentlets.forEach((area) => {
                const { contentlet } = JSON.parse(area.payload as unknown as string);
                const element = contentletElement(contentlet.identifier, containerPayload);

                if (element) {
                    vi.spyOn(element, 'getBoundingClientRect').mockReturnValue(
                        rect(container.x + area.x, container.y + area.y, area.width, area.height)
                    );
                }
            });
        });

        // c2's original place in container A, when it is the one being moved.
        const source = placeholder();

        if (source) {
            vi.spyOn(source, 'getBoundingClientRect').mockReturnValue(rect(0, 250, 1000, 200));
        }
    };

    /** The pointer moves over `element` at `clientY`, mid-way across the page. */
    const dragOver = (element: HTMLElement | null | undefined, clientY: number) => {
        layOut();
        spectator.dispatchMouseEvent(
            rendered(element, 'the drop target'),
            'dragover',
            500,
            clientY
        );
        spectator.detectChanges();
    };

    const lastTarget = (): EmaDropTarget | undefined => spectator.hostComponent.targets.at(-1);

    const startDrag = (dragItem: EmaDragItem, containers: Container[] = CONTAINERS) => {
        spectator = createHost(
            `<dot-ema-page-dropzone
                [containers]="containers"
                [dragItem]="dragItem"
                (dropTargetChange)="targets.push($event)" />`,
            { hostProps: { containers, dragItem, targets: [] } }
        );
    };

    describe('while a contentlet from the palette is dragged', () => {
        beforeEach(() => startDrag(PALETTE_ITEM));

        it('should resolve to before a contentlet when the pointer is over its top half', () => {
            dragOver(contentletElement('c2'), 300);

            expect(lastTarget()).toEqual({
                kind: 'contentlet',
                payload: { container: CONTAINER_A, contentlet: contentletOf('c2') },
                position: 'before'
            });
        });

        it('should resolve to after a contentlet when the pointer is over its bottom half', () => {
            dragOver(contentletElement('c2'), 400);

            expect(lastTarget()).toEqual({
                kind: 'contentlet',
                payload: { container: CONTAINER_A, contentlet: contentletOf('c2') },
                position: 'after'
            });
        });

        it.each([
            { clientY: 210, position: 'after', pivot: 'c1' },
            { clientY: 240, position: 'before', pivot: 'c2' },
            { clientY: 460, position: 'after', pivot: 'c2' },
            { clientY: 730, position: 'after', pivot: 'c3' }
        ])(
            'should resolve the space between contentlets at y=$clientY to $position $pivot, the contentlet with the nearest centre',
            ({ clientY, position, pivot }) => {
                dragOver(containerElements()[0], clientY);

                expect(lastTarget()).toEqual({
                    kind: 'contentlet',
                    payload: { container: CONTAINER_A, contentlet: contentletOf(pivot) },
                    position
                });
            }
        );

        it('should resolve a container with no contentlets to the empty container', () => {
            dragOver(containerElements()[1], 900);

            expect(lastTarget()).toEqual({
                kind: 'empty-container',
                payload: { container: CONTAINER_B }
            });
        });

        it('should mark only a container with no contentlets as empty', () => {
            const [withContentlets, empty] = containerElements();

            expect(withContentlets.getAttribute('data-empty')).toBe('false');
            expect(empty.getAttribute('data-empty')).toBe('true');
        });

        it('should draw the insertion line at the edge of the nearest contentlet when the pointer is between contentlets', () => {
            // Nearest is c2, above its centre: the line goes on c2's top edge.
            dragOver(containerElements()[0], 240);

            const line = insertionLine();

            expect(line.style.top).toBe('250px');
            expect(line.style.height).toBe('3px');
            expect(line.style.opacity).toBe('1');
        });

        it('should draw no placeholder', () => {
            expect(spectator.queryAll(byTestId(PLACEHOLDER))).toHaveLength(0);
        });
    });

    describe('while a contentlet on the page is moved', () => {
        beforeEach(() => startDrag(MOVED_C2));

        it('should draw a placeholder over its original position', () => {
            const placeholders = spectator.queryAll<HTMLElement>(byTestId(PLACEHOLDER));

            expect(placeholders).toHaveLength(1);
            expect(placeholders[0].style.left).toBe('0px');
            expect(placeholders[0].style.top).toBe('250px');
            expect(placeholders[0].style.width).toBe('1000px');
            expect(placeholders[0].style.height).toBe('200px');
        });

        it('should draw the placeholder only in the container it came from, not where the same contentlet appears again', () => {
            spectator.setHostInput({ containers: [...CONTAINERS, CONTAINER_C_WITH_C2] });

            expect(spectator.queryAll(byTestId(PLACEHOLDER))).toHaveLength(1);
            expect(placeholder()?.closest('[data-type="container"]')).toBe(containerElements()[0]);
        });

        it('should resolve to no target and hide the insertion line when the pointer is over the placeholder', () => {
            // A real target first, so the line is showing.
            dragOver(contentletElement('c3'), 650);
            expect(insertionLine().style.opacity).toBe('1');

            expect(placeholder()).not.toBeNull();
            dragOver(placeholder(), 300);

            expect(lastTarget()).toEqual(expect.objectContaining({ kind: 'none' }));
            expect(insertionLine().style.opacity).toBe('0');
        });

        it.each([
            { where: 'the bottom half of the contentlet above it', over: 'c1', clientY: 150 },
            { where: 'the top half of the contentlet below it', over: 'c3', clientY: 550 },
            { where: 'the space right above it', over: 'gap', clientY: 210 },
            { where: 'the space right below it', over: 'gap', clientY: 490 }
        ])(
            'should resolve to no target and hide the insertion line over $where, which leaves the order as it is',
            ({ over, clientY }) => {
                dragOver(contentletElement('c3'), 650);
                expect(insertionLine().style.opacity).toBe('1');

                dragOver(
                    over === 'gap' ? containerElements()[0] : contentletElement(over),
                    clientY
                );

                expect(lastTarget()).toEqual(expect.objectContaining({ kind: 'none' }));
                expect(insertionLine().style.opacity).toBe('0');
            }
        );
    });

    // Review of #38575: while the page scrolls under a still pointer, new bounds arrive with no
    // new dragover. The line and the target must follow the blocks, not stay where they were.
    describe('when the page scrolls under a still pointer', () => {
        /** The same containers, moved `dy` px, as the store shifts them while the page scrolls. */
        const scrolledBy = (dy: number, containers: Container[] = CONTAINERS): Container[] =>
            containers.map((container) => ({ ...container, y: container.y + dy }));

        const pageScrolls = (dy: number) => {
            spectator.setHostInput({ containers: scrolledBy(dy) });
            spectator.detectChanges();
        };

        describe('while a contentlet from the palette is dragged', () => {
            beforeEach(() => startDrag(PALETTE_ITEM));

            it('should move the line to the edge of the contentlet now under the pointer and report it', () => {
                // Top half of c2: the line is on c2's top edge.
                dragOver(contentletElement('c2'), 300);
                expect(insertionLine().style.top).toBe('250px');

                // The page scrolls 220px down: c3 (500–700 in its container) is now at 280–480,
                // and the pointer at 300 is over its top half.
                pageScrolls(-220);

                expect(lastTarget()).toEqual({
                    kind: 'contentlet',
                    payload: { container: CONTAINER_A, contentlet: contentletOf('c3') },
                    position: 'before'
                });
                expect(insertionLine().style.top).toBe('280px');
                expect(insertionLine().style.opacity).toBe('1');
            });

            it('should not resolve anything before the pointer has been over a target', () => {
                pageScrolls(-220);

                expect(spectator.hostComponent.targets).toEqual([]);
                expect(insertionLine().style.opacity).toBe('0');
            });

            it('should not resolve anything once the pointer has left the dropzone', () => {
                dragOver(contentletElement('c2'), 300);
                spectator.dispatchMouseEvent(spectator.element, 'dragleave');
                spectator.detectChanges();
                const reported = spectator.hostComponent.targets.length;

                pageScrolls(-220);

                expect(spectator.hostComponent.targets).toHaveLength(reported);
                expect(lastTarget()).toEqual(expect.objectContaining({ kind: 'none' }));
                expect(insertionLine().style.opacity).toBe('0');
            });
        });

        describe('while a contentlet on the page is moved', () => {
            beforeEach(() => startDrag(MOVED_C2));

            it('should resolve to no target and hide the line once its own placeholder is under the pointer', () => {
                dragOver(contentletElement('c3'), 650);
                expect(lastTarget()).toEqual(expect.objectContaining({ kind: 'contentlet' }));

                // The page scrolls 300px up: c2's place (250–450) is now at 550–750, under 650.
                pageScrolls(300);

                expect(lastTarget()).toEqual(expect.objectContaining({ kind: 'none' }));
                expect(insertionLine().style.opacity).toBe('0');
            });
        });
    });
});
