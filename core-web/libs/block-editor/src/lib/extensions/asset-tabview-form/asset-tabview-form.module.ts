import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { AssetTabviewFormComponent } from './asset-tabview-form.component';
import { DotAssetCardListComponent } from './components/dot-asset-search/components/dot-asset-card-list/dot-asset-card-list.component';
import { DotAssetCardSkeletonComponent } from './components/dot-asset-search/components/dot-asset-card-skeleton/dot-asset-card-skeleton.component';
import { DotAssetCardComponent } from './components/dot-asset-search/components/dot-asset-card/dot-asset-card.component';
import { DotAssetSearchComponent } from './components/dot-asset-search/dot-asset-search.component';
import { DotExternalAssetComponent } from './components/dot-external-asset/dot-external-asset.component';

import { PrimengModule } from '../../shared/primeng.module';
import { DotImageService } from '../image-uploader/services/dot-image/dot-image.service';

@NgModule({
    imports: [CommonModule, FormsModule, ReactiveFormsModule, PrimengModule],
    declarations: [
        AssetTabviewFormComponent,
        DotAssetCardListComponent,
        DotAssetCardComponent,
        DotAssetCardSkeletonComponent,
        DotExternalAssetComponent,
        DotAssetSearchComponent
    ],
    providers: [DotImageService],
    exports: [AssetTabviewFormComponent, DotAssetSearchComponent]
})
export class AssetTabviewFormModule {}
