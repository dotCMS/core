import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { DotAppsListComponent } from './dot-apps-list/dot-apps-list.component';
import { DotAppsListResolver } from './dot-apps-list/dot-apps-list-resolver.service';
import { DotAppsConfigurationComponent } from './dot-apps-configuration/dot-apps-configuration.component';
import { DotAppsConfigurationResolver } from './dot-apps-configuration/dot-apps-configuration-resolver.service';
import { DotAppsConfigurationDetailComponent } from '@portlets/dot-apps/dot-apps-configuration-detail/dot-apps-configuration-detail.component';
import { DotAppsConfigurationDetailResolver } from '@portlets/dot-apps/dot-apps-configuration-detail/dot-apps-configuration-detail-resolver.service';

const routes: Routes = [
    {
        component: DotAppsConfigurationDetailComponent,
        path: ':appKey/create/:id',
        resolve: {
            data: DotAppsConfigurationDetailResolver
        }
    },
    {
        component: DotAppsConfigurationDetailComponent,
        path: ':appKey/edit/:id',
        resolve: {
            data: DotAppsConfigurationDetailResolver
        }
    },
    {
        component: DotAppsConfigurationComponent,
        path: ':appKey',
        resolve: {
            data: DotAppsConfigurationResolver
        }
    },
    {
        path: '',
        component: DotAppsListComponent,
        resolve: {
            canAccessPortlet: DotAppsListResolver
        }
    }
];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule]
})
export class DotAppsRoutingModule {}
