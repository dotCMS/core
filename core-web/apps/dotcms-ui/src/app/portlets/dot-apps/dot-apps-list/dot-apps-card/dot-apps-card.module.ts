import { MarkdownModule } from 'ngx-markdown';

import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { AvatarModule } from 'primeng/avatar';
import { BadgeModule } from 'primeng/badge';
import { CardModule } from 'primeng/card';
import { TooltipModule } from 'primeng/tooltip';

import { DotAvatarDirective, DotIconModule, DotMessagePipe, DotSafeHtmlPipe } from '@dotcms/ui';

import { DotAppsCardComponent } from './dot-apps-card.component';

@NgModule({
    imports: [
        CommonModule,
        CardModule,
        AvatarModule,
        BadgeModule,
        DotIconModule,
        MarkdownModule.forChild(),
        TooltipModule,
        DotSafeHtmlPipe,
        DotAvatarDirective,
        DotMessagePipe
    ],
    declarations: [DotAppsCardComponent],
    exports: [DotAppsCardComponent]
})
export class DotAppsCardModule {}
