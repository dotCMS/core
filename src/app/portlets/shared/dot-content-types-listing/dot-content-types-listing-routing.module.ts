import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { DotContentTypesPortletComponent } from '.';

const routes: Routes = [
    {
        component: DotContentTypesPortletComponent,
        path: ''
    }
];

@NgModule({
    exports: [RouterModule],
    imports: [RouterModule.forChild(routes)]
})
export class DotContentTypesListingRoutingModule {}
