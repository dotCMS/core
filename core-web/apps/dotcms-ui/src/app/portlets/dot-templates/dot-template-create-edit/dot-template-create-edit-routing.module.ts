import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { CanDeactivateGuardService } from '@dotcms/data-access';

import { DotTemplateCreateEditComponent } from './dot-template-create-edit.component';

const routes: Routes = [
    {
        path: '',
        component: DotTemplateCreateEditComponent,
        canDeactivate: [CanDeactivateGuardService]
    }
];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule]
})
export class DotTemplateCreateEditRoutingModule {}
