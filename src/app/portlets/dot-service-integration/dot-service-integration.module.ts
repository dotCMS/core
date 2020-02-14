import { NgModule } from '@angular/core';

import { DotServiceIntegrationRoutingModule } from './dot-service-integration-routing.module';
import { DotServiceIntegrationListModule } from './dot-service-integration-list/dot-service-integration-list.module';
import { DotServiceIntegrationConfigurationModule } from './dot-service-integration-configuration/dot-service-integration-configuration.module';

@NgModule({
    imports: [
        DotServiceIntegrationListModule,
        DotServiceIntegrationConfigurationModule,
        DotServiceIntegrationRoutingModule
    ]
})
export class DotServiceIntegrationModule {}
