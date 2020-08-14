import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DropdownModule, SelectButtonModule, CalendarModule } from 'primeng/primeng';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { PushPublishService } from '@services/push-publish/push-publish.service';
import { DotPushPublishFormComponent } from '@components/_common/forms/dot-push-publish-form/dot-push-publish-form.component';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { PushPublishEnvSelectorModule } from '@components/_common/dot-push-publish-env-selector/dot-push-publish-env-selector.module';
import { DotFieldValidationMessageModule } from '@components/_common/dot-field-validation-message/dot-file-validation-message.module';
import { DotParseHtmlService } from '@services/dot-parse-html/dot-parse-html.service';

@NgModule({
    declarations: [DotPushPublishFormComponent],
    exports: [DotPushPublishFormComponent],
    imports: [
        CommonModule,
        FormsModule,
        CalendarModule,
        DotDialogModule,
        PushPublishEnvSelectorModule,
        ReactiveFormsModule,
        DropdownModule,
        DotFieldValidationMessageModule,
        SelectButtonModule,
        DotPipesModule,
    ],
    providers: [PushPublishService, DotParseHtmlService]
})
export class DotPushPublishFormModule {}
