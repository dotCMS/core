import { NgModule } from '@angular/core';
import { DotEditLayoutComponent } from './layout/dot-edit-layout/dot-edit-layout.component';
import { RouterModule, Routes } from '@angular/router';

const dotEditPage: Routes = [
    {
        component: DotEditLayoutComponent,
        path: ''
    },
    {
        component: DotEditLayoutComponent,
        path: 'layout'
    }
];

@NgModule({
    exports: [RouterModule],
    imports: [RouterModule.forChild(dotEditPage)]
})
export class DotEditPageRoutingModule {}
