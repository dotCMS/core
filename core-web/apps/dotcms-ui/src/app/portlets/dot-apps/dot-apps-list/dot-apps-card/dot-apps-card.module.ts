import { MarkdownModule } from 'ngx-markdown';

import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { CardModule } from 'primeng/card';
import { TooltipModule } from 'primeng/tooltip';

import { DotAvatarModule } from '@components/_common/dot-avatar/dot-avatar.module';
import { DotIconModule } from '@dotcms/ui';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { DotAppsCardComponent } from './dot-apps-card.component';


@NgModule({
    imports: [
        CommonModule,
        CardModule,
        DotAvatarModule,
        DotIconModule,
        MarkdownModule.forChild(),
        TooltipModule,
        DotPipesModule
    ],
    declarations: [DotAppsCardComponent],
    exports: [DotAppsCardComponent]
})
export class DotAppsCardModule {}
