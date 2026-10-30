import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { CanDeactivateGuardService } from '@dotcms/data-access';

import { DotTemplateNewComponent } from './dot-template-new.component';
import { DotTemplateGuard } from './guards/dot-template.guard';

const routes: Routes = [
    {
        path: '',
        component: DotTemplateNewComponent
    },
    {
        path: ':type',
        loadChildren: () =>
            import('../dot-template-create-edit.module').then((m) => m.DotTemplateCreateEditModule),
        canDeactivate: [CanDeactivateGuardService],
        canLoad: [DotTemplateGuard]
    }
];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule]
})
export class DotTemplateNewRoutingModule {}
