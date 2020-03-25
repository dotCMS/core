import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { DotAppsListComponent } from './dot-apps-list/dot-apps-list.component';
import { DotAppsListResolver } from './dot-apps-list/dot-apps-list-resolver.service';
import { DotAppsConfigurationComponent } from './dot-apps-configuration/dot-apps-configuration.component';
import { DotAppsConfigurationResolver } from './dot-apps-configuration/dot-apps-configuration-resolver.service';

const routes: Routes = [
    {
        component: DotAppsConfigurationComponent,
        path: ':serviceKey',
        resolve: {
            data: DotAppsConfigurationResolver
        }
    },
    {
        path: '',
        component: DotAppsListComponent,
        resolve: {
            appsServices: DotAppsListResolver
        }
    }
];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule]
})
export class DotAppsRoutingModule {}
