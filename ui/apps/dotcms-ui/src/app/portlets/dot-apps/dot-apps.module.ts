import { NgModule } from '@angular/core';

import { DotAppsRoutingModule } from './dot-apps-routing.module';
import { DotAppsListModule } from './dot-apps-list/dot-apps-list.module';
import { DotAppsConfigurationModule } from './dot-apps-configuration/dot-apps-configuration.module';
import { DotAppsConfigurationDetailModule } from '@portlets/dot-apps/dot-apps-configuration-detail/dot-apps-configuration-detail.module';

@NgModule({
    imports: [
        DotAppsListModule,
        DotAppsConfigurationModule,
        DotAppsConfigurationDetailModule,
        DotAppsRoutingModule
    ]
})
export class DotAppsModule {}
