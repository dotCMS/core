import {
    initialAdjustState,
    initialCropState,
    initialFileInfoState,
    initialTransformState
} from './image-editor.state';

import { SLICE_KEYS } from '../image-editor.constants';
import {
    AdjustState,
    CropState,
    EditableSlices,
    FileInfoState,
    FilterCategory,
    ImageEditorAssetContext,
    ImageEditorHistoryEntry,
    ImageEditorOpenParams,
    ImageEditorState,
    SlicePatch,
    TransformState
} from '../models/image-editor.models';

/**
 * Pure helpers for the {@link ImageEditorStore}: history coalescing/replay,
 * asset-context construction, slice-patch reducers and small formatters. Kept out
 * of the store file so it stays focused on the signalStore definition; every
 * function here is side-effect-free and unit-testable in isolation.
 */

/** The pristine values of the editable slices, used to seed and reset history. */
export const initialEditableSlices: EditableSlices = {
    adjust: initialAdjustState,
    transform: initialTransformState,
    crop: initialCropState,
    fileInfo: initialFileInfoState
};

/** Extracts the editable slices from the full state for snapshotting. */
export function editableSlicesOf(state: ImageEditorState): EditableSlices {
    return {
        adjust: state.adjust,
        transform: state.transform,
        crop: state.crop,
        fileInfo: state.fileInfo
    };
}

/**
 * Coalesces a new edit into the undo/redo history. When the current head entry
 * shares the edit's coalesce key (the control identity, e.g. `brightness`) the
 * entry is updated in place (so dragging a single slider produces one history
 * step) and any redo tail is discarded; otherwise a new entry is appended — also
 * discarding any redo tail. Switching to a different control in the same
 * {@link FilterCategory} therefore starts a new entry rather than overwriting the
 * previous one. Either way a new value invalidates forward history built against
 * the old one.
 * @param state - The current editor state
 * @param category - The category the edit belongs to (used for grouping/labels)
 * @param label - The human-readable label for the entry
 * @param snapshot - The editable slices to capture
 * @param coalesceKey - The control identity edits merge on; defaults to `category`
 * @returns The new `history` array and `historyIndex`
 */
/** Process-local monotonic sequence backing unique, collision-free history-entry ids. */
let historyEntrySeq = 0;

export function coalesceHistory(
    state: ImageEditorState,
    category: FilterCategory,
    label: string,
    snapshot: EditableSlices,
    coalesceKey: string = category
): Pick<ImageEditorState, 'history' | 'historyIndex'> {
    const head = state.history[state.historyIndex];

    if (head && head.coalesceKey === coalesceKey) {
        // Update the head in place AND drop any redo tail: a new value for the same
        // control invalidates forward history built against the old value (matching
        // the redo-tail truncation the new-entry branch below performs).
        const history = [
            ...state.history.slice(0, state.historyIndex),
            { ...head, label, snapshot }
        ];

        return { history, historyIndex: state.historyIndex };
    }

    const entry: ImageEditorHistoryEntry = {
        // A monotonic counter keeps ids collision-free even for two entries created
        // in the same millisecond (Date.now() could collide, e.g. under fake timers).
        id: `${category}-${++historyEntrySeq}`,
        category,
        coalesceKey,
        label,
        snapshot
    };
    const history = [...state.history.slice(0, state.historyIndex + 1), entry];

    return { history, historyIndex: history.length - 1 };
}

/** Builds a partial state that restores the editable slices from a snapshot. */
export function restoreSlices(snapshot: EditableSlices): EditableSlices {
    return {
        adjust: snapshot.adjust,
        transform: snapshot.transform,
        crop: snapshot.crop,
        fileInfo: snapshot.fileInfo
    };
}

/** Resolves the editable slices for a given history index, or initial when empty. */
export function slicesAtIndex(history: ImageEditorHistoryEntry[], index: number): EditableSlices {
    return index < 0
        ? restoreSlices(initialEditableSlices)
        : restoreSlices(history[index].snapshot);
}

/**
 * The field-level changes in `next` relative to `prev`, grouped by slice. The
 * slices are flat objects of primitives, so a shallow per-field compare captures
 * exactly what an edit changed — including the cross-slice resets that crop/resize
 * exclusivity produces.
 */
function diffSlices(next: EditableSlices, prev: EditableSlices): SlicePatch {
    const patch: Record<string, Record<string, unknown>> = {};

    for (const key of SLICE_KEYS) {
        const nextFields = next[key] as unknown as Record<string, unknown>;
        const prevFields = prev[key] as unknown as Record<string, unknown>;
        const fieldPatch: Record<string, unknown> = {};

        for (const field of Object.keys(nextFields)) {
            if (nextFields[field] !== prevFields[field]) {
                fieldPatch[field] = nextFields[field];
            }
        }

        if (Object.keys(fieldPatch).length > 0) {
            patch[key] = fieldPatch;
        }
    }

    return patch as SlicePatch;
}

