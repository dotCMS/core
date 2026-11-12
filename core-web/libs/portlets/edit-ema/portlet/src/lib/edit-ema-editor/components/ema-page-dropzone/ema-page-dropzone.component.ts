import {
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    computed,
    effect,
    inject,
    input,
    output,
    signal,
    untracked
} from '@angular/core';

import { DotMessagePipe } from '@dotcms/ui';

import { DotErrorPipe } from './pipes/error/dot-error.pipe';
import { DotPositionPipe } from './pipes/position/dot-position.pipe';
import { Container, ContentletArea, EmaDragItem, EmaDropTarget } from './types';

import { ClientData } from '../../../shared/models';
import { doesMoveChangeOrder } from '../../../utils';

/** Whether a container refuses the drag; the template draws its error zone from the same pipe. */
const DOT_ERROR = new DotErrorPipe();

const POINTER_INITIAL_POSITION: Record<string, string> = {
    left: '0',
    width: '0',
    opacity: '0',
    top: '0'
};

// Height in pixels for the drop indicator when container is not empty
const DROP_INDICATOR_HEIGHT_PX = 3;

const NO_TARGET: EmaDropTarget = { kind: 'none' };

/** A contentlet of a container, with its payload read once. */
interface DropzoneContentlet {
    area: ContentletArea;
    payload: ClientData;
    /** The JSON written to `data-payload`. */
    payloadAttribute: string;
    /** Whether it is the contentlet being moved, in the container it is moved from. */
    isSource: boolean;
}

/** A container of the page, with its payload read once. */
interface DropzoneContainer {
    container: Container;
    payload: ClientData;
    /** The JSON written to `data-payload`. */
    payloadAttribute: string;
    contentlets: DropzoneContentlet[];
}

/**
 * The drop targets drawn over the page while something is dragged. It is the one place that
 * decides where a drop goes: it resolves the target under the pointer, draws the insertion line
 * there, and reports the target to the canvas, which drops on it. A contentlet being moved shows
 * as a gray placeholder at its original place, which is not a target.
 */
@Component({
    selector: 'dot-ema-page-dropzone',
    imports: [DotPositionPipe, DotErrorPipe, DotMessagePipe],
    templateUrl: './ema-page-dropzone.component.html',
    styleUrls: ['./ema-page-dropzone.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        '(dragover)': 'onHostDragover($event)',
        '(dragleave)': 'onHostDragleave($event)'
    }
})
export class EmaPageDropzoneComponent {
    containers = input<Container[]>([]);
    dragItem = input<EmaDragItem | null>(null);
    zoomLevel = input<number>(1);

    /**
     * Where a drop would go, each time it changes. Named like the inputs above, without the `$`
     * prefix: outputs cannot be aliased, and the canvas binds `dropTargetChange`.
     */
    readonly dropTargetChange = output<EmaDropTarget>();

    readonly #el = inject(ElementRef<HTMLElement>);

    /** The last target reported, so a target is reported once, not on every `dragover`. */
    #lastTarget: EmaDropTarget | null = null;

    /**
     * Where the pointer last was over the dropzone, in admin-window coordinates; null before
     * the first dragover and after the pointer left.
     */
    #pointer: { clientX: number; clientY: number } | null = null;

    /** Where the insertion line is drawn. */
    protected readonly $pointerPosition = signal<Record<string, string>>(POINTER_INITIAL_POSITION);

    /**
     * The containers and contentlets to draw, with their payloads read. The contentlet being
     * moved is marked as the source, only in the container it is moved from: the same contentlet
     * can sit in another container too.
     */
    protected readonly $containers = computed<DropzoneContainer[]>(() => {
        const draggedPayload = this.dragItem()?.draggedPayload;
        const source =
            draggedPayload?.type === 'contentlet' && draggedPayload.move
                ? draggedPayload.item
                : null;

        return this.containers().map((container) => {
            const payload = readPayload(container.payload);
            const isSourceContainer =
                !!source?.container &&
                source.container.identifier === payload?.container?.identifier &&
                source.container.uuid === payload?.container?.uuid;

            return {
                container,
                payload,
                payloadAttribute: writePayload(container.payload),
                contentlets: container.contentlets.map((area) => {
                    const contentletPayload = readPayload(area.payload);

                    return {
                        area,
                        payload: contentletPayload,
                        payloadAttribute: writePayload(area.payload),
                        isSource:
                            isSourceContainer &&
                            contentletPayload?.contentlet?.identifier ===
                                source.contentlet?.identifier
                    };
                })
            };
        });
    });

