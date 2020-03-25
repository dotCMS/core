import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/primeng';

import { DotAppsConfigurationListComponent } from './dot-apps-configuration-list.component';
import { DotAppsConfigurationItemModule } from './dot-apps-configuration-item/dot-apps-configuration-item.module';

@NgModule({
    imports: [
        ButtonModule,
        CommonModule,
        DotAppsConfigurationItemModule
    ],
    declarations: [DotAppsConfigurationListComponent],
    exports: [DotAppsConfigurationListComponent]
})
export class DotAppsConfigurationListModule {}
