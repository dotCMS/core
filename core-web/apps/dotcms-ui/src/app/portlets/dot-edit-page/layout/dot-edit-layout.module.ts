import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { DotGlobalMessageModule } from '@components/_common/dot-global-message/dot-global-message.module';
import { TemplateBuilderModule } from '@dotcms/template-builder';

import { DotEditLayoutComponent } from './dot-edit-layout/dot-edit-layout.component';

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
        DotGlobalMessageModule
    ],
    exports: [DotEditLayoutComponent]
})
export class DotEditLayoutModule {}
