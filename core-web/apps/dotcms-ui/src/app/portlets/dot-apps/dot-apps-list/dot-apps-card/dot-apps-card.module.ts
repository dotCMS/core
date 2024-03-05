import { MarkdownModule } from 'ngx-markdown';

import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { AvatarModule } from 'primeng/avatar';
import { BadgeModule } from 'primeng/badge';
import { CardModule } from 'primeng/card';
import { TooltipModule } from 'primeng/tooltip';

import { DotAvatarDirective, DotIconModule, DotMessagePipe } from '@dotcms/ui';

import { DotAppsCardComponent } from './dot-apps-card.component';

import { DotPipesModule } from '../../../../view/pipes/dot-pipes.module';

@NgModule({
    imports: [
        CommonModule,
        CardModule,
        AvatarModule,
        BadgeModule,
        DotIconModule,
        MarkdownModule.forChild(),
        TooltipModule,
        DotPipesModule,
        DotAvatarDirective,
        DotMessagePipe
    ],
    declarations: [DotAppsCardComponent],
    exports: [DotAppsCardComponent]
})
export class DotAppsCardModule {}
