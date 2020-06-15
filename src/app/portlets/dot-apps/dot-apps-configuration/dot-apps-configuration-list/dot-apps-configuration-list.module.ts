import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/primeng';

import { DotAppsConfigurationListComponent } from './dot-apps-configuration-list.component';
import { DotAppsConfigurationItemModule } from './dot-apps-configuration-item/dot-apps-configuration-item.module';
import { DotPipesModule } from '@pipes/dot-pipes.module';

@NgModule({
    imports: [
        ButtonModule,
        CommonModule,
        DotAppsConfigurationItemModule,
        DotPipesModule
    ],
    declarations: [DotAppsConfigurationListComponent],
    exports: [DotAppsConfigurationListComponent]
})
export class DotAppsConfigurationListModule {}
