import { signalStore, withState } from '@ngrx/signals';
import { Dispatcher, injectDispatch } from '@ngrx/signals/events';
import { of } from 'rxjs';

import { Injector, runInInjectionContext } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { withAsset } from './with-asset.feature';

import { ImageEditorOpenParams } from '../../models/image-editor.models';
import { DotImageEditorService } from '../../services/dot-image-editor.service';
import { imageEditorLifecycleEvents } from '../image-editor.events';
import { initialFocalPointState, initialImageEditorState } from '../image-editor.state';

const AssetStore = signalStore(withState(initialImageEditorState), withAsset());

const BASE_PARAMS: ImageEditorOpenParams = {
    inode: 'inode-1',
    variable: 'fileAsset',
    fieldName: 'fileAsset',
    fileName: 'photo.png',
    mimeType: 'image/png'
};

describe('withAsset', () => {
    let store: InstanceType<typeof AssetStore>;
    let lifecycle: ReturnType<typeof injectDispatch<typeof imageEditorLifecycleEvents>>;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                AssetStore,
                Dispatcher,
                {
                    provide: DotImageEditorService,
                    useValue: {
                        loadAssetMeta: jest
                            .fn()
                            .mockReturnValue(
                                of({ naturalWidth: 800, naturalHeight: 600, originalBytes: 5000 })
                            )
                    }
                }
            ]
        });
        const injector = TestBed.inject(Injector);
        store = TestBed.inject(AssetStore);
        runInInjectionContext(injector, () => {
            lifecycle = injectDispatch(imageEditorLifecycleEvents);
        });
    });

    it('seeds focalPoint and seededFocalPoint from the open params', () => {
        lifecycle.assetRequested({ ...BASE_PARAMS, focalPoint: { x: 0.3, y: 0.7 } });

        expect(store.focalPoint()).toEqual({ x: 0.3, y: 0.7 });
        expect(store.seededFocalPoint()).toEqual({ x: 0.3, y: 0.7 });
    });

    it('defaults focalPoint and seededFocalPoint to the center when none is provided', () => {
        lifecycle.assetRequested(BASE_PARAMS);

        expect(store.focalPoint()).toEqual(initialFocalPointState);
        expect(store.seededFocalPoint()).toEqual(initialFocalPointState);
    });
});
