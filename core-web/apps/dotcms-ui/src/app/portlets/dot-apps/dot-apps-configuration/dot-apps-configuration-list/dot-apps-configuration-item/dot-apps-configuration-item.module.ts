import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

import { DotCopyLinkModule } from '@dotcms/app/view/components/dot-copy-link/dot-copy-link.module';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { DotAppsConfigurationItemComponent } from './dot-apps-configuration-item.component';

@NgModule({
    imports: [CommonModule, DotCopyLinkModule, TooltipModule, DotPipesModule, ButtonModule],
    declarations: [DotAppsConfigurationItemComponent],
    exports: [DotAppsConfigurationItemComponent]
})
export class DotAppsConfigurationItemModule {}
