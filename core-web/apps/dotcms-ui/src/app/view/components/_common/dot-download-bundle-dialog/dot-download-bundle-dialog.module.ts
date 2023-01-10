import { NgModule } from '@angular/core';
import { DotPushPublishFiltersService } from '@dotcms/data-access';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { DotDownloadBundleDialogComponent } from '@components/_common/dot-download-bundle-dialog/dot-download-bundle-dialog.component';
import { DotDownloadBundleDialogService } from '@dotcms/app/api/services/dot-download-bundle-dialog/dot-download-bundle-dialog.service';
import { DropdownModule } from 'primeng/dropdown';
import { SelectButtonModule } from 'primeng/selectbutton';

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
