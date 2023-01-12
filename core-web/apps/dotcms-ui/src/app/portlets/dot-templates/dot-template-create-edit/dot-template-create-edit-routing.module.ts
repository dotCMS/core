import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { DotTemplateCreateEditComponent } from './dot-template-create-edit.component';

const routes: Routes = [
    {
        path: '',
        component: DotTemplateCreateEditComponent
    }
];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule]
})
export class DotTemplateCreateEditRoutingModule {}
