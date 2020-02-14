import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { DotServiceIntegrationConfigurationItemComponent } from './dot-service-integration-configuration-item.component';
import { DotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';

@NgModule({
    imports: [
        CommonModule,
        DotIconButtonModule,
    ],
    declarations: [DotServiceIntegrationConfigurationItemComponent],
    exports: [DotServiceIntegrationConfigurationItemComponent]
})
export class DotServiceIntegrationConfigurationItemModule {}
