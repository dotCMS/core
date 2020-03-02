import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { CalendarModule, DropdownModule, CheckboxModule } from 'primeng/primeng';
import { PushPublishEnvSelectorModule } from '../dot-push-publish-env-selector/dot-push-publish-env-selector.module';
import { DotFieldValidationMessageModule } from '../dot-field-validation-message/dot-file-validation-message.module';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { DotPushPublishFiltersService } from '@services/dot-push-publish-filters/dot-push-publish-filters.service';
import { DotPushPublishDialogComponent } from '@components/_common/dot-push-publish-dialog/dot-push-publish-dialog.component';

@NgModule({
    declarations: [DotPushPublishDialogComponent],
    exports: [DotPushPublishDialogComponent],
    providers: [DotPushPublishFiltersService],
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
export class DotPushPublishDialogModule {}
