import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { TooltipModule } from 'primeng/tooltip';

import { DotPageToolsSeoComponent } from '@dotcms/portlets/dot-ema/ui';
import { DotIconComponent, DotMessagePipe, DotSafeHtmlPipe } from '@dotcms/ui';

import { DotEditPageNavComponent } from './dot-edit-page-nav.component';

@NgModule({
    imports: [
        CommonModule,
        RouterModule,
        TooltipModule,
        DotIconComponent,
        DotSafeHtmlPipe,
        DotMessagePipe,
        DotPageToolsSeoComponent
    ],
    declarations: [DotEditPageNavComponent],
    exports: [DotEditPageNavComponent]
})
export class DotEditPageNavModule {}
