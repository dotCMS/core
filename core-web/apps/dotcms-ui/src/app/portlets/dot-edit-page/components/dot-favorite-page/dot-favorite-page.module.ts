import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
// import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
// import { MultiSelectModule } from 'primeng/multiselect';

import { DotFieldValidationMessageModule } from '@components/_common/dot-field-validation-message/dot-file-validation-message.module';
import { DotFormDialogModule } from '@components/dot-form-dialog/dot-form-dialog.module';
import { DotAutofocusModule } from '@directives/dot-autofocus/dot-autofocus.module';
import { DotTempFileUploadService } from '@dotcms/app/api/services/dot-temp-file-upload/dot-temp-file-upload.service';
import { DotPagesFavoritePageEmptySkeletonComponent } from '@dotcms/app/portlets/dot-pages/dot-pages-favorite-page-empty-skeleton/dot-pages-favorite-page-empty-skeleton.component';
import { DotWorkflowActionsFireService } from '@dotcms/data-access';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { DotFavoritePageComponent } from './dot-favorite-page.component';

@NgModule({
    declarations: [DotFavoritePageComponent],
    exports: [DotFavoritePageComponent],
    imports: [
        CommonModule,
        ButtonModule,
        DotAutofocusModule,
        DotFormDialogModule,
        DotFieldValidationMessageModule,
        DotPagesFavoritePageEmptySkeletonComponent,
        DotPipesModule,
        // DropdownModule,
        InputTextModule,
        ReactiveFormsModule
        // MultiSelectModule
    ],
    providers: [
        DotTempFileUploadService,
        DotWorkflowActionsFireService //DotRolesService
    ]
})
export class DotFavoritePageModule {}
