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
import { DotWorkflowActionsFireService } from '@dotcms/app/api/services/dot-workflow-actions-fire/dot-workflow-actions-fire.service';
import { DotRolesService } from '@dotcms/app/api/services/dot-roles/dot-roles.service';
import { MultiSelectModule } from 'primeng/multiselect';

@NgModule({
    declarations: [DotFavoritePageComponent],
    exports: [DotFavoritePageComponent],
    imports: [
        CommonModule,
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
