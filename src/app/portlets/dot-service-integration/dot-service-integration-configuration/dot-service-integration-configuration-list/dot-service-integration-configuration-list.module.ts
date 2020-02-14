import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/primeng';

import { DotServiceIntegrationConfigurationListComponent } from './dot-service-integration-configuration-list.component';
import { DotServiceIntegrationConfigurationItemModule } from './dot-service-integration-configuration-item/dot-service-integration-configuration-item.module';

@NgModule({
    imports: [
        ButtonModule,
        CommonModule,
        DotServiceIntegrationConfigurationItemModule
    ],
    declarations: [DotServiceIntegrationConfigurationListComponent],
    exports: [DotServiceIntegrationConfigurationListComponent]
})
export class DotServiceIntegrationConfigurationListModule {}
