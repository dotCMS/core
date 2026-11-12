import {
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    computed,
    inject,
    input,
    output,
    signal
} from '@angular/core';

import { DotMessagePipe } from '@dotcms/ui';

import { DotErrorPipe } from './pipes/error/dot-error.pipe';
import { DotPositionPipe } from './pipes/position/dot-position.pipe';
import { Container, ContentletArea, EmaDragItem, EmaDropTarget } from './types';

import { ClientData } from '../../../shared/models';

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
 * there, and reports the target to the canvas, which drops on it.
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

    /** Where the insertion line is drawn. */
    protected readonly $pointerPosition = signal<Record<string, string>>(POINTER_INITIAL_POSITION);

    /** The containers and contentlets to draw, with their payloads read. */
    protected readonly $containers = computed<DropzoneContainer[]>(() =>
        this.containers().map((container) => ({
            container,
            payload: readPayload(container.payload),
            payloadAttribute: writePayload(container.payload),
            contentlets: container.contentlets.map((area) => ({
                area,
                payload: readPayload(area.payload),
                payloadAttribute: writePayload(area.payload)
            }))
        }))
    );

    /**
     * Resolve the drop target under the pointer in a container, draw the insertion line there and
     * report the target. Over a contentlet, the target is before or after it, by which half the
     * pointer is in. Between contentlets, it is the contentlet whose vertical centre is nearest.
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

        const zoom = this.zoomLevel();
        const hostRect = this.#el.nativeElement.getBoundingClientRect();
        // Bounds come from the page, unzoomed and relative to their container.
        const pointerX = (event.clientX - hostRect.left) / zoom - x;
        const pointerY = (event.clientY - hostRect.top) / zoom - y;

        const pivot =
            container.contentlets.find(({ area }) => isInside(area, pointerX, pointerY)) ??
            nearestByCentre(container.contentlets, pointerY);
        const position = pointerY < pivot.area.y + pivot.area.height / 2 ? 'before' : 'after';
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
        if (event.target === this.#el.nativeElement) {
            this.#clearTarget();
        }
    }

    /**
     * The pointer left the dropzone: no target.
     *
     * @param {DragEvent} event
     * @memberof EmaPageDropzoneComponent
     */
    onHostDragleave(event: DragEvent): void {
        const next = event.relatedTarget as Node | null;

        if (!next || !this.#el.nativeElement.contains(next)) {
            this.#clearTarget();
        }
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

function isInside(area: ContentletArea, x: number, y: number): boolean {
    return x >= area.x && x <= area.x + area.width && y >= area.y && y <= area.y + area.height;
}

function nearestByCentre(contentlets: DropzoneContentlet[], y: number): DropzoneContentlet {
    const distance = ({ area }: DropzoneContentlet) => Math.abs(y - (area.y + area.height / 2));

    return contentlets.reduce((nearest, contentlet) =>
        distance(contentlet) < distance(nearest) ? contentlet : nearest
    );
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
