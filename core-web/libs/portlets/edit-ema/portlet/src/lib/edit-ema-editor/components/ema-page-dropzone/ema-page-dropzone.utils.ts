import { ContentletArea, DropzoneContentlet, EmaDropTarget } from './types';

import { areContainersEquals, doesMoveChangeOrder } from '../../../utils';

/** The JSON for `data-payload`: the page sends it as JSON; tests and older paths hand objects. */
export function writePayload(payload: unknown): string {
    return typeof payload === 'string' ? payload : JSON.stringify(payload ?? null);
}

/** Whether the point (`x`, `y`) is inside `area`, edges included. */
export function isInside(
    area: Pick<ContentletArea, 'x' | 'y' | 'width' | 'height'>,
    x: number,
    y: number
): boolean {
    return x >= area.x && x <= area.x + area.width && y >= area.y && y <= area.y + area.height;
}

/**
 * The contentlet whose block is nearest the point (`x`, `y`): 0 inside a block, otherwise the
 * distance to its nearest edge, so blocks laid out in a row or in a column both work. On a tie
 * the earlier contentlet wins.
 */
export function nearestToPoint(
    contentlets: DropzoneContentlet[],
    x: number,
    y: number
): DropzoneContentlet {
    const distance = ({ area }: DropzoneContentlet) =>
        Math.hypot(
            Math.max(area.x - x, 0, x - (area.x + area.width)),
            Math.max(area.y - y, 0, y - (area.y + area.height))
        );

    return contentlets.reduce((nearest, contentlet) =>
        distance(contentlet) < distance(nearest) ? contentlet : nearest
    );
}

/**
 * Whether dropping next to `pivot` puts the moved contentlet back where it is: on itself, right
 * after the contentlet above it, or right before the one below it. The rule is the canvas's,
 * `doesMoveChangeOrder`, applied to the container's contentlets in the order the page draws them.
 */
export function leavesOrderUnchanged(
    contentlets: DropzoneContentlet[],
    pivot: DropzoneContentlet,
    position: 'before' | 'after'
): boolean {
    const identifierOf = ({ payload }: DropzoneContentlet) => payload?.contentlet?.identifier;
    const source = contentlets.find(({ isSource }) => isSource);
    const sourceId = source && identifierOf(source);

    if (!sourceId) {
        return false;
    }

    return !doesMoveChangeOrder({
        // The container's order holds identifiers only, so an area without one is not in it.
        contentletsId: contentlets.map(identifierOf).filter((id): id is string => !!id),
        contentletId: sourceId,
        pivotId: identifierOf(pivot),
        position
    });
}

/**
 * Whether two targets are the same place: the same kind, in the same container, and for a
 * contentlet the same contentlet and side. Payloads are compared by what they name, not by
 * reference, so a payload read again is still the same target.
 */
export function isSameTarget(a: EmaDropTarget, b: EmaDropTarget): boolean {
    if (a.kind === 'none' || b.kind === 'none') {
        return a.kind === b.kind;
    }

    const sameContainer =
        !!a.payload?.container && areContainersEquals(a.payload.container, b.payload?.container);

    if (a.kind === 'contentlet' && b.kind === 'contentlet') {
        return (
            sameContainer &&
            a.payload?.contentlet?.identifier === b.payload?.contentlet?.identifier &&
            a.position === b.position
        );
    }

    return a.kind === b.kind && sameContainer;
}
