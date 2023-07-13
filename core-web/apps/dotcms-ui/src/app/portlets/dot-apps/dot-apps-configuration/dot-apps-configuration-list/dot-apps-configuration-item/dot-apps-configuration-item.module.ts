import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { TooltipModule } from 'primeng/tooltip';

import { DotCopyLinkModule } from '@dotcms/app/view/components/dot-copy-link/dot-copy-link.module';
import { DotIconModule, DotMessagePipe, UiDotIconButtonModule } from '@dotcms/ui';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { DotAppsConfigurationItemComponent } from './dot-apps-configuration-item.component';

@NgModule({
    imports: [
        CommonModule,
        UiDotIconButtonModule,
        DotCopyLinkModule,
        DotIconModule,
        TooltipModule,
        DotPipesModule,
        DotMessagePipe
    ],
    declarations: [DotAppsConfigurationItemComponent],
    exports: [DotAppsConfigurationItemComponent]
})
export class DotAppsConfigurationItemModule {}
