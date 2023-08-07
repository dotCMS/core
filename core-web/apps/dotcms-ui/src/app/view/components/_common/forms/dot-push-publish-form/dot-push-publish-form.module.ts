import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { AutoFocusModule } from 'primeng/autofocus';
import { CalendarModule } from 'primeng/calendar';
import { DropdownModule } from 'primeng/dropdown';
import { SelectButtonModule } from 'primeng/selectbutton';

import { DotFieldValidationMessageModule } from '@components/_common/dot-field-validation-message/dot-file-validation-message.module';
import { PushPublishEnvSelectorModule } from '@components/_common/dot-push-publish-env-selector/dot-push-publish-env-selector.module';
import { DotPushPublishFormComponent } from '@components/_common/forms/dot-push-publish-form/dot-push-publish-form.component';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { DotParseHtmlService } from '@dotcms/app/api/services/dot-parse-html/dot-parse-html.service';
import { PushPublishService } from '@dotcms/app/api/services/push-publish/push-publish.service';
import { DotcmsConfigService } from '@dotcms/dotcms-js';
import { DotFieldRequiredDirective, DotMessagePipe } from '@dotcms/ui';
import { DotPipesModule } from '@pipes/dot-pipes.module';

@NgModule({
    declarations: [DotPushPublishFormComponent],
    exports: [DotPushPublishFormComponent],
    imports: [
        CommonModule,
        AutoFocusModule,
        FormsModule,
        CalendarModule,
        DotDialogModule,
        PushPublishEnvSelectorModule,
        ReactiveFormsModule,
        DropdownModule,
        DotFieldValidationMessageModule,
        SelectButtonModule,
        DotPipesModule,
        DotFieldRequiredDirective,
        DotMessagePipe
    ],
    providers: [PushPublishService, DotParseHtmlService, DotcmsConfigService]
})
export class DotPushPublishFormModule {}
