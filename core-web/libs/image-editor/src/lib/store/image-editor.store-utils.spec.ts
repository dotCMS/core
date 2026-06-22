import { initialImageEditorState } from './image-editor.state';
import {
    adjustPatch,
    coalesceHistory,
    contextFromParams,
    editableSlicesOf,
    errorMessage,
    fileInfoPatch,
    focalCenteredCrop,
    initialEditableSlices,
    rebuildHistory,
    restoreSlices,
    slicesAtIndex,
    transformPatch
} from './image-editor.store-utils';

import {
    EditableSlices,
    ImageEditorHistoryEntry,
    ImageEditorState
} from '../models/image-editor.models';

/** A history entry whose snapshot patches the given editable slices. */
function entry(
    id: string,
    category: ImageEditorHistoryEntry['category'],
    patch: Partial<EditableSlices>
): ImageEditorHistoryEntry {
    return {
        id,
        category,
        label: id,
        snapshot: { ...initialEditableSlices, ...patch }
    };
}

/** A full state seeded with a history and head index. */
function stateWith(
    history: ImageEditorHistoryEntry[],
    historyIndex = history.length - 1,
    overrides: Partial<ImageEditorState> = {}
): ImageEditorState {
    return { ...initialImageEditorState, history, historyIndex, ...overrides };
}

