import { NgModule } from '@angular/core';
import { DotPushPublishFiltersService } from '@services/dot-push-publish-filters/dot-push-publish-filters.service';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { DropdownModule, SelectButtonModule } from 'primeng/primeng';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { DotDownloadBundleDialogComponent } from '@components/_common/dot-download-bundle-dialog/dot-download-bundle-dialog.component';
import { DotDownloadBundleDialogService } from '@services/dot-download-bundle-dialog/dot-download-bundle-dialog.service';

@NgModule({
    declarations: [DotDownloadBundleDialogComponent],
    exports: [DotDownloadBundleDialogComponent],
    providers: [DotPushPublishFiltersService, DotDownloadBundleDialogService],
    imports: [
        CommonModule,
        FormsModule,
        DotDialogModule,
        ReactiveFormsModule,
        DropdownModule,
        SelectButtonModule,
        DotPipesModule
    ]
})
export class DotDownloadBundleDialogModule {}
