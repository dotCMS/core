import { signalStore, signalStoreFeature, type, withComputed, withState } from '@ngrx/signals';
import { Dispatcher, injectDispatch } from '@ngrx/signals/events';
import { NEVER, of, throwError } from 'rxjs';

import { computed, Injector, runInInjectionContext } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { DotCMSTempFile } from '@dotcms/dotcms-models';

import { withSave } from './with-save.feature';

import { AppliedFilter, ImageEditorState } from '../../models/image-editor.models';
import { DotImageEditorService } from '../../services/dot-image-editor.service';
import { imageEditorLifecycleEvents } from '../image-editor.events';
import { initialImageEditorState } from '../image-editor.state';

// `withSave` consumes the `appliedFilters` prop from `withPreview`; stub it with a
// fixed empty chain so the feature can be exercised in isolation.
function withAppliedFiltersStub() {
    return signalStoreFeature(
        type<{ state: ImageEditorState }>(),
        withComputed(() => ({ appliedFilters: computed<AppliedFilter[]>(() => []) }))
    );
}

// Seed a realistic asset context + a moved focal point so the URL the feature builds
// (store signals -> buildSaveUrl) can be asserted, not only the status transitions.
const SaveStore = signalStore(
    withState({
        ...initialImageEditorState,
        assetContext: {
            ...initialImageEditorState.assetContext,
            variable: 'binary',
            originalUrl: '/contentAsset/image/abc123/binary'
        },
        focalPoint: { x: 0.25, y: 0.75 }
    }),
    withAppliedFiltersStub(),
    withSave()
);

const TEMP_FILE: DotCMSTempFile = {
    fileName: 'edited.png',
    folder: 'shared',
    id: 'temp_578026b7cc',
    image: true,
    length: 1024,
    mimeType: 'image/png',
    referenceUrl: '/dA/temp_578026b7cc',
    thumbnailUrl: '/dA/temp_578026b7cc/thumb'
};

describe('withSave', () => {
    let store: InstanceType<typeof SaveStore>;
    let service: { saveEditedImage: jest.Mock };
    let lifecycle: ReturnType<typeof injectDispatch<typeof imageEditorLifecycleEvents>>;

    function setup(): jest.SpyInstance {
        const dispatchSpy = jest.spyOn(Dispatcher.prototype, 'dispatch');

        TestBed.configureTestingModule({
            providers: [
                SaveStore,
                Dispatcher,
                { provide: DotImageEditorService, useValue: service }
            ]
        });
        const injector = TestBed.inject(Injector);
        store = TestBed.inject(SaveStore);
        runInInjectionContext(injector, () => {
            lifecycle = injectDispatch(imageEditorLifecycleEvents);
        });

        return dispatchSpy;
    }

    beforeEach(() => {
        service = { saveEditedImage: jest.fn().mockReturnValue(of(TEMP_FILE)) };
    });

    afterEach(() => {
        jest.restoreAllMocks();
    });

    it('marks the editor saving on saveRequested while the save is in flight', () => {
        // A never-completing save holds the request pending so 'saving' is observable.
        service.saveEditedImage.mockReturnValue(NEVER);
        setup();

        lifecycle.saveRequested();

        expect(store.saveStatus()).toBe('saving');
        expect(store.saveError()).toBeNull();
    });

    it('dispatches saveSucceeded and returns to idle on success', () => {
        const dispatchSpy = setup();

        lifecycle.saveRequested();

        expect(service.saveEditedImage).toHaveBeenCalledTimes(1);
        // The editor only edits images, so the result is always flagged as an image and
        // seeded with the current focal point (see `enrichEditedImage`).
        expect(dispatchSpy).toHaveBeenCalledWith(
            imageEditorLifecycleEvents.saveSucceeded({
                ...TEMP_FILE,
                image: true,
                metadata: {
                    contentType: 'image/png',
                    fileSize: 1024,
                    length: 1024,
                    modDate: 0,
                    name: 'edited.png',
                    sha256: '',
                    title: 'edited.png',
                    version: 0,
                    isImage: true,
                    focalPoint: '0.25,0.75'
                }
            })
        );
        expect(store.saveStatus()).toBe('idle');
        expect(store.saveError()).toBeNull();
    });

    it('recognizes the edit as an image even when the servlet returns no metadata', () => {
        // Regression: the Save servlet can return the edited temp file as
        // `metadata: null, image: false, mimeType: "unknown"`. Without enrichment the
        // thumbnail, the "edit image" gate and the file-info dialog stop treating it as
        // an image (blank thumb / hidden pencil / crash on `metadata.title`).
        service.saveEditedImage.mockReturnValue(
            of({ ...TEMP_FILE, image: false, mimeType: 'unknown', metadata: null } as never)
        );
        const dispatchSpy = setup();

        lifecycle.saveRequested();

        expect(dispatchSpy).toHaveBeenCalledWith(
            imageEditorLifecycleEvents.saveSucceeded({
                ...TEMP_FILE,
                image: true,
                mimeType: 'unknown',
                metadata: {
                    contentType: 'unknown',
                    fileSize: 1024,
                    length: 1024,
                    modDate: 0,
                    name: 'edited.png',
                    sha256: '',
                    title: 'edited.png',
                    version: 0,
                    isImage: true,
                    focalPoint: '0.25,0.75'
                }
            })
        );
    });

    it('folds the current focal point into the returned temp metadata', () => {
        // The servlet returns a temp whose serialized metadata is basic (no focalPoint),
        // so the feature injects the focal so an in-session reopen can re-seed the marker.
        const metadata = {
            contentType: 'image/png',
            fileSize: 1024,
            isImage: true,
            length: 1024,
            modDate: 0,
            name: 'edited.png',
            sha256: 'abc',
            title: 'edited.png',
            version: 1
        };
        service.saveEditedImage.mockReturnValue(of({ ...TEMP_FILE, metadata }));
        const dispatchSpy = setup();

        lifecycle.saveRequested();

        expect(dispatchSpy).toHaveBeenCalledWith(
            imageEditorLifecycleEvents.saveSucceeded({
                ...TEMP_FILE,
                metadata: { ...metadata, focalPoint: '0.25,0.75' }
            })
        );
    });

    it('builds the Save URL from the asset context, filter chain and focal point', () => {
        setup();

        lifecycle.saveRequested();

        // assetContext.variable + originalUrl, the (empty) stubbed chain, and the moved
        // focal point all flow through buildSaveUrl into the single GET.
        expect(service.saveEditedImage).toHaveBeenCalledWith(
            '/contentAsset/image/abc123/binary/filter/FocalPoint/fp/0.25,0.75' +
                '?binaryFieldId=binary&_imageToolSaveFile=true&overwrite=true'
        );
    });

    it('dispatches saveFailed and surfaces the error on failure', () => {
        service.saveEditedImage.mockReturnValue(throwError(() => new Error('boom')));
        const dispatchSpy = setup();

        lifecycle.saveRequested();

        expect(dispatchSpy).toHaveBeenCalledWith(
            imageEditorLifecycleEvents.saveFailed(expect.any(Error))
        );
        expect(store.saveStatus()).toBe('error');
        expect(store.saveError()).toBe('boom');
    });
});