describe('image-editor.store-utils', () => {
    describe('editableSlicesOf', () => {
        it('extracts exactly the five editable slices', () => {
            const slices = editableSlicesOf(initialImageEditorState);

            expect(Object.keys(slices).sort()).toEqual([
                'adjust',
                'crop',
                'fileInfo',
                'focalPoint',
                'transform'
            ]);
            expect(slices.adjust).toBe(initialImageEditorState.adjust);
        });
    });

    describe('restoreSlices', () => {
        it('returns a shallow copy carrying every slice', () => {
            const restored = restoreSlices(initialEditableSlices);

            expect(restored).toEqual(initialEditableSlices);
            expect(restored).not.toBe(initialEditableSlices);
        });
    });

    describe('slicesAtIndex', () => {
        it('returns the initial slices for index < 0', () => {
            const history = [
                entry('a', 'adjust', {
                    adjust: { ...initialEditableSlices.adjust, brightness: 50 }
                })
            ];

            expect(slicesAtIndex(history, -1)).toEqual(initialEditableSlices);
        });

        it('returns the snapshot at a valid index', () => {
            const snapshot = { ...initialEditableSlices.adjust, brightness: 50 };
            const history = [entry('a', 'adjust', { adjust: snapshot })];

            expect(slicesAtIndex(history, 0).adjust.brightness).toBe(50);
        });
    });

    describe('coalesceHistory', () => {
        it('appends a new entry when the head category differs', () => {
            const state = stateWith([entry('a', 'adjust', {})], 0);

            const result = coalesceHistory(state, 'rotate', 'Rotate 90°', editableSlicesOf(state));

            expect(result.history).toHaveLength(2);
            expect(result.historyIndex).toBe(1);
            expect(result.history[1].category).toBe('rotate');
        });

        it('appends a new entry when history is empty', () => {
            const result = coalesceHistory(
                initialImageEditorState,
                'adjust',
                'Brightness 10',
                editableSlicesOf(initialImageEditorState)
            );

            expect(result.history).toHaveLength(1);
            expect(result.historyIndex).toBe(0);
        });

        it('updates in place when the head shares the category', () => {
            const state = stateWith([entry('a', 'adjust', {})], 0);

            const result = coalesceHistory(
                state,
                'adjust',
                'Brightness 20',
                editableSlicesOf(state)
            );

            expect(result.history).toHaveLength(1);
            expect(result.history[0].label).toBe('Brightness 20');
            expect(result.historyIndex).toBe(0);
        });

        it('drops the redo tail on a same-category in-place update', () => {
            const state = stateWith(
                [entry('a', 'adjust', {}), entry('b', 'rotate', {})],
                0 // head on the adjust entry; rotate is a redo step
            );

            const result = coalesceHistory(
                state,
                'adjust',
                'Brightness 30',
                editableSlicesOf(state)
            );

            expect(result.history).toHaveLength(1);
            expect(result.history[0].label).toBe('Brightness 30');
            expect(result.historyIndex).toBe(0);
        });

        it('truncates the redo tail when appending a different category', () => {
            const state = stateWith(
                [entry('a', 'adjust', {}), entry('b', 'rotate', {})],
                0 // head before the rotate redo step
            );

            const result = coalesceHistory(
                state,
                'flip',
                'Flip horizontal',
                editableSlicesOf(state)
            );

            expect(result.history.map((e) => e.category)).toEqual(['adjust', 'flip']);
            expect(result.historyIndex).toBe(1);
        });
    });

    describe('rebuildHistory', () => {
        const wide = { ...initialEditableSlices.adjust, brightness: 10 };
        const tall = { ...initialEditableSlices.adjust, brightness: 10, hue: 20 };

        it('removes the first entry and replays survivors from initial', () => {
            const history = [
                entry('a', 'adjust', { adjust: wide }),
                entry('b', 'rotate', {
                    adjust: wide,
                    transform: { ...initialEditableSlices.transform, rotateDeg: 90 }
                })
            ];

            const rebuilt = rebuildHistory(history, 0);

            expect(rebuilt).toHaveLength(1);
            // The removed brightness delta is folded out; rotation survives.
            expect(rebuilt[0].snapshot.adjust.brightness).toBe(0);
            expect(rebuilt[0].snapshot.transform.rotateDeg).toBe(90);
        });

        it('removes a middle entry and rebuilds later snapshots without its delta', () => {
            const history = [
                entry('a', 'adjust', { adjust: wide }),
                entry('b', 'hue', { adjust: tall }),
                entry('c', 'rotate', {
                    adjust: tall,
                    transform: { ...initialEditableSlices.transform, rotateDeg: 45 }
                })
            ];

            const rebuilt = rebuildHistory(history, 1);

            expect(rebuilt.map((e) => e.id)).toEqual(['a', 'c']);
            // The hue delta (from entry b) is folded out of the surviving rotate snapshot.
            expect(rebuilt[1].snapshot.adjust.hue).toBe(0);
            expect(rebuilt[1].snapshot.adjust.brightness).toBe(10);
            expect(rebuilt[1].snapshot.transform.rotateDeg).toBe(45);
        });

        it('removes the last entry', () => {
            const history = [
                entry('a', 'adjust', { adjust: wide }),
                entry('b', 'rotate', { adjust: wide })
            ];

            expect(rebuildHistory(history, 1).map((e) => e.id)).toEqual(['a']);
        });
    });

    describe('focalCenteredCrop', () => {
        it('uses the full width when the target aspect is wider than the image', () => {
            const state = stateWith([], -1, {
                assetContext: {
                    ...initialImageEditorState.assetContext,
                    naturalWidth: 1000,
                    naturalHeight: 800
                },
                focalPoint: { x: 0.5, y: 0.5, active: true }
            });

            // 16:9 (1.78) > 1000/800 (1.25): keep full width, derive a shorter height.
            const crop = focalCenteredCrop(16 / 9, state);

            expect(crop.w).toBe(1000);
            expect(crop.h).toBe(Math.round(1000 / (16 / 9)));
            expect(crop.active).toBe(true);
        });

        it('uses the full height when the target aspect is taller than the image', () => {
            const state = stateWith([], -1, {
                assetContext: {
                    ...initialImageEditorState.assetContext,
                    naturalWidth: 1000,
                    naturalHeight: 800
                },
                focalPoint: { x: 0.5, y: 0.5, active: true }
            });

            // 1:1 (1.0) <= 1.25: keep full height, derive a narrower width.
            const crop = focalCenteredCrop(1, state);

            expect(crop.h).toBe(800);
            expect(crop.w).toBe(800);
        });

        it('centers on the image middle when no focal point is active', () => {
            const state = stateWith([], -1, {
                assetContext: {
                    ...initialImageEditorState.assetContext,
                    naturalWidth: 1000,
                    naturalHeight: 800
                },
                focalPoint: { x: 0.8, y: 0.8, active: false }
            });

            // Inactive focal → fx/fy default to 0.5: an 800×800 region centered → x=100, y=0.
            const crop = focalCenteredCrop(1, state);

            expect(crop.x).toBe(100);
            expect(crop.y).toBe(0);
        });

        it('clamps the crop origin to the image bounds for an off-center focal point', () => {
            const state = stateWith([], -1, {
                assetContext: {
                    ...initialImageEditorState.assetContext,
                    naturalWidth: 1000,
                    naturalHeight: 800
                },
                focalPoint: { x: 0.8, y: 0.5, active: true }
            });

            // 800 wide region centered on x=800 wants x=400 but clamps to 1000−800=200.
            const crop = focalCenteredCrop(1, state);

            expect(crop.x).toBe(200);
            expect(crop.y).toBe(0);
        });
    });

    describe('contextFromParams', () => {
        it('prefers the temp file id when present and marks it a temp file', () => {
            const ctx = contextFromParams({
                tempId: 'temp-1',
                inode: 'inode-1',
                variable: 'fileAsset',
                fieldName: 'fileAsset'
            });

            expect(ctx.idOrTempId).toBe('temp-1');
            expect(ctx.isTempFile).toBe(true);
            expect(ctx.originalUrl).toBe('/contentAsset/image/temp-1/fileAsset');
        });

        it('falls through an empty temp id to the inode', () => {
            const ctx = contextFromParams({
                tempId: '',
                inode: 'inode-1',
                variable: 'fileAsset',
                fieldName: 'fileAsset',
                byInode: true
            });

            expect(ctx.idOrTempId).toBe('inode-1');
            expect(ctx.isTempFile).toBe(false);
            expect(ctx.byInode).toBe(true);
        });

        it('defaults byInode to false and optional strings to empty', () => {
            const ctx = contextFromParams({ inode: 'inode-1', variable: 'v', fieldName: 'f' });

            expect(ctx.byInode).toBe(false);
            expect(ctx.fileName).toBe('');
            expect(ctx.mimeType).toBe('');
        });
    });

    describe('slice patch helpers', () => {
        it('adjustPatch sets loading, bumps the cache-bust and coalesces history', () => {
            const next = adjustPatch(
                initialImageEditorState,
                { ...initialImageEditorState.adjust, brightness: 10 },
                'adjust',
                'Brightness 10'
            );

            expect(next.adjust.brightness).toBe(10);
            expect(next.previewStatus).toBe('loading');
            expect(next.cacheBust).toBe(1);
            expect(next.history).toHaveLength(1);
        });

        it('transformPatch applies the transform and crop together', () => {
            const next = transformPatch(
                initialImageEditorState,
                { ...initialImageEditorState.transform, scale: 50 },
                initialImageEditorState.crop,
                'adjust',
                'Scale 50%'
            );

            expect(next.transform.scale).toBe(50);
            expect(next.previewStatus).toBe('loading');
            expect(next.cacheBust).toBe(1);
        });

        it('fileInfoPatch applies the fileInfo slice', () => {
            const next = fileInfoPatch(
                initialImageEditorState,
                { ...initialImageEditorState.fileInfo, quality: 50 },
                'compression',
                'Quality 50'
            );

            expect(next.fileInfo.quality).toBe(50);
            expect(next.history[0].category).toBe('compression');
        });
    });

    describe('errorMessage', () => {
        it('returns the message of an Error instance', () => {
            expect(errorMessage(new Error('boom'), 'fallback')).toBe('boom');
        });

        it('returns a string payload as-is', () => {
            expect(errorMessage('plain error', 'fallback')).toBe('plain error');
        });

        it('returns the fallback for any other payload', () => {
            expect(errorMessage({ unexpected: true }, 'fallback')).toBe('fallback');
            expect(errorMessage(undefined, 'fallback')).toBe('fallback');
        });
    });
});
