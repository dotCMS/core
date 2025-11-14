import { Routes } from '@angular/router';

import { CanDeactivateGuardService } from '@dotcms/data-access';

import { DotTemplateNewComponent } from './dot-template-new.component';
import { DotTemplateGuard } from './guards/dot-template.guard';

export const DotTemplateNewRoutes: Routes = [
    {
        path: '',
        component: DotTemplateNewComponent,
        providers: [DotTemplateGuard, CanDeactivateGuardService]
    },
    {
        path: ':type',
        loadComponent: () =>
            import('../dot-template-create-edit.component').then(
                (m) => m.DotTemplateCreateEditComponent
            ),
        canDeactivate: [CanDeactivateGuardService],
        canLoad: [DotTemplateGuard]
    }
];
