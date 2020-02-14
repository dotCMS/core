import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { DotServiceIntegrationListComponent } from './dot-service-integration-list/dot-service-integration-list.component';
import { DotServiceIntegrationListResolver } from './dot-service-integration-list/dot-service-integration-list-resolver.service';
import { DotServiceIntegrationConfigurationComponent } from './dot-service-integration-configuration/dot-service-integration-configuration.component';
import { DotServiceIntegrationConfigurationResolver } from './dot-service-integration-configuration/dot-service-integration-configuration-resolver.service';

const routes: Routes = [
    {
        component: DotServiceIntegrationConfigurationComponent,
        path: ':serviceKey',
        resolve: {
            data: DotServiceIntegrationConfigurationResolver
        }
    },
    {
        path: '',
        component: DotServiceIntegrationListComponent,
        resolve: {
            integrationServices: DotServiceIntegrationListResolver
        }
    }
];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule]
})
export class DotServiceIntegrationRoutingModule {}
