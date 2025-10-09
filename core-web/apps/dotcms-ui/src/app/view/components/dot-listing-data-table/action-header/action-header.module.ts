import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { SplitButtonModule } from 'primeng/splitbutton';

import { DotMessagePipe, DotSafeHtmlPipe } from '@dotcms/ui';

import { ActionHeaderComponent } from './action-header.component';

import { DotActionButtonComponent } from '../../_common/dot-action-button/dot-action-button.component';

@NgModule({
    bootstrap: [],
    declarations: [ActionHeaderComponent],
    exports: [ActionHeaderComponent],
    imports: [
        CommonModule,
        DotActionButtonComponent,
        SplitButtonModule,
        DotSafeHtmlPipe,
        DotMessagePipe
    ],
    providers: []
})
export class ActionHeaderModule {}
