import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { DotAssetSearchComponent, DotSpinnerModule } from '@dotcms/ui';

import { AssetFormComponent } from './asset-form.component';
import { DotExternalAssetComponent } from './components/dot-external-asset/dot-external-asset.component';
import { DotAssetPreviewComponent } from './components/dot-upload-asset/components/dot-asset-preview/dot-asset-preview.component';
import { DotUploadAssetComponent } from './components/dot-upload-asset/dot-upload-asset.component';

import { DotUploadFileService } from '../../shared';
import { PrimengModule } from '../../shared/primeng.module';

@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        DotSpinnerModule,
        PrimengModule,
        DotAssetSearchComponent
    ],
    declarations: [
        AssetFormComponent,
        DotExternalAssetComponent,
        DotUploadAssetComponent,
        DotAssetPreviewComponent
    ],
    providers: [DotUploadFileService],
    exports: [AssetFormComponent]
})
export class AssetFormModule {}
