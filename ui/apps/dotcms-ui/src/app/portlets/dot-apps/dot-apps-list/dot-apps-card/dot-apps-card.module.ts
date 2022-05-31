import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { DotAppsCardComponent } from './dot-apps-card.component';
import { DotAvatarModule } from '@components/_common/dot-avatar/dot-avatar.module';
import { DotIconModule } from '@dotcms/ui';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { CardModule } from 'primeng/card';
import { TooltipModule } from 'primeng/tooltip';
import { MarkdownModule } from 'ngx-markdown';

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
