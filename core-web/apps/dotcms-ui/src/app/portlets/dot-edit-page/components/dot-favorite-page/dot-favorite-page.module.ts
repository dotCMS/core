import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { DotFormDialogModule } from '@components/dot-form-dialog/dot-form-dialog.module';
import { DotFieldValidationMessageModule } from '@components/_common/dot-field-validation-message/dot-file-validation-message.module';
import { DotAutofocusModule } from '@directives/dot-autofocus/dot-autofocus.module';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { DotFavoritePageComponent } from './dot-favorite-page.component';
import { DotTempFileUploadService } from '@dotcms/app/api/services/dot-temp-file-upload/dot-temp-file-upload.service';
import { DotWorkflowActionsFireService } from '@dotcms/data-access';
import { DotRolesService } from '@dotcms/data-access';
import { MultiSelectModule } from 'primeng/multiselect';
import { ButtonModule } from 'primeng/button';

@NgModule({
    declarations: [DotFavoritePageComponent],
    exports: [DotFavoritePageComponent],
    imports: [
        CommonModule,
        ButtonModule,
        DotAutofocusModule,
        DotFormDialogModule,
        DotFieldValidationMessageModule,
        DotPipesModule,
        DropdownModule,
        InputTextModule,
        ReactiveFormsModule,
        MultiSelectModule
    ],
    providers: [DotTempFileUploadService, DotWorkflowActionsFireService, DotRolesService]
})
export class DotFavoritePageModule {}