/** Merges a field-level patch over a base set of slices. */
function applySlicePatch(base: EditableSlices, patch: SlicePatch): EditableSlices {
    return {
        adjust: { ...base.adjust, ...patch.adjust },
        transform: { ...base.transform, ...patch.transform },
        crop: { ...base.crop, ...patch.crop },
        fileInfo: { ...base.fileInfo, ...patch.fileInfo }
    };
}

/**
 * Removes the entry at `removedIdx` and replays the survivors. Each entry's own
 * change is recovered as a field-level delta against its predecessor; dropping the
 * removed delta and re-folding the rest from the initial state rebuilds every
 * surviving entry's cumulative snapshot. This keeps a removed edit's effect from
 * lingering in later snapshots (the bug a plain `filter` left behind) and keeps
 * undo/redo correct afterwards.
 */
export function rebuildHistory(
    history: ImageEditorHistoryEntry[],
    removedIdx: number
): ImageEditorHistoryEntry[] {
    const deltas = history.map((entry, index) =>
        diffSlices(
            entry.snapshot,
            index === 0 ? initialEditableSlices : history[index - 1].snapshot
        )
    );

    let accumulated = restoreSlices(initialEditableSlices);

    return history
        .filter((_, index) => index !== removedIdx)
        .map((entry, keptIndex) => {
            const originalIdx = keptIndex < removedIdx ? keptIndex : keptIndex + 1;
            accumulated = applySlicePatch(accumulated, deltas[originalIdx]);

            return { ...entry, snapshot: accumulated };
        });
}

/** Produces the asset context for a freshly requested asset. */
export function contextFromParams(params: ImageEditorOpenParams): ImageEditorAssetContext {
    // Use `||` (not `??`) so an empty-string tempId falls through to the inode — callers
    // such as the binary field default tempId to '' when no upload has happened.
    const idOrTempId = params.tempId || params.inode || '';
    const byInode = params.byInode ?? false;

    return {
        idOrTempId,
        inode: params.inode ?? null,
        tempId: params.tempId ?? null,
        variable: params.variable,
        fieldName: params.fieldName,
        fileName: params.fileName ?? '',
        mimeType: params.mimeType ?? '',
        isTempFile: !!params.tempId,
        byInode,
        naturalWidth: 0,
        naturalHeight: 0,
        // The /contentAsset/image/ path segment is the field VARIABLE (the canonical id
        // the server resolves), not the display name.
        originalUrl: `/contentAsset/image/${idOrTempId}/${params.variable}`
    };
}

/** Applies an adjust-slice edit, bumps the cache and coalesces history. */
export function adjustPatch(
    state: ImageEditorState,
    adjust: AdjustState,
    category: FilterCategory,
    label: string,
    coalesceKey: string = category
): ImageEditorState {
    const next: ImageEditorState = {
        ...state,
        adjust,
        previewStatus: 'loading',
        cacheBust: state.cacheBust + 1
    };

    return {
        ...next,
        ...coalesceHistory(next, category, label, editableSlicesOf(next), coalesceKey)
    };
}

/** Applies a transform-slice edit, bumps the cache and coalesces history. */
export function transformPatch(
    state: ImageEditorState,
    transform: TransformState,
    crop: CropState,
    category: FilterCategory,
    label: string,
    coalesceKey: string = category
): ImageEditorState {
    const next: ImageEditorState = {
        ...state,
        transform,
        crop,
        previewStatus: 'loading',
        cacheBust: state.cacheBust + 1
    };

    return {
        ...next,
        ...coalesceHistory(next, category, label, editableSlicesOf(next), coalesceKey)
    };
}

/** Applies a fileInfo-slice edit, bumps the cache and coalesces history. */
export function fileInfoPatch(
    state: ImageEditorState,
    fileInfo: FileInfoState,
    category: FilterCategory,
    label: string,
    coalesceKey: string = category
): ImageEditorState {
    const next: ImageEditorState = {
        ...state,
        fileInfo,
        previewStatus: 'loading',
        cacheBust: state.cacheBust + 1
    };

    return {
        ...next,
        ...coalesceHistory(next, category, label, editableSlicesOf(next), coalesceKey)
    };
}

/** Extracts a readable message from an unknown error payload. */
export function errorMessage(payload: unknown, fallback: string): string {
    if (payload instanceof Error) {
        return payload.message;
    }

    if (typeof payload === 'string') {
        return payload;
    }

    return fallback;
}
