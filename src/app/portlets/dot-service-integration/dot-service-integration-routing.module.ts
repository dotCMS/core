import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { DotServiceIntegrationListComponent } from './dot-service-integration-list/dot-service-integration-list.component';
import { DotServiceIntegrationListResolver } from './dot-service-integration-list/dot-service-integration-list-resolver.service';
import { DotServiceIntegrationConfigurationListComponent } from './dot-service-integration-configuration-list/dot-service-integration-configuration-list.component';
import { DotServiceIntegrationConfigurationListResolver } from './dot-service-integration-configuration-list/dot-service-integration-configuration-list-resolver.service';

const routes: Routes = [
    {
        component: DotServiceIntegrationConfigurationListComponent,
        path: ':serviceKey',
        resolve: {
            data: DotServiceIntegrationConfigurationListResolver
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
