import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { DotExperimentsListComponent } from './dot-experiments-list.component';

const routes: Routes = [{ path: ':pageId', component: DotExperimentsListComponent }];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule]
})
export class DotExperimentsListRoutingModule {}
