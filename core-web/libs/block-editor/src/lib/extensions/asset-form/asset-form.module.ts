import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { Tabs, TabList, Tab, TabPanels, TabPanel } from 'primeng/tabs';
import { FileUpload } from 'primeng/fileupload';
import { Button } from 'primeng/button';
import { InputText } from 'primeng/inputtext';

import { DotAssetSearchComponent, DotSpinnerComponent } from '@dotcms/ui';

import { AssetFormComponent } from './asset-form.component';
import { DotExternalAssetComponent } from './components/dot-external-asset/dot-external-asset.component';
import { DotAssetPreviewComponent } from './components/dot-upload-asset/components/dot-asset-preview/dot-asset-preview.component';
import { DotUploadAssetComponent } from './components/dot-upload-asset/dot-upload-asset.component';

@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        DotSpinnerComponent,
        Tabs,
        TabList,
        Tab,
        TabPanels,
        TabPanel,
        FileUpload,
        Button,
        InputText,
        DotAssetSearchComponent
    ],
    declarations: [
        AssetFormComponent,
        DotExternalAssetComponent,
        DotUploadAssetComponent,
        DotAssetPreviewComponent
    ],
    exports: [AssetFormComponent]
})
export class AssetFormModule {}