    constructor() {
        // New bounds arrive with no dragover while the page scrolls under a still pointer: the
        // line and the target follow the blocks instead of staying where they were.
        effect(() => {
            const containers = this.$containers();

            untracked(() => this.#resolveAgain(containers));
        });
    }

    /**
     * Resolve the drop target under the pointer in a container, draw the insertion line there and
     * report the target. Over a contentlet, the target is before or after it, by which half the
     * pointer is in. Between contentlets, it is the contentlet whose vertical centre is nearest.
     * The moved contentlet's own place, and the places right next to it, are no target.
     *
     * @param {DragEvent} event
     * @param {DropzoneContainer} container the container the pointer is in
     * @memberof EmaPageDropzoneComponent
     */
    onDragover(event: DragEvent, container: DropzoneContainer): void {
        const target = event.target as HTMLElement;

        // The error zone over a container that refuses the drag is not a target.
        if (target.dataset?.['dropzone'] !== 'true') {
            this.#clearTarget();

            return;
        }

        const pointer = this.#toBounds(event.clientX, event.clientY);

        this.#resolveIn(container, pointer.x, pointer.y);
    }

    /**
     * Resolve the target in `container` for a pointer at (`pointerX`, `pointerY`) in the page's
     * unzoomed coordinates, draw the insertion line there and report the target.
     */
    #resolveIn(container: DropzoneContainer, pointerX: number, pointerY: number): void {
        const { x, y, width, height } = container.container;

        if (!container.contentlets.length) {
            this.$pointerPosition.set({
                left: `${x}px`,
                width: `${width}px`,
                opacity: '0.1',
                top: `${y}px`,
                height: `${height}px`
            });
            this.#report({ kind: 'empty-container', payload: container.payload });

            return;
        }

        // Contentlet bounds are relative to their container.
        const localX = pointerX - x;
        const localY = pointerY - y;

        const pivot =
            container.contentlets.find(({ area }) => isInside(area, localX, localY)) ??
            nearestByCentre(container.contentlets, localY);
        const position = localY < pivot.area.y + pivot.area.height / 2 ? 'before' : 'after';

        if (leavesOrderUnchanged(container.contentlets, pivot, position)) {
            this.#clearTarget();

            return;
        }

        const { area } = pivot;

        this.$pointerPosition.set({
            left: `${x + area.x}px`,
            width: `${area.width}px`,
            opacity: '1',
            top: `${y + area.y + (position === 'after' ? area.height : 0)}px`,
            height: `${DROP_INDICATOR_HEIGHT_PX}px`
        });
        this.#report({ kind: 'contentlet', payload: pivot.payload, position });
    }

    /**
     * The pointer is over the dropzone itself, outside every container: no target.
     *
     * @param {DragEvent} event
     * @memberof EmaPageDropzoneComponent
     */
    onHostDragover(event: DragEvent): void {
        // Every dragover over the dropzone reaches the host, so this is where the pointer is.
        this.#pointer = { clientX: event.clientX, clientY: event.clientY };

        if (event.target === this.#el.nativeElement) {
            this.#clearTarget();
        }
    }

    /**
     * The pointer left the dropzone: no target, and nothing to resolve again until it is back.
     *
     * @param {DragEvent} event
     * @memberof EmaPageDropzoneComponent
     */
    onHostDragleave(event: DragEvent): void {
        const next = event.relatedTarget as Node | null;

        if (!next || !this.#el.nativeElement.contains(next)) {
            this.#pointer = null;
            this.#clearTarget();
        }
    }

