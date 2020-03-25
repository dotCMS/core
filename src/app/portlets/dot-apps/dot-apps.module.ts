import { NgModule } from '@angular/core';

import { DotAppsRoutingModule } from './dot-apps-routing.module';
import { DotAppsListModule } from './dot-apps-list/dot-apps-list.module';
import { DotAppsConfigurationModule } from './dot-apps-configuration/dot-apps-configuration.module';

@NgModule({
    imports: [
        DotAppsListModule,
        DotAppsConfigurationModule,
        DotAppsRoutingModule
    ]
})
export class DotAppsModule {}
