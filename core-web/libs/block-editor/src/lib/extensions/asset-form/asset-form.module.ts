import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { DotSpinnerModule } from '@dotcms/ui';

import { AssetFormComponent } from './asset-form.component';
import { DotAssetCardComponent } from './components/dot-asset-search/components/dot-asset-card/dot-asset-card.component';
import { DotAssetCardListComponent } from './components/dot-asset-search/components/dot-asset-card-list/dot-asset-card-list.component';
import { DotAssetCardSkeletonComponent } from './components/dot-asset-search/components/dot-asset-card-skeleton/dot-asset-card-skeleton.component';
import { DotAssetSearchComponent } from './components/dot-asset-search/dot-asset-search.component';
import { DotExternalAssetComponent } from './components/dot-external-asset/dot-external-asset.component';
import { DotAssetPreviewComponent } from './components/dot-upload-asset/components/dot-asset-preview/dot-asset-preview.component';
import { DotUploadAssetComponent } from './components/dot-upload-asset/dot-upload-asset.component';

import { DotUploadFileService } from '../../shared';
import { PrimengModule } from '../../shared/primeng.module';

@NgModule({
    imports: [CommonModule, FormsModule, ReactiveFormsModule, DotSpinnerModule, PrimengModule],
    declarations: [
        AssetFormComponent,
        DotAssetCardListComponent,
        DotAssetCardComponent,
        DotAssetCardSkeletonComponent,
        DotExternalAssetComponent,
        DotAssetSearchComponent,
        DotUploadAssetComponent,
        DotAssetPreviewComponent
    ],
    providers: [DotUploadFileService],
    exports: [AssetFormComponent, DotAssetSearchComponent]
})
export class AssetFormModule {}
