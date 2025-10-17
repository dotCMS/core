import { MarkdownModule } from 'ngx-markdown';

import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { AvatarModule } from 'primeng/avatar';
import { BadgeModule } from 'primeng/badge';
import { CardModule } from 'primeng/card';
import { TooltipModule } from 'primeng/tooltip';

import { DotAvatarDirective, DotIconComponent, DotMessagePipe, DotSafeHtmlPipe } from '@dotcms/ui';

import { DotAppsCardComponent } from './dot-apps-card.component';

@NgModule({
    imports: [
        CommonModule,
        CardModule,
        AvatarModule,
        BadgeModule,
        DotIconComponent,
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
