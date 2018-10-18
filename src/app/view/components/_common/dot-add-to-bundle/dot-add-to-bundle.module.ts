import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ButtonModule, DropdownModule } from 'primeng/primeng';
import { FieldValidationMessageModule } from '../field-validation-message/file-validation-message.module';
import { DotAddToBundleComponent } from './dot-add-to-bundle.component';
import { AddToBundleService } from '@services/add-to-bundle/add-to-bundle.service';
import { DotCurrentUserService } from '@services/dot-current-user/dot-current-user.service';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';

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
        FieldValidationMessageModule
    ],
    providers: [AddToBundleService, DotCurrentUserService]
})
export class DotAddToBundleModule {}
