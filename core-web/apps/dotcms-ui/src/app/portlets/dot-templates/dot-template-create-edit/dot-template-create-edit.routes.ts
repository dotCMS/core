import { Routes } from '@angular/router';

import { CanDeactivateGuardService } from '@dotcms/data-access';

import { DotTemplateCreateEditComponent } from './dot-template-create-edit.component';

export const routes: Routes = [
    {
        path: '',
        component: DotTemplateCreateEditComponent,
        canDeactivate: [CanDeactivateGuardService]
    }
];
