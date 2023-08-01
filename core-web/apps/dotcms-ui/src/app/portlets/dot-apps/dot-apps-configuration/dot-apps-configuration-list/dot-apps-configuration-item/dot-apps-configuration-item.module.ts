import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

import { DotCopyLinkModule } from '@dotcms/app/view/components/dot-copy-link/dot-copy-link.module';
import { DotMessagePipe } from '@dotcms/ui';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { DotAppsConfigurationItemComponent } from './dot-apps-configuration-item.component';

@NgModule({
    imports: [
        CommonModule,
        DotCopyLinkModule,
        TooltipModule,
        DotMessagePipe,
        ButtonModule,
        DotPipesModule
    ],
    declarations: [DotAppsConfigurationItemComponent],
    exports: [DotAppsConfigurationItemComponent]
})
export class DotAppsConfigurationItemModule {}
