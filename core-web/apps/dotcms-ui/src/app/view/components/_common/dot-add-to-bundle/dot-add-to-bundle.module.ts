import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { AddToBundleService, DotCurrentUserService } from '@dotcms/data-access';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { ButtonModule } from 'primeng/button';
import { DropdownModule } from 'primeng/dropdown';
import { DotFieldValidationMessageModule } from '../dot-field-validation-message/dot-file-validation-message.module';
import { DotAddToBundleComponent } from './dot-add-to-bundle.component';

@NgModule({
    declarations: [DotAddToBundleComponent],
    exports: [DotAddToBundleComponent],
    imports: [
        CommonModule,
        ButtonModule,
        FormsModule,
        DotDialogModule,
        ReactiveFormsModule,
        DropdownModule,
        DotFieldValidationMessageModule,
        DotPipesModule
    ],
    providers: [AddToBundleService, DotCurrentUserService]
})
export class DotAddToBundleModule {}
