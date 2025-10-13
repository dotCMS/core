import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { TemplateBuilderModule } from '@dotcms/template-builder';

import { DotEditLayoutComponent } from './dot-edit-layout/dot-edit-layout.component';

import { DotGlobalMessageComponent } from '../../../view/components/_common/dot-global-message/dot-global-message.component';

const routes: Routes = [
    {
        component: DotEditLayoutComponent,
        path: ''
    }
];

@NgModule({
    declarations: [DotEditLayoutComponent],
    imports: [
        CommonModule,
        RouterModule.forChild(routes),
        TemplateBuilderModule,
        DotGlobalMessageComponent
    ],
    exports: [DotEditLayoutComponent]
})
export class DotEditLayoutModule {}
