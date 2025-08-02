import { NgModule } from '@angular/core';

import { DotAppsConfigurationModule } from './dot-apps-configuration/dot-apps-configuration.module';
import { DotAppsConfigurationDetailModule } from './dot-apps-configuration-detail/dot-apps-configuration-detail.module';
import { DotAppsListModule } from './dot-apps-list/dot-apps-list.module';
import { DotAppsRoutingModule } from './dot-apps-routing.module';

@NgModule({
    imports: [
        DotAppsListModule,
        DotAppsConfigurationModule,
        DotAppsConfigurationDetailModule,
        DotAppsRoutingModule
    ]
})
export class DotAppsModule {}
