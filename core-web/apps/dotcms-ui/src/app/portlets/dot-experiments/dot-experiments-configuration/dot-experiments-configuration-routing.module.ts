import { RouterModule, Routes } from '@angular/router';
import { NgModule } from '@angular/core';

import { DotExperimentsConfigurationComponent } from './dot-experiments-configuration.component';

const routes: Routes = [
    {
        path: ':pageId/:experimentId',
        component: DotExperimentsConfigurationComponent
    }
];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule],
    providers: []
})
export class DotExperimentsConfigurationRoutingModule {}
