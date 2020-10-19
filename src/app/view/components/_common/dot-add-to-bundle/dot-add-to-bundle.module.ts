import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { DotFieldValidationMessageModule } from '../dot-field-validation-message/dot-file-validation-message.module';
import { DotAddToBundleComponent } from './dot-add-to-bundle.component';
import { AddToBundleService } from '@services/add-to-bundle/add-to-bundle.service';
import { DotCurrentUserService } from '@services/dot-current-user/dot-current-user.service';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { ButtonModule } from 'primeng/button';
import { DropdownModule } from 'primeng/dropdown';

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
