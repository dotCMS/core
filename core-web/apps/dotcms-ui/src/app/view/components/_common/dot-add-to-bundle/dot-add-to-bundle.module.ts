import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DropdownModule } from 'primeng/dropdown';

import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { AddToBundleService, DotCurrentUserService } from '@dotcms/data-access';
import { DotFieldValidationMessageComponent, DotMessagePipe, DotSafeHtmlPipe } from '@dotcms/ui';

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
        DotFieldValidationMessageComponent,
        DotSafeHtmlPipe,
        DotMessagePipe
    ],
    providers: [AddToBundleService, DotCurrentUserService]
})
export class DotAddToBundleModule {}
