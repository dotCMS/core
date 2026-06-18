import { mockProvider, SpyObject } from '@ngneat/spectator/jest';
import { Dispatcher, injectDispatch } from '@ngrx/signals/events';
import { of, Subject, throwError } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Injector, runInInjectionContext } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { DotHttpErrorManagerService, DotMessageService } from '@dotcms/data-access';
import { DotCMSTempFile } from '@dotcms/dotcms-models';

import {
    imageEditorHistoryEvents,
    imageEditorLifecycleEvents,
    imageEditorPanelEvents,
    imageEditorToolEvents
} from './image-editor.events';
import { ImageEditorStore } from './image-editor.store';

import { ImageEditorOpenParams } from '../models/image-editor.models';
import { DotImageEditorService } from '../services/dot-image-editor.service';

const TEMP_FILE: DotCMSTempFile = {
    id: 'temp-123',
    fileName: 'edited.png',
    folder: '',
    image: true,
    length: 2048,
    mimeType: 'image/png',
    referenceUrl: '',
    thumbnailUrl: ''
};

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
    let httpErrorManager: SpyObject<DotHttpErrorManagerService>;
    let injector: Injector;

    // Self-dispatching event groups, resolved within the injection context.
    let panel: ReturnType<typeof injectDispatch<typeof imageEditorPanelEvents>>;
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
                    persistFocalPoint: jest.fn().mockReturnValue(of(void 0)),
                    saveEditedImage: jest.fn().mockReturnValue(of(TEMP_FILE)),
                    triggerDownload: jest.fn()
                }),
                mockProvider(DotHttpErrorManagerService, {
                    handle: jest.fn().mockReturnValue(of({}))
                }),
                mockProvider(DotMessageService, { get: jest.fn((key: string) => key) })
            ]
        });

        injector = TestBed.inject(Injector);
        // Instantiating the store runs its `onInit` effects.
        store = TestBed.inject(ImageEditorStore);
        service = TestBed.inject(DotImageEditorService) as SpyObject<DotImageEditorService>;
        httpErrorManager = TestBed.inject(
            DotHttpErrorManagerService
        ) as SpyObject<DotHttpErrorManagerService>;

        runInInjectionContext(injector, () => {
            panel = injectDispatch(imageEditorPanelEvents);
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
            panel.brightnessChanged(150);

            expect(store.adjust().brightness).toBe(100);
            expect(store.history()).toHaveLength(1);
            expect(store.history()[0].category).toBe('adjust');
            expect(store.history()[0].label).toBe('Brightness 100');
            expect(store.historyIndex()).toBe(0);
            expect(store.cacheBust()).toBe(1);
            expect(store.previewStatus()).toBe('loading');
        });

        it('should coalesce rapid same-category edits into a single entry', () => {
            panel.brightnessChanged(10);
            panel.brightnessChanged(20);
            panel.saturationChanged(30);

            expect(store.adjust().brightness).toBe(20);
            expect(store.adjust().saturation).toBe(30);
            expect(store.history()).toHaveLength(1);
            expect(store.history()[0].label).toBe('Saturation 30');
        });

        it('should append a new entry when the category changes', () => {
            panel.brightnessChanged(10);
            panel.rotateChanged(90);

            expect(store.history()).toHaveLength(2);
            expect(store.history()[0].category).toBe('adjust');
            expect(store.history()[1].category).toBe('rotate');
            expect(store.history()[1].label).toBe('Rotate 90°');
        });

        it('should clamp scale and reset crop (resize XOR crop)', () => {
            tool.cropApplied({ x: 0, y: 0, w: 100, h: 100, active: false, aspect: null });
            panel.scaleChanged(500);

            expect(store.transform().scale).toBe(400);
            expect(store.crop().active).toBe(false);
        });

        it('should toggle flip flags under the flip category', () => {
            panel.flipHToggled();
            panel.flipVToggled();

            expect(store.transform().flipH).toBe(true);
            expect(store.transform().flipV).toBe(true);
            expect(store.history().some((entry) => entry.category === 'flip')).toBe(true);
        });

        it('should set compression under the compression category', () => {
            panel.compressionChanged('jpeg');

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
            panel.scaleChanged(50);
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
    });

    describe('history', () => {
        it('should remove a specific edit and recompute slices', () => {
            panel.brightnessChanged(20);
            panel.rotateChanged(45);
            const rotateId = store.history()[1].id;

            history.editRemoved({ id: rotateId });

            expect(store.history()).toHaveLength(1);
            expect(store.transform().rotateDeg).toBe(0);
            expect(store.adjust().brightness).toBe(20);
        });

        it('should keep the head on the correct entry when a middle edit is removed', () => {
            panel.brightnessChanged(20);
            panel.rotateChanged(45);
            panel.saturationChanged(30);

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
            // Slices restore from the new head's snapshot, which carries the
            // brightness that was active when the rotation was applied.
            expect(store.transform().rotateDeg).toBe(45);
            expect(store.adjust().brightness).toBe(20);
            expect(store.adjust().saturation).toBe(0);
        });

        it('should undo and redo edits', () => {
            panel.brightnessChanged(20);
            panel.rotateChanged(45);

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
            panel.brightnessChanged(20);

            history.undoRequested();
            history.undoRequested();
            expect(store.historyIndex()).toBe(-1);

            history.redoRequested();
            history.redoRequested();
            expect(store.historyIndex()).toBe(0);
        });

        it('should reset everything', () => {
            panel.brightnessChanged(20);
            panel.rotateChanged(45);

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

            panel.brightnessChanged(20);
            expect(store.canUndo()).toBe(true);
            expect(store.canRedo()).toBe(false);

            history.undoRequested();
            expect(store.canUndo()).toBe(false);
            expect(store.canRedo()).toBe(true);
        });

        it('isDirty becomes true only with a non-empty filter chain', () => {
            expect(store.isDirty()).toBe(false);
            panel.brightnessChanged(20);
            expect(store.isDirty()).toBe(true);
        });

        it('isBusy reflects loading/saving status', () => {
            expect(store.isBusy()).toBe(false);
            panel.brightnessChanged(20);
            expect(store.isBusy()).toBe(true);

            lifecycle.previewLoaded();
            expect(store.isBusy()).toBe(false);
        });

        it('canSave requires loaded preview, not saving and dirty', () => {
            expect(store.canSave()).toBe(false);

            panel.brightnessChanged(20);
            expect(store.canSave()).toBe(false); // still loading

            lifecycle.previewLoaded();
            expect(store.canSave()).toBe(true);
        });
    });

    describe('preview lifecycle', () => {
        it('previewLoaded sets status loaded and clears error', () => {
            lifecycle.previewErrored();
            expect(store.previewStatus()).toBe('error');

            lifecycle.previewLoaded();
            expect(store.previewStatus()).toBe('loaded');
            expect(store.error()).toBeNull();
        });

        it('previewErrored sets the error status', () => {
            lifecycle.previewErrored();
            expect(store.previewStatus()).toBe('error');
            expect(store.error()).toBe('Failed to render preview');
        });
    });

    describe('previewUrl', () => {
        it('recomputes when a slice changes', () => {
            lifecycle.assetRequested(OPEN_PARAMS);
            const before = store.previewUrl();

            panel.brightnessChanged(40);
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

            panel.brightnessChanged(10);
            panel.brightnessChanged(20);

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
    });

    describe('save', () => {
        it('success sets the saved temp file and saved status', () => {
            lifecycle.assetRequested(OPEN_PARAMS);
            panel.brightnessChanged(20);
            lifecycle.saveRequested();

            expect(service.saveEditedImage).toHaveBeenCalled();
            expect(store.savedTempFile()).toEqual(TEMP_FILE);
            expect(store.saveStatus()).toBe('saved');
        });

        it('persists the focal point before saving when active', () => {
            lifecycle.assetRequested(OPEN_PARAMS);
            tool.focalPointSet({ x: 0.3, y: 0.4 });
            lifecycle.saveRequested();

            expect(service.persistFocalPoint).toHaveBeenCalledWith(expect.any(String), {
                x: 0.3,
                y: 0.4
            });
            expect(service.saveEditedImage).toHaveBeenCalled();
        });

        it('does not persist the focal point when inactive', () => {
            lifecycle.assetRequested(OPEN_PARAMS);
            panel.brightnessChanged(20);
            lifecycle.saveRequested();

            expect(service.persistFocalPoint).not.toHaveBeenCalled();
        });

        it('error path handles the error and sets save status to error', () => {
            const error = new HttpErrorResponse({ status: 500 });
            service.saveEditedImage.mockReturnValue(throwError(() => error));

            lifecycle.assetRequested(OPEN_PARAMS);
            panel.brightnessChanged(20);
            lifecycle.saveRequested();

            expect(httpErrorManager.handle).toHaveBeenCalledWith(error);
            expect(store.saveStatus()).toBe('error');
        });

        it('ignores a second save while one is in flight and never strands saving', () => {
            // Defer the first save so a second request arrives mid-flight.
            const inFlight = new Subject<DotCMSTempFile>();
            service.saveEditedImage.mockReturnValue(inFlight.asObservable());

            lifecycle.assetRequested(OPEN_PARAMS);
            panel.brightnessChanged(20);

            lifecycle.saveRequested();
            expect(store.saveStatus()).toBe('saving');

            // A second trigger while saving must be ignored (exhaustMap), not
            // cancel the first and strand the store in 'saving'.
            lifecycle.saveAsRequested();

            // Complete the original save.
            inFlight.next(TEMP_FILE);
            inFlight.complete();

            expect(service.saveEditedImage).toHaveBeenCalledTimes(1);
            expect(store.savedTempFile()).toEqual(TEMP_FILE);
            expect(store.saveStatus()).toBe('saved');
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
