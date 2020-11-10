import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { DotTemplateComponent } from './dot-template-advanced.component';

const routes: Routes = [
    {
        path: '',
        component: DotTemplateComponent
    }
];

@NgModule({ imports: [RouterModule.forChild(routes)] })
export class DotTemplateRoutingModule {}
