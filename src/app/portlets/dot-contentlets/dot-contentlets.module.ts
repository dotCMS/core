import { DotContentletsComponent } from './dot-contentlets.component';
import { Routes, RouterModule } from '@angular/router';
import { NgModule } from '@angular/core';

const routes: Routes = [
    {
        component: DotContentletsComponent,
        path: ''
    }
];

@NgModule({
    declarations: [DotContentletsComponent],
    imports: [
        RouterModule.forChild(routes),
    ],
    exports: [],
    providers: []
})
export class DotContentletsModule {}
