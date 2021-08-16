import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { DotAppsConfigurationItemComponent } from './dot-apps-configuration-item.component';
import { UiDotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { DotIconModule } from '@dotcms/ui';
import { TooltipModule } from 'primeng/tooltip';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { DotCopyLinkModule } from '@dotcms/app/view/components/dot-copy-link/dot-copy-link.module';

@NgModule({
    imports: [
        CommonModule,
        UiDotIconButtonModule,
        DotCopyLinkModule,
        DotIconModule,
        TooltipModule,
        DotPipesModule
    ],
    declarations: [DotAppsConfigurationItemComponent],
    exports: [DotAppsConfigurationItemComponent]
})
export class DotAppsConfigurationItemModule {}
