import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { DotPushPublishContentTypesDialogComponent } from './dot-push-publish-dialog.component';
import { CalendarModule, DropdownModule, CheckboxModule } from 'primeng/primeng';
import { PushPublishEnvSelectorModule } from '../dot-push-publish-env-selector/dot-push-publish-env-selector.module';
import { DotFieldValidationMessageModule } from '../dot-field-validation-message/dot-file-validation-message.module';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';

@NgModule({
    declarations: [DotPushPublishContentTypesDialogComponent],
    exports: [DotPushPublishContentTypesDialogComponent],
    imports: [
        CommonModule,
        FormsModule,
        CalendarModule,
        DotDialogModule,
        PushPublishEnvSelectorModule,
        ReactiveFormsModule,
        DropdownModule,
        DotFieldValidationMessageModule,
        CheckboxModule
    ]
})
export class DotPushPublishContentTypesDialogModule {}
