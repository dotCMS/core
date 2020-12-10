import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { DotTemplateGuard } from '@portlets/dot-templates/dot-template-create-edit/dot-template-new/guards/dot-template.guard';
import { DotTemplateNewComponent } from './dot-template-new.component';

const routes: Routes = [
    {
        path: '',
        component: DotTemplateNewComponent
    },
    {
        path: ':type',
        loadChildren: () =>
            import(
                '@portlets/dot-templates/dot-template-create-edit/dot-template-create-edit.module.ts'
            ).then((m) => m.DotTemplateCreateEditModule),
        canLoad: [DotTemplateGuard]
    }
];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule]
})
export class DotTemplateNewRoutingModule {}
