import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { DotAppsConfigurationItemComponent } from './dot-apps-configuration-item.component';
import { DotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { DotIconModule } from '@components/_common/dot-icon/dot-icon.module';
import { TooltipModule } from 'primeng/tooltip';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { DotCopyButtonModule } from '@dotcms/app/view/components/dot-copy-button/dot-copy-button.module';

@NgModule({
    imports: [
        CommonModule,
        DotIconButtonModule,
        DotCopyButtonModule,
        DotIconModule,
        TooltipModule,
        DotPipesModule
    ],
    declarations: [DotAppsConfigurationItemComponent],
    exports: [DotAppsConfigurationItemComponent]
})
export class DotAppsConfigurationItemModule {}
