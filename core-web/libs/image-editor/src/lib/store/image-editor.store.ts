import { tapResponse } from '@ngrx/operators';
import { signalStore, withComputed, withState } from '@ngrx/signals';
import { Dispatcher, Events, on, withEventHandlers, withReducer } from '@ngrx/signals/events';
import { EMPTY } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { computed, inject } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';

import {
    catchError,
    debounceTime,
    distinctUntilChanged,
    exhaustMap,
    switchMap,
    tap
} from 'rxjs/operators';

import { DotHttpErrorManagerService } from '@dotcms/data-access';
import { DotCMSTempFile } from '@dotcms/dotcms-models';

import {
    imageEditorAdjustEvents,
    imageEditorFileInfoEvents,
    imageEditorHistoryEvents,
    imageEditorLifecycleEvents,
    imageEditorToolEvents,
    imageEditorTransformEvents
} from './image-editor.events';
import {
    ImageEditorState,
    initialAdjustState,
    initialCropState,
    initialFileInfoState,
    initialFocalPointState,
    initialImageEditorState,
    initialTransformState,
    RANGES
} from './image-editor.state';

import {
    AdjustState,
    CompressionMode,
    CropState,
    FileInfoState,
    FilterCategory,
    FocalPointState,
    ImageEditorAssetContext,
    ImageEditorHistoryEntry,
    ImageEditorOpenParams,
    TransformState
} from '../models/image-editor.models';
import { DotImageEditorService } from '../services/dot-image-editor.service';
import { clamp, computeOutputDimensions } from '../utils/dimensions.util';
import { buildFilterChain, buildPreviewUrl } from '../utils/image-filter-url.builder';

/** The editable slices captured in a history snapshot. */
type EditableSlices = ImageEditorHistoryEntry['snapshot'];

/** The pristine values of the editable slices, used to seed and reset history. */
const initialEditableSlices: EditableSlices = {
    adjust: initialAdjustState,
    transform: initialTransformState,
    crop: initialCropState,
    focalPoint: initialFocalPointState,
    fileInfo: initialFileInfoState
};

/** Extracts the editable slices from the full state for snapshotting. */
function editableSlicesOf(state: ImageEditorState): EditableSlices {
    return {
        adjust: state.adjust,
        transform: state.transform,
        crop: state.crop,
        focalPoint: state.focalPoint,
        fileInfo: state.fileInfo
    };
}

/**
 * Coalesces a new edit into the undo/redo history. When the current head entry
 * shares the edit's category the entry is updated in place (so dragging a slider
 * produces a single history step); otherwise any redo tail is discarded and a new
 * entry is appended.
 * @param state - The current editor state
 * @param category - The category the edit belongs to
 * @param label - The human-readable label for the entry
 * @param snapshot - The editable slices to capture
 * @returns The new `history` array and `historyIndex`
 */
function coalesceHistory(
    state: ImageEditorState,
    category: FilterCategory,
    label: string,
    snapshot: EditableSlices
): Pick<ImageEditorState, 'history' | 'historyIndex'> {
    const head = state.history[state.historyIndex];

    if (head && head.category === category) {
        const history = state.history.map((entry, index) =>
            index === state.historyIndex ? { ...entry, label, snapshot } : entry
        );

        return { history, historyIndex: state.historyIndex };
    }

    const entry: ImageEditorHistoryEntry = {
        id: `${category}-${Date.now()}-${state.cacheBust}`,
        category,
        label,
        snapshot
    };
    const history = [...state.history.slice(0, state.historyIndex + 1), entry];

    return { history, historyIndex: history.length - 1 };
}

/** Builds a partial state that restores the editable slices from a snapshot. */
function restoreSlices(snapshot: EditableSlices): EditableSlices {
    return {
        adjust: snapshot.adjust,
        transform: snapshot.transform,
        crop: snapshot.crop,
        focalPoint: snapshot.focalPoint,
        fileInfo: snapshot.fileInfo
    };
}

/** Resolves the editable slices for a given history index, or initial when empty. */
function slicesAtIndex(history: ImageEditorHistoryEntry[], index: number): EditableSlices {
    return index < 0
        ? restoreSlices(initialEditableSlices)
        : restoreSlices(history[index].snapshot);
}

