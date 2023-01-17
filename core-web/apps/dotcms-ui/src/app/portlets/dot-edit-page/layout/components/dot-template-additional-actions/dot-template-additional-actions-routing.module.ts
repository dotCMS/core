import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { DotLegacyTemplateAdditionalActionsComponent } from './dot-legacy-template-additional-actions-iframe/dot-legacy-template-additional-actions-iframe.component';

const routes: Routes = [
    {
        path: '',
        component: DotLegacyTemplateAdditionalActionsComponent
    }
];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule]
})
export class DotTemplateAdditionalActionsRoutingModule {}
