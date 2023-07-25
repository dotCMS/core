import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { DotTemplateGuard } from '@portlets/dot-templates/dot-template-create-edit/dot-template-new/guards/dot-template.guard';
import { LayoutEditorCanDeactivateGuardService } from '@services/guards/layout-editor-can-deactivate-guard.service';

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
                '@portlets/dot-templates/dot-template-create-edit/dot-template-create-edit.module'
            ).then((m) => m.DotTemplateCreateEditModule),
        canDeactivate: [LayoutEditorCanDeactivateGuardService],
        canLoad: [DotTemplateGuard]
    }
];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule]
})
export class DotTemplateNewRoutingModule {}
