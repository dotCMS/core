import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotPipesModule } from '@pipes/dot-pipes.module';

import { DotAppsConfigurationItemModule } from './dot-apps-configuration-item/dot-apps-configuration-item.module';
import { DotAppsConfigurationListComponent } from './dot-apps-configuration-list.component';

@NgModule({
    imports: [ButtonModule, CommonModule, DotAppsConfigurationItemModule, DotPipesModule],
    declarations: [DotAppsConfigurationListComponent],
    exports: [DotAppsConfigurationListComponent]
})
export class DotAppsConfigurationListModule {}
