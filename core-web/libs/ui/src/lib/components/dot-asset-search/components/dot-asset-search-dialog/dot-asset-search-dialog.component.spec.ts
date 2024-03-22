import { createComponentFactory, Spectator } from '@ngneat/spectator';
import { MockComponent } from 'ng-mocks';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { EMPTY_CONTENTLET } from '@dotcms/utils-testing';

import { DotAssetSearchDialogComponent } from './dot-asset-search-dialog.component';

import { DotAssetSearchComponent } from '../../dot-asset-search.component';

describe('DotAssetSearchDialogComponent', () => {
    let spectator: Spectator<DotAssetSearchDialogComponent>;
    let dynamicDialogRef: DynamicDialogRef;
    const createComponent = createComponentFactory({
        component: DotAssetSearchDialogComponent,
        declarations: [MockComponent(DotAssetSearchComponent)],
        providers: [
            {
                provide: DynamicDialogRef,
                useValue: {
                    close: (_) => {
                        /* */
                    }
                }
            },
            {
                provide: DynamicDialogConfig,
                useValue: {
                    data: {
                        assetType: 'image'
                    }
                }
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
        dynamicDialogRef = spectator.inject(DynamicDialogRef, true);
    });

    it('should set editorAssetType from config data', () => {
        const dotAssetSearchComponent = spectator.query(DotAssetSearchComponent);
        expect(dotAssetSearchComponent.type).toBe('image');
    });

    it('should close dialog with selected asset on addAsset', () => {
        const spy = spyOn(dynamicDialogRef, 'close');
        spectator.triggerEventHandler(DotAssetSearchComponent, 'addAsset', EMPTY_CONTENTLET);
        expect(spy).toHaveBeenCalledWith(EMPTY_CONTENTLET);
    });
});
