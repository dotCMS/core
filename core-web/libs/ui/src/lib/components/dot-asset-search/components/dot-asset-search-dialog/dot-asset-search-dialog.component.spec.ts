import { createComponentFactory, Spectator } from '@ngneat/spectator';
import { MockComponent } from 'ng-mocks';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotAssetSearchDialogComponent } from './dot-asset-search-dialog.component';

import { DotAssetSearchComponent } from '../../dot-asset-search.component';

describe('DotAssetSearchDialogComponent', () => {
    let spectator: Spectator<DotAssetSearchDialogComponent>;
    let component: DotAssetSearchDialogComponent;

    const createComponent = createComponentFactory({
        component: DotAssetSearchDialogComponent,
        declarations: [MockComponent(DotAssetSearchComponent)],
        providers: [DynamicDialogRef, DynamicDialogConfig]
    });

    beforeEach(() => {
        spectator = createComponent();
        component = spectator.component;
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
