import { mockProvider, SpyObject } from '@ngneat/spectator/jest';
import { Dispatcher, injectDispatch } from '@ngrx/signals/events';
import { of, throwError } from 'rxjs';

import { Injector, runInInjectionContext } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { DotMessageService } from '@dotcms/data-access';

import {
    imageEditorAdjustEvents,
    imageEditorFileInfoEvents,
    imageEditorHistoryEvents,
    imageEditorLifecycleEvents,
    imageEditorToolEvents,
    imageEditorTransformEvents
} from './image-editor.events';
import { ImageEditorStore } from './image-editor.store';

import { ImageEditorOpenParams } from '../models/image-editor.models';
import { DotImageEditorService } from '../services/dot-image-editor.service';

const OPEN_PARAMS: ImageEditorOpenParams = {
    inode: 'inode-1',
    variable: 'fileAsset',
    fieldName: 'fileAsset',
    fileName: 'photo.png',
    mimeType: 'image/png'
};

describe('ImageEditorStore', () => {
    let store: InstanceType<typeof ImageEditorStore>;
    let service: SpyObject<DotImageEditorService>;
    let injector: Injector;

    // Self-dispatching event groups, resolved within the injection context.
    let adjust: ReturnType<typeof injectDispatch<typeof imageEditorAdjustEvents>>;
    let transform: ReturnType<typeof injectDispatch<typeof imageEditorTransformEvents>>;
    let fileInfo: ReturnType<typeof injectDispatch<typeof imageEditorFileInfoEvents>>;
    let tool: ReturnType<typeof injectDispatch<typeof imageEditorToolEvents>>;
    let history: ReturnType<typeof injectDispatch<typeof imageEditorHistoryEvents>>;
    let lifecycle: ReturnType<typeof injectDispatch<typeof imageEditorLifecycleEvents>>;

    beforeEach(() => {
        jest.useFakeTimers();

        TestBed.configureTestingModule({
            providers: [
                ImageEditorStore,
                Dispatcher,
                mockProvider(DotImageEditorService, {
                    getFileSize: jest.fn().mockReturnValue(of(1000)),
                    loadAssetMeta: jest
                        .fn()
                        .mockReturnValue(
                            of({ naturalWidth: 800, naturalHeight: 600, originalBytes: 5000 })
                        ),
                    triggerDownload: jest.fn()
                }),
                mockProvider(DotMessageService, { get: jest.fn((key: string) => key) })
            ]
        });

        injector = TestBed.inject(Injector);
        // Instantiating the store runs its `onInit` effects.
        store = TestBed.inject(ImageEditorStore);
        service = TestBed.inject(DotImageEditorService) as SpyObject<DotImageEditorService>;

        runInInjectionContext(injector, () => {
            adjust = injectDispatch(imageEditorAdjustEvents);
            transform = injectDispatch(imageEditorTransformEvents);
            fileInfo = injectDispatch(imageEditorFileInfoEvents);
            tool = injectDispatch(imageEditorToolEvents);
            history = injectDispatch(imageEditorHistoryEvents);
            lifecycle = injectDispatch(imageEditorLifecycleEvents);
        });
    });

    afterEach(() => {
        jest.useRealTimers();
    });

    it('should start from the initial state', () => {
        expect(store.adjust().brightness).toBe(0);
        expect(store.transform().scale).toBe(100);
        expect(store.fileInfo().quality).toBe(85);
        expect(store.focalPoint()).toEqual({ x: 0.5, y: 0.5, active: false });
        expect(store.activeTool()).toBe('move');
        expect(store.previewStatus()).toBe('idle');
        expect(store.history()).toEqual([]);
        expect(store.historyIndex()).toBe(-1);
        expect(store.cacheBust()).toBe(0);
    });

    describe('panel edits', () => {
        it('should clamp and patch brightness and append a history entry', () => {
            adjust.brightnessChanged(150);

            expect(store.adjust().brightness).toBe(100);
            expect(store.history()).toHaveLength(1);
            expect(store.history()[0].category).toBe('adjust');
            expect(store.history()[0].label).toBe('Brightness 100');
            expect(store.historyIndex()).toBe(0);
            expect(store.cacheBust()).toBe(1);
            expect(store.previewStatus()).toBe('loading');
        });

        it('should coalesce rapid same-category edits into a single entry', () => {
            adjust.brightnessChanged(10);
            adjust.brightnessChanged(20);
            adjust.saturationChanged(30);

            expect(store.adjust().brightness).toBe(20);
            expect(store.adjust().saturation).toBe(30);
            expect(store.history()).toHaveLength(1);
            expect(store.history()[0].label).toBe('Saturation 30');
        });

        it('should append a new entry when the category changes', () => {
            adjust.brightnessChanged(10);
            transform.rotateChanged(90);

            expect(store.history()).toHaveLength(2);
            expect(store.history()[0].category).toBe('adjust');
            expect(store.history()[1].category).toBe('rotate');
            expect(store.history()[1].label).toBe('Rotate 90°');
        });

        it('should clamp scale and reset crop (resize XOR crop)', () => {
            tool.cropApplied({ x: 0, y: 0, w: 100, h: 100, active: false, aspect: null });
            transform.scaleChanged(500);

            expect(store.transform().scale).toBe(400);
            expect(store.crop().active).toBe(false);
        });

        it('should toggle flip flags under the flip category', () => {
            transform.flipHToggled();
            transform.flipVToggled();

            expect(store.transform().flipH).toBe(true);
            expect(store.transform().flipV).toBe(true);
            expect(store.history().some((entry) => entry.category === 'flip')).toBe(true);
        });

        it('should set compression under the compression category', () => {
            fileInfo.compressionChanged('jpeg');

            expect(store.fileInfo().compression).toBe('jpeg');
            expect(store.history()[0].category).toBe('compression');
        });
    });

    describe('tools', () => {
        it('should select a tool without touching history', () => {
            tool.toolSelected('crop');

            expect(store.activeTool()).toBe('crop');
            expect(store.history()).toHaveLength(0);
        });

        it('should apply a crop, reset resize and add a crop entry', () => {
            transform.scaleChanged(50);
            tool.cropApplied({ x: 10, y: 10, w: 200, h: 150, active: false, aspect: null });

            expect(store.crop()).toEqual({
                x: 10,
                y: 10,
                w: 200,
                h: 150,
                active: true,
                aspect: null
            });
            expect(store.transform().scale).toBe(100);
            expect(store.activeTool()).toBe('move');
            expect(store.history().at(-1)?.category).toBe('crop');
        });

        it('should cancel a crop back to inactive', () => {
            tool.cropApplied({ x: 10, y: 10, w: 200, h: 150, active: false, aspect: null });
            tool.cropCancelled();

            expect(store.crop().active).toBe(false);
            expect(store.activeTool()).toBe('move');
        });

        it('should set and clear the focal point', () => {
            tool.focalPointSet({ x: 0.25, y: 0.75 });

            expect(store.focalPoint()).toEqual({ x: 0.25, y: 0.75, active: true });
            expect(store.history().at(-1)?.category).toBe('focal');

            tool.focalPointCleared();

            expect(store.focalPoint()).toEqual({ x: 0.5, y: 0.5, active: false });
        });

        it('should apply an aspect crop centered on the focal point', () => {
            lifecycle.assetRequested(OPEN_PARAMS);
            lifecycle.assetLoaded({ naturalWidth: 1000, naturalHeight: 800, originalBytes: 5000 });
            tool.focalPointSet({ x: 0.8, y: 0.5 });

            tool.aspectCropApplied({ aspect: 1, label: '1:1' });

            // 1:1 in a 1000×800 image → an 800×800 region; centered on x=0.8 (=800px)
            // it wants x=400 but clamps to the right edge (1000−800=200), y centers at 0.
            expect(store.crop()).toEqual({ x: 200, y: 0, w: 800, h: 800, active: true, aspect: 1 });
            expect(store.transform().scale).toBe(100);
            expect(store.activeTool()).toBe('move');
            expect(store.history().at(-1)?.category).toBe('crop');
        });
    });

    describe('history', () => {
        it('should remove a specific edit and recompute slices', () => {
            adjust.brightnessChanged(20);
            transform.rotateChanged(45);
            const rotateId = store.history()[1].id;

            history.editRemoved({ id: rotateId });

            expect(store.history()).toHaveLength(1);
            expect(store.transform().rotateDeg).toBe(0);
            expect(store.adjust().brightness).toBe(20);
        });

        it('should keep the head on the correct entry when a middle edit is removed', () => {
            adjust.brightnessChanged(20);
            transform.rotateChanged(45);
            adjust.saturationChanged(30);

            // Step the head back to the rotate entry, then remove the older
            // brightness entry so the removed index sits before the head.
            history.undoRequested();
            expect(store.historyIndex()).toBe(1);
            const brightnessId = store.history()[0].id;

            history.editRemoved({ id: brightnessId });

            // The head must not jump to the tail (saturation) — it stays on the
            // rotate entry, whose logical position shifted from index 1 to 0.
            expect(store.history()).toHaveLength(2);
            expect(store.historyIndex()).toBe(0);
            expect(store.history()[0].category).toBe('rotate');
            expect(store.history()[0].label).toBe('Rotate 45°');
            // The removed brightness edit is replayed out: the rebuilt snapshot at
            // the head keeps the rotation but no longer carries the brightness.
            expect(store.transform().rotateDeg).toBe(45);
            expect(store.adjust().brightness).toBe(0);
            expect(store.adjust().saturation).toBe(0);
        });

        it('should drop a removed edit effect while re-applying the survivors', () => {
            // The reported bug: remove compression but keep crop — crop must still
            // apply and compression must revert.
            fileInfo.compressionChanged('webp');
            tool.cropApplied({ x: 10, y: 10, w: 100, h: 100, active: true, aspect: null });
            const compressionId = store.history()[0].id;

            history.editRemoved({ id: compressionId });

            expect(store.history()).toHaveLength(1);
            expect(store.fileInfo().compression).toBe('none');
            expect(store.crop().active).toBe(true);
        });

        it('should undo and redo edits', () => {
            adjust.brightnessChanged(20);
            transform.rotateChanged(45);

            history.undoRequested();
            expect(store.historyIndex()).toBe(0);
            expect(store.transform().rotateDeg).toBe(0);
            expect(store.adjust().brightness).toBe(20);

            history.undoRequested();
            expect(store.historyIndex()).toBe(-1);
            expect(store.adjust().brightness).toBe(0);

            history.redoRequested();
            expect(store.historyIndex()).toBe(0);
            expect(store.adjust().brightness).toBe(20);
        });

        it('should not undo past the start nor redo past the end', () => {
            adjust.brightnessChanged(20);

            history.undoRequested();
            history.undoRequested();
            expect(store.historyIndex()).toBe(-1);

            history.redoRequested();
            history.redoRequested();
            expect(store.historyIndex()).toBe(0);
        });

        it('drops the redo tail when a same-category edit follows an undo', () => {
            adjust.brightnessChanged(20); // entry 0 (adjust)
            transform.rotateChanged(45); // entry 1 (rotate)
            expect(store.history()).toHaveLength(2);

            history.undoRequested(); // back to entry 0; entry 1 is now a redo step
            expect(store.canRedo()).toBe(true);

            // A new same-category edit coalesces in place AND invalidates the redo
            // tail (it was built against the pre-undo value).
            adjust.brightnessChanged(40);
            expect(store.history()).toHaveLength(1);
            expect(store.historyIndex()).toBe(0);
            expect(store.canRedo()).toBe(false);
            expect(store.adjust().brightness).toBe(40);
        });

        it('should reset everything', () => {
            adjust.brightnessChanged(20);
            transform.rotateChanged(45);

            history.resetRequested();

            expect(store.history()).toEqual([]);
            expect(store.historyIndex()).toBe(-1);
            expect(store.adjust().brightness).toBe(0);
            expect(store.transform().rotateDeg).toBe(0);
        });
    });

    describe('computed truth tables', () => {
        it('canUndo / canRedo reflect the history index', () => {
            expect(store.canUndo()).toBe(false);
            expect(store.canRedo()).toBe(false);

            adjust.brightnessChanged(20);
            expect(store.canUndo()).toBe(true);
            expect(store.canRedo()).toBe(false);

            history.undoRequested();
            expect(store.canUndo()).toBe(false);
            expect(store.canRedo()).toBe(true);
        });

        it('isDirty becomes true only with a non-empty filter chain', () => {
            expect(store.isDirty()).toBe(false);
            adjust.brightnessChanged(20);
            expect(store.isDirty()).toBe(true);
        });

        it('isBusy reflects loading/saving status', () => {
            expect(store.isBusy()).toBe(false);
            adjust.brightnessChanged(20);
            expect(store.isBusy()).toBe(true);

            lifecycle.previewLoaded();
            expect(store.isBusy()).toBe(false);
        });
    });

    describe('preview lifecycle', () => {
        it('silently retries failures up to the budget before surfacing the error', () => {
            let bust = store.cacheBust();

            // Each failure within the budget stays loading and bumps the cache-bust
            // for a fresh attempt; the error is never surfaced yet.
            for (let attempt = 1; attempt <= 3; attempt++) {
                lifecycle.previewErrored();
                expect(store.previewStatus()).toBe('loading');
                expect(store.cacheBust()).toBe(bust + 1);
                expect(store.error()).toBeNull();
                bust = store.cacheBust();
            }

            // The failure past the budget surfaces the error.
            lifecycle.previewErrored();
            expect(store.previewStatus()).toBe('error');
            expect(store.error()).toBe('Failed to render preview');
        });

        it('previewLoaded sets status loaded, clears error and resets the retry budget', () => {
            // Exhaust the budget (3 silent retries) then fail once more to reach error.
            for (let attempt = 0; attempt < 4; attempt++) {
                lifecycle.previewErrored();
            }
            expect(store.previewStatus()).toBe('error');

            lifecycle.previewLoaded();
            expect(store.previewStatus()).toBe('loaded');
            expect(store.error()).toBeNull();

            // Budget restored: a later single failure retries again rather than erroring.
            lifecycle.previewErrored();
            expect(store.previewStatus()).toBe('loading');
        });

        it('retryRequested reloads and restores the silent-retry budget', () => {
            // Exhaust the budget so the editor is in the error state.
            for (let attempt = 0; attempt < 4; attempt++) {
                lifecycle.previewErrored();
            }
            expect(store.previewStatus()).toBe('error');
            const bust = store.cacheBust();

            lifecycle.retryRequested();
            expect(store.previewStatus()).toBe('loading');
            expect(store.cacheBust()).toBe(bust + 1);

            // Budget reset: the next failure retries (loading) rather than erroring.
            lifecycle.previewErrored();
            expect(store.previewStatus()).toBe('loading');
        });
    });

    describe('previewUrl', () => {
        it('recomputes when a slice changes', () => {
            lifecycle.assetRequested(OPEN_PARAMS);
            const before = store.previewUrl();

            adjust.brightnessChanged(40);
            const after = store.previewUrl();

            expect(after).not.toBe(before);
            expect(after).toContain('/filter/');
        });
    });

    describe('debounced size effect', () => {
        it('fires once after 250ms and updates currentBytes', () => {
            lifecycle.assetRequested(OPEN_PARAMS);
            service.getFileSize.mockClear();
            service.getFileSize.mockReturnValue(of(4242));

            adjust.brightnessChanged(10);
            adjust.brightnessChanged(20);

            jest.advanceTimersByTime(250);

            expect(service.getFileSize).toHaveBeenCalledTimes(1);
            expect(store.fileInfo().currentBytes).toBe(4242);
        });
    });

    describe('asset loading', () => {
        it('assetRequested -> loadAssetMeta -> assetLoaded patches the context', () => {
            lifecycle.assetRequested(OPEN_PARAMS);

            expect(service.loadAssetMeta).toHaveBeenCalled();
            expect(store.assetContext().idOrTempId).toBe('inode-1');
            expect(store.assetContext().originalUrl).toBe('/contentAsset/image/inode-1/fileAsset');
            expect(store.assetContext().naturalWidth).toBe(800);
            expect(store.assetContext().naturalHeight).toBe(600);
            expect(store.fileInfo().originalBytes).toBe(5000);
        });

        it('surfaces a load failure when loadAssetMeta errors', () => {
            service.loadAssetMeta.mockReturnValue(throwError(() => new Error('boom')));

            lifecycle.assetRequested(OPEN_PARAMS);

            expect(store.previewStatus()).toBe('error');
            expect(store.error()).toBe('boom');
        });
    });

    describe('download', () => {
        it('triggers a download of the current preview', () => {
            lifecycle.assetRequested(OPEN_PARAMS);

            lifecycle.downloadRequested();

            expect(service.triggerDownload).toHaveBeenCalledWith(store.previewUrl(), 'photo.png');
        });
    });
});
