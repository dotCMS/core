import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { DotTemplateDesignerComponent } from './dot-template-designer.component';

const routes: Routes = [
    {
        path: '',
        component: DotTemplateDesignerComponent
    }
];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule]
})
export class DotTemplateDesignerRoutingModule {}
