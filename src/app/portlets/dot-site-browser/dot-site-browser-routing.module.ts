import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { DotSiteBrowserComponent } from './dot-site-browser.component';

const routes: Routes = [
    {
        path: '',
        component: DotSiteBrowserComponent
    }
];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule]
})
export class DotSiteBrowserRoutingModule {}
