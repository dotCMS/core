import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { DotEditLayoutDesignerModule } from '@components/dot-edit-layout-designer/dot-edit-layout-designer.module';
import { DotShowHideFeatureDirective } from '@dotcms/app/shared/directives/dot-show-hide-feature/dot-show-hide-feature.directive';
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
        DotEditLayoutDesignerModule,
        DotShowHideFeatureDirective,
        TemplateBuilderModule
    ],
    exports: [DotEditLayoutComponent]
})
export class DotEditLayoutModule {}
