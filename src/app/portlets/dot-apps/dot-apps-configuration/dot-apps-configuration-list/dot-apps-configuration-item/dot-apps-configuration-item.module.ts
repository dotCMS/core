import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { DotAppsConfigurationItemComponent } from './dot-apps-configuration-item.component';
import { DotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';

@NgModule({
    imports: [
        CommonModule,
        DotIconButtonModule,
    ],
    declarations: [DotAppsConfigurationItemComponent],
    exports: [DotAppsConfigurationItemComponent]
})
export class DotAppsConfigurationItemModule {}