/** The editable slices, in snapshot order. */
const SLICE_KEYS = ['adjust', 'transform', 'crop', 'focalPoint', 'fileInfo'] as const;

/** A field-level patch over the editable slices (only the fields that changed). */
type SlicePatch = {
    adjust?: Partial<AdjustState>;
    transform?: Partial<TransformState>;
    crop?: Partial<CropState>;
    focalPoint?: Partial<FocalPointState>;
    fileInfo?: Partial<FileInfoState>;
};

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
        focalPoint: { ...base.focalPoint, ...patch.focalPoint },
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
function rebuildHistory(
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

/**
 * The largest crop of the given aspect ratio that fits the natural image,
 * centered on the active focal point (or the image center when none is set) and
 * clamped to the image bounds. This is what makes the focal point visible: the
 * kept region follows the focal point instead of the center.
 */
function focalCenteredCrop(aspect: number, state: ImageEditorState): CropState {
    const { naturalWidth, naturalHeight } = state.assetContext;
    const naturalAspect = naturalWidth / naturalHeight;

    let w: number;
    let h: number;
    if (aspect > naturalAspect) {
        w = naturalWidth;
        h = Math.round(naturalWidth / aspect);
    } else {
        h = naturalHeight;
        w = Math.round(naturalHeight * aspect);
    }

    const fx = state.focalPoint.active ? state.focalPoint.x : 0.5;
    const fy = state.focalPoint.active ? state.focalPoint.y : 0.5;
    const x = clamp(Math.round(fx * naturalWidth - w / 2), 0, naturalWidth - w);
    const y = clamp(Math.round(fy * naturalHeight - h / 2), 0, naturalHeight - h);

    return { x, y, w, h, active: true, aspect };
}

/** Produces the asset context for a freshly requested asset. */
function contextFromParams(params: ImageEditorOpenParams): ImageEditorAssetContext {
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

const COMPRESSION_LABELS: Record<CompressionMode, string> = {
    none: 'None',
    auto: 'Auto',
    jpeg: 'JPEG',
    webp: 'WebP'
};

/**
 * Times a failed preview is silently retried before the error UI is shown. The
 * server renders filter chains on the fly, so the first request for a fresh URL
 * can race that generation and return an incomplete response; a couple of silent
 * re-attempts (mirroring a manual refresh) almost always lands the finished image.
 */
const AUTO_PREVIEW_RETRY_LIMIT = 3;

/**
 * NgRx SignalStore for the image editor. Built entirely on the events API:
 * synchronous state transitions are folded by `withReducer` — one block per event
 * group (adjust, transform, file info, tools, history, lifecycle) — derived state
 * is exposed through `withComputed`, and asynchronous side effects (asset loading,
 * debounced size resolution, save and download) are declared as `withEventHandlers`
 * that react to the dispatched events stream. The store is NOT provided in root —
 * the editor dialog component supplies it so each editor instance is isolated.
 */
export const ImageEditorStore = signalStore(
    withState(initialImageEditorState),
    // Adjust panel: color & light.
    withReducer(
        on(imageEditorAdjustEvents.brightnessChanged, ({ payload }, state) => {
            const value = clamp(payload, RANGES.brightness.min, RANGES.brightness.max);
            const adjust: AdjustState = { ...state.adjust, brightness: value };

            return adjustPatch(state, adjust, 'adjust', `Brightness ${value}`);
        }),
        on(imageEditorAdjustEvents.hueChanged, ({ payload }, state) => {
            const value = clamp(payload, RANGES.hue.min, RANGES.hue.max);
            const adjust: AdjustState = { ...state.adjust, hue: value };

            return adjustPatch(state, adjust, 'adjust', `Hue ${value}`);
        }),
        on(imageEditorAdjustEvents.saturationChanged, ({ payload }, state) => {
            const value = clamp(payload, RANGES.saturation.min, RANGES.saturation.max);
            const adjust: AdjustState = { ...state.adjust, saturation: value };

            return adjustPatch(state, adjust, 'adjust', `Saturation ${value}`);
        }),
        on(imageEditorAdjustEvents.grayscaleToggled, ({ payload }, state) => {
            const adjust: AdjustState = { ...state.adjust, grayscale: payload };

            return adjustPatch(state, adjust, 'grayscale', `Grayscale ${payload ? 'on' : 'off'}`);
        })
    ),
    // Transform panel: scale, rotate, flip, output size.
    withReducer(
        on(imageEditorTransformEvents.scaleChanged, ({ payload }, state) => {
            const value = clamp(payload, RANGES.scale.min, RANGES.scale.max);
            // Resizing supersedes crop, mirroring the filter-chain rule.
            const transform: TransformState = { ...state.transform, scale: value };
            const crop = value !== 100 ? initialCropState : state.crop;

            return transformPatch(state, transform, crop, 'adjust', `Scale ${value}%`);
        }),
        on(imageEditorTransformEvents.rotateChanged, ({ payload }, state) => {
            const value = clamp(payload, RANGES.rotate.min, RANGES.rotate.max);
            const transform: TransformState = { ...state.transform, rotateDeg: value };

            return transformPatch(state, transform, state.crop, 'rotate', `Rotate ${value}°`);
        }),
        on(imageEditorTransformEvents.flipHToggled, (_event, state) => {
            const transform: TransformState = { ...state.transform, flipH: !state.transform.flipH };

            return transformPatch(state, transform, state.crop, 'flip', 'Flip horizontal');
        }),
        on(imageEditorTransformEvents.flipVToggled, (_event, state) => {
            const transform: TransformState = { ...state.transform, flipV: !state.transform.flipV };

            return transformPatch(state, transform, state.crop, 'flip', 'Flip vertical');
        }),
        on(imageEditorTransformEvents.outputDimsChanged, ({ payload }, state) => {
            const transform: TransformState = {
                ...state.transform,
                outputWidth: payload.width,
                outputHeight: payload.height
            };
            const isResizing = payload.width != null || payload.height != null;
            const crop = isResizing ? initialCropState : state.crop;

            return transformPatch(state, transform, crop, 'adjust', 'Resize');
        })
    ),
    // File info panel: compression & quality.
    withReducer(
        on(imageEditorFileInfoEvents.compressionChanged, ({ payload }, state) => {
            const fileInfo: FileInfoState = { ...state.fileInfo, compression: payload };

            return fileInfoPatch(
                state,
                fileInfo,
                'compression',
                `Compression ${COMPRESSION_LABELS[payload]}`
            );
        }),
        on(imageEditorFileInfoEvents.qualityChanged, ({ payload }, state) => {
            const value = clamp(payload, RANGES.quality.min, RANGES.quality.max);
            const fileInfo: FileInfoState = { ...state.fileInfo, quality: value };

            return fileInfoPatch(state, fileInfo, 'compression', `Quality ${value}`);
        })
    ),
    // Canvas tools: move / crop / focal.
    withReducer(
        on(imageEditorToolEvents.toolSelected, ({ payload }, state) => ({
            ...state,
            activeTool: payload
        })),
        on(imageEditorToolEvents.cropApplied, ({ payload }, state) => {
            const crop: CropState = { ...payload, active: true };
            // Crop and resize are mutually exclusive: applying a crop clears resize.
            const transform: TransformState = {
                ...state.transform,
                scale: 100,
                outputWidth: null,
                outputHeight: null
            };
            const next: ImageEditorState = {
                ...state,
                crop,
                transform,
                activeTool: 'move',
                previewStatus: 'loading',
                cacheBust: state.cacheBust + 1
            };

            return {
                ...next,
                ...coalesceHistory(next, 'crop', 'Crop', editableSlicesOf(next))
            };
        }),
        on(imageEditorToolEvents.cropCancelled, (_event, state) => ({
            ...state,
            crop: initialCropState,
            activeTool: 'move' as const
        })),
        on(imageEditorToolEvents.focalPointSet, ({ payload }, state) => {
            // No preview reload: the focal point doesn't change the rendered image
            // on its own — it's a saved anchor consumed by the aspect crop and
            // persisted on save. Just record it (and a coalesced history entry).
            const focalPoint: FocalPointState = { x: payload.x, y: payload.y, active: true };
            const next: ImageEditorState = { ...state, focalPoint };

            return {
                ...next,
                ...coalesceHistory(
                    next,
                    'focal',
                    `Focal point ${payload.x.toFixed(2)}, ${payload.y.toFixed(2)}`,
                    editableSlicesOf(next)
                )
            };
        }),
        on(imageEditorToolEvents.focalPointCleared, (_event, state) => ({
            ...state,
            focalPoint: initialFocalPointState
        })),
        on(imageEditorToolEvents.aspectCropApplied, ({ payload }, state) => {
            if (!state.assetContext.naturalWidth || !state.assetContext.naturalHeight) {
                return state;
            }

            const crop = focalCenteredCrop(payload.aspect, state);
            // Cropping is mutually exclusive with resize, and returns to the move tool.
            const transform: TransformState = {
                ...state.transform,
                scale: 100,
                outputWidth: null,
                outputHeight: null
            };
            const next: ImageEditorState = {
                ...state,
                crop,
                transform,
                activeTool: 'move',
                previewStatus: 'loading',
                cacheBust: state.cacheBust + 1
            };

            return {
                ...next,
                ...coalesceHistory(next, 'crop', `Crop ${payload.label}`, editableSlicesOf(next))
            };
        })
    ),
    // Applied-edits history: remove / undo / redo / reset.
    withReducer(
        on(imageEditorHistoryEvents.editRemoved, ({ payload }, state) => {
            const removedIdx = state.history.findIndex((entry) => entry.id === payload.id);

            if (removedIdx === -1) {
                return state;
            }

            // Replay the survivors so the removed edit's effect is folded out (a
            // plain filter left it baked into later snapshots).
            const history = rebuildHistory(state.history, removedIdx);
            // Preserve the logical head: shift it back only when an entry at or
            // before the head was removed, never jumping it forward to the tail.
            const historyIndex =
                removedIdx <= state.historyIndex
                    ? Math.max(-1, state.historyIndex - 1)
                    : Math.min(state.historyIndex, history.length - 1);

            return {
                ...state,
                ...slicesAtIndex(history, historyIndex),
                history,
                historyIndex,
                previewStatus: 'loading' as const,
                cacheBust: state.cacheBust + 1
            };
        }),
        on(imageEditorHistoryEvents.undoRequested, (_event, state) => {
            const historyIndex = Math.max(-1, state.historyIndex - 1);

            return {
                ...state,
                ...slicesAtIndex(state.history, historyIndex),
                historyIndex,
                previewStatus: 'loading' as const,
                cacheBust: state.cacheBust + 1
            };
        }),
        on(imageEditorHistoryEvents.redoRequested, (_event, state) => {
            const historyIndex = Math.min(state.history.length - 1, state.historyIndex + 1);

            return {
                ...state,
                ...slicesAtIndex(state.history, historyIndex),
                historyIndex,
                previewStatus: 'loading' as const,
                cacheBust: state.cacheBust + 1
            };
        }),
        on(imageEditorHistoryEvents.resetRequested, (_event, state) => ({
            ...state,
            ...restoreSlices(initialEditableSlices),
            history: [],
            historyIndex: -1,
            previewStatus: 'loading' as const,
            cacheBust: state.cacheBust + 1
        }))
    ),
    // Editor lifecycle: load, preview, size, download, save.
    withReducer(
        on(imageEditorLifecycleEvents.assetRequested, ({ payload }, _state) => ({
            ...initialImageEditorState,
            assetContext: contextFromParams(payload),
            previewStatus: 'loading' as const
        })),
        on(imageEditorLifecycleEvents.assetLoaded, ({ payload }, state) => ({
            ...state,
            assetContext: {
                ...state.assetContext,
                naturalWidth: payload.naturalWidth,
                naturalHeight: payload.naturalHeight
            },
            fileInfo: {
                ...state.fileInfo,
                originalBytes: payload.originalBytes,
                currentBytes: payload.originalBytes
            },
            focalPoint: payload.focalPoint ?? state.focalPoint
        })),
        on(imageEditorLifecycleEvents.assetLoadFailed, ({ payload }, state) => ({
            ...state,
            previewStatus: 'error' as const,
            error: errorMessage(payload, 'Failed to load image')
        })),
        on(imageEditorLifecycleEvents.previewLoaded, (_event, state) => ({
            ...state,
            previewStatus: 'loaded' as const,
            previewRetries: 0,
            error: null
        })),
        // Image GETs for heavy filter chains can fail transiently even when the URL
        // is valid (a later HEAD/GET returns 200). Retry silently with a fresh
        // cache-bust up to AUTO_PREVIEW_RETRY_LIMIT before surfacing the error UI.
        on(imageEditorLifecycleEvents.previewErrored, (_event, state) => {
            if (state.previewRetries < AUTO_PREVIEW_RETRY_LIMIT) {
                return {
                    ...state,
                    previewRetries: state.previewRetries + 1,
                    previewStatus: 'loading' as const,
                    cacheBust: state.cacheBust + 1
                };
            }

            return {
                ...state,
                previewStatus: 'error' as const,
                previewRetries: 0,
                error: 'Failed to render preview'
            };
        }),
        on(imageEditorLifecycleEvents.retryRequested, (_event, state) => ({
            ...state,
            // A manual retry restores the silent-retry budget.
            previewRetries: 0,
            previewStatus: 'loading' as const,
            cacheBust: state.cacheBust + 1
        })),
        on(imageEditorLifecycleEvents.previewSizeResolved, ({ payload }, state) => ({
            ...state,
            fileInfo: { ...state.fileInfo, currentBytes: payload }
        })),
        on(imageEditorLifecycleEvents.saveRequested, (_event, state) => ({
            ...state,
            saveStatus: 'saving' as const
        })),
        on(imageEditorLifecycleEvents.saveAsRequested, (_event, state) => ({
            ...state,
            saveStatus: 'saving' as const
        })),
        on(imageEditorLifecycleEvents.saveSucceeded, ({ payload }, state) => ({
            ...state,
            savedTempFile: payload,
            saveStatus: 'saved' as const
        })),
        on(imageEditorLifecycleEvents.saveFailed, ({ payload }, state) => ({
            ...state,
            saveStatus: 'error' as const,
            error: errorMessage(payload, 'Failed to save image')
        }))
    ),
    withComputed((store) => {
        const appliedFilters = computed(() =>
            buildFilterChain({
                adjust: store.adjust(),
                transform: store.transform(),
                crop: store.crop(),
                fileInfo: store.fileInfo(),
                naturalWidth: store.assetContext().naturalWidth,
                naturalHeight: store.assetContext().naturalHeight
            })
        );

        return {
            /** The ordered server filter chain derived from the current edits. */
            appliedFilters,
            /** The fully-qualified, cache-busted preview URL for the current edits. */
            previewUrl: computed(() =>
                buildPreviewUrl(store.assetContext(), appliedFilters(), store.cacheBust())
            ),
            /** The applied edits up to the current history head, for the edits list. */
            appliedEdits: computed(() =>
                store
                    .history()
                    .slice(0, store.historyIndex() + 1)
                    .map(({ id, category, label }) => ({ id, category, label }))
            ),
            /** The effective output dimensions of the edited image. */
            outputDimensions: computed(() =>
                computeOutputDimensions(store.assetContext(), store.transform(), store.crop())
            ),
            /** Whether there is a previous history step to undo to. */
            canUndo: computed(() => store.historyIndex() > -1),
            /** Whether there is a forward history step to redo to. */
            canRedo: computed(() => store.historyIndex() < store.history().length - 1),
            /** Whether any edit produces a non-empty filter chain. */
            isDirty: computed(() => appliedFilters().length > 0),
            /** Whether the editor is mid-flight loading a preview or saving. */
            isBusy: computed(
                () => store.previewStatus() === 'loading' || store.saveStatus() === 'saving'
            ),
            /** Whether a save can be initiated right now. */
            canSave: computed(
                () =>
                    store.previewStatus() === 'loaded' &&
                    store.saveStatus() !== 'saving' &&
                    appliedFilters().length > 0
            )
        };
    }),
    // Asynchronous side effects, declared as event handlers: each returned
    // observable reacts to the dispatched event stream (or a derived signal) and
    // dispatches result events. The store subscribes to them for its lifetime.
    withEventHandlers((store) => {
        const events = inject(Events);
        const dispatcher = inject(Dispatcher);
        const service = inject(DotImageEditorService);
        const httpErrorManager = inject(DotHttpErrorManagerService);

        // Save the edited image and dispatch the outcome.
        const saveEditedImage$ = () =>
            service.saveEditedImage(store.previewUrl(), store.assetContext().variable).pipe(
                tapResponse({
                    next: (tempFile: DotCMSTempFile) =>
                        dispatcher.dispatch(imageEditorLifecycleEvents.saveSucceeded(tempFile)),
                    error: (error: HttpErrorResponse) => {
                        // Surface the error but keep the editor open for retry.
                        httpErrorManager.handle(error);
                        dispatcher.dispatch(imageEditorLifecycleEvents.saveFailed(error));
                    }
                }),
                // Swallow the rethrown error so the effect stream stays alive.
                catchError(() => EMPTY)
            );

        return {
            // Load asset metadata whenever a new asset is requested.
            loadAsset$: events.on(imageEditorLifecycleEvents.assetRequested).pipe(
                switchMap(() =>
                    service.loadAssetMeta(store.assetContext()).pipe(
                        tapResponse({
                            next: (meta) =>
                                dispatcher.dispatch(imageEditorLifecycleEvents.assetLoaded(meta)),
                            error: (error) =>
                                dispatcher.dispatch(
                                    imageEditorLifecycleEvents.assetLoadFailed(error)
                                )
                        })
                    )
                )
            ),

            // Resolve the edited preview size, debounced against rapid edits.
            resolveSize$: toObservable(store.previewUrl).pipe(
                debounceTime(250),
                distinctUntilChanged(),
                switchMap((url) =>
                    service
                        .getFileSize(url)
                        .pipe(
                            tap((bytes) =>
                                dispatcher.dispatch(
                                    imageEditorLifecycleEvents.previewSizeResolved(bytes)
                                )
                            )
                        )
                )
            ),

            // Persist the focal point first (when active), then save the image.
            // `exhaustMap` ignores new save triggers while one is in flight: a
            // destructive write must not be cancelled mid-flight, which would
            // strand `saveStatus: 'saving'` with no terminal event.
            save$: events
                .on(
                    imageEditorLifecycleEvents.saveRequested,
                    imageEditorLifecycleEvents.saveAsRequested
                )
                .pipe(
                    exhaustMap(() => {
                        const focalPoint = store.focalPoint();

                        if (!focalPoint.active) {
                            return saveEditedImage$();
                        }

                        return service
                            .persistFocalPoint(store.assetContext().originalUrl, {
                                x: focalPoint.x,
                                y: focalPoint.y
                            })
                            .pipe(switchMap(() => saveEditedImage$()));
                    })
                ),

            // Trigger a client-side download of the current preview.
            download$: events
                .on(imageEditorLifecycleEvents.downloadRequested)
                .pipe(
                    tap(() =>
                        service.triggerDownload(store.previewUrl(), store.assetContext().fileName)
                    )
                )
        };
    })
);

/** Applies an adjust-slice edit, bumps the cache and coalesces history. */
function adjustPatch(
    state: ImageEditorState,
    adjust: AdjustState,
    category: FilterCategory,
    label: string
): ImageEditorState {
    const next: ImageEditorState = {
        ...state,
        adjust,
        previewStatus: 'loading',
        cacheBust: state.cacheBust + 1
    };

    return { ...next, ...coalesceHistory(next, category, label, editableSlicesOf(next)) };
}

/** Applies a transform-slice edit, bumps the cache and coalesces history. */
function transformPatch(
    state: ImageEditorState,
    transform: TransformState,
    crop: CropState,
    category: FilterCategory,
    label: string
): ImageEditorState {
    const next: ImageEditorState = {
        ...state,
        transform,
        crop,
        previewStatus: 'loading',
        cacheBust: state.cacheBust + 1
    };

    return { ...next, ...coalesceHistory(next, category, label, editableSlicesOf(next)) };
}

/** Applies a fileInfo-slice edit, bumps the cache and coalesces history. */
function fileInfoPatch(
    state: ImageEditorState,
    fileInfo: FileInfoState,
    category: FilterCategory,
    label: string
): ImageEditorState {
    const next: ImageEditorState = {
        ...state,
        fileInfo,
        previewStatus: 'loading',
        cacheBust: state.cacheBust + 1
    };

    return { ...next, ...coalesceHistory(next, category, label, editableSlicesOf(next)) };
}

/** Extracts a readable message from an unknown error payload. */
function errorMessage(payload: unknown, fallback: string): string {
    if (payload instanceof Error) {
        return payload.message;
    }

    if (typeof payload === 'string') {
        return payload;
    }

    return fallback;
}
