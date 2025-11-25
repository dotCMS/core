import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { AutoFocusModule } from 'primeng/autofocus';
import { CalendarModule } from 'primeng/calendar';
import { DropdownModule } from 'primeng/dropdown';
import { SelectButtonModule } from 'primeng/selectbutton';

import { PushPublishEnvSelectorModule } from '@components/_common/dot-push-publish-env-selector/dot-push-publish-env-selector.module';
import { DotPushPublishFormComponent } from '@components/_common/forms/dot-push-publish-form/dot-push-publish-form.component';
import { DotParseHtmlService } from '@dotcms/app/api/services/dot-parse-html/dot-parse-html.service';
import { PushPublishService } from '@dotcms/data-access';
import { DotcmsConfigService } from '@dotcms/dotcms-js';
import {
    DotDialogModule,
    DotFieldRequiredDirective,
    DotFieldValidationMessageComponent,
    DotMessagePipe,
    DotSafeHtmlPipe
} from '@dotcms/ui';

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
        DotFieldValidationMessageComponent,
        SelectButtonModule,
        DotSafeHtmlPipe,
        DotFieldRequiredDirective,
        DotMessagePipe
    ],
    providers: [PushPublishService, DotParseHtmlService, DotcmsConfigService]
})
export class DotPushPublishFormModule {}
