import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { AutoFocusModule } from 'primeng/autofocus';
import { CalendarModule } from 'primeng/calendar';
import { DropdownModule } from 'primeng/dropdown';
import { SelectButtonModule } from 'primeng/selectbutton';

import { PushPublishService } from '@dotcms/data-access';
import { DotcmsConfigService } from '@dotcms/dotcms-js';
import {
    DotDialogComponent,
    DotFieldRequiredDirective,
    DotFieldValidationMessageComponent,
    DotMessagePipe,
    DotSafeHtmlPipe
} from '@dotcms/ui';

import { DotPushPublishFormComponent } from './dot-push-publish-form.component';

import { DotParseHtmlService } from '../../../../../api/services/dot-parse-html/dot-parse-html.service';
import { PushPublishEnvSelectorModule } from '../../dot-push-publish-env-selector/dot-push-publish-env-selector.module';

@NgModule({
    declarations: [DotPushPublishFormComponent],
    exports: [DotPushPublishFormComponent],
    imports: [
        CommonModule,
        AutoFocusModule,
        FormsModule,
        CalendarModule,
        DotDialogComponent,
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