    /**
     * The bounds changed with no new dragover, as when the page scrolls under a still pointer:
     * resolve the target again where the pointer was, so the line and the target follow the
     * blocks. Nothing to do before the first dragover or after the pointer left.
     */
    #resolveAgain(containers: DropzoneContainer[]): void {
        if (!this.#pointer) {
            return;
        }

        const { x, y } = this.#toBounds(this.#pointer.clientX, this.#pointer.clientY);
        const container = containers.find(({ container: bounds }) => isInside(bounds, x, y));

        // Over no container, or over one that refuses the drag (its error zone covers it).
        if (
            !container ||
            DOT_ERROR.transform(container.container, this.dragItem() ?? undefined).message
        ) {
            this.#clearTarget();

            return;
        }

        this.#resolveIn(container, x, y);
    }

    /** A point in the admin window, in the page's unzoomed coordinates the bounds use. */
    #toBounds(clientX: number, clientY: number): { x: number; y: number } {
        const zoom = this.zoomLevel();
        const hostRect = this.#el.nativeElement.getBoundingClientRect();

        return {
            x: (clientX - hostRect.left) / zoom,
            y: (clientY - hostRect.top) / zoom
        };
    }

    /** Hide the insertion line and report that a drop would go nowhere. */
    #clearTarget(): void {
        this.$pointerPosition.set(POINTER_INITIAL_POSITION);
        this.#report(NO_TARGET);
    }

    /** Report the target when it is not the one reported last. */
    #report(target: EmaDropTarget): void {
        if (this.#lastTarget && isSameTarget(this.#lastTarget, target)) {
            return;
        }

        this.#lastTarget = target;
        this.dropTargetChange.emit(target);
    }
}

/** The page sends payloads as JSON; tests and older paths may hand the object itself. */
function readPayload(payload: unknown): ClientData {
    return typeof payload === 'string' ? JSON.parse(payload) : (payload as ClientData);
}

function writePayload(payload: unknown): string {
    return typeof payload === 'string' ? payload : JSON.stringify(payload ?? null);
}

function isInside(
    area: Pick<ContentletArea, 'x' | 'y' | 'width' | 'height'>,
    x: number,
    y: number
): boolean {
    return x >= area.x && x <= area.x + area.width && y >= area.y && y <= area.y + area.height;
}

function nearestByCentre(contentlets: DropzoneContentlet[], y: number): DropzoneContentlet {
    const distance = ({ area }: DropzoneContentlet) => Math.abs(y - (area.y + area.height / 2));

    return contentlets.reduce((nearest, contentlet) =>
        distance(contentlet) < distance(nearest) ? contentlet : nearest
    );
}

/**
 * Whether dropping next to `pivot` puts the moved contentlet back where it is: on itself, right
 * after the contentlet above it, or right before the one below it. The rule is the canvas's,
 * `doesMoveChangeOrder`, applied to the container's contentlets in the order the page draws them.
 */
function leavesOrderUnchanged(
    contentlets: DropzoneContentlet[],
    pivot: DropzoneContentlet,
    position: 'before' | 'after'
): boolean {
    const source = contentlets.find(({ isSource }) => isSource);

    if (!source) {
        return false;
    }

    const identifierOf = ({ payload }: DropzoneContentlet) => payload?.contentlet?.identifier;

    return !doesMoveChangeOrder({
        contentletsId: contentlets.map(identifierOf),
        contentletId: identifierOf(source),
        pivotId: identifierOf(pivot),
        position
    });
}

function isSameTarget(a: EmaDropTarget, b: EmaDropTarget): boolean {
    if (a.kind === 'none' || b.kind === 'none') {
        return a.kind === b.kind;
    }

    return (
        a.kind === b.kind &&
        a.payload === b.payload &&
        (a.kind === 'contentlet' ? a.position : undefined) ===
            (b.kind === 'contentlet' ? b.position : undefined)
    );
}
