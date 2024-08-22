import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { DotGlobalMessageModule } from '@components/_common/dot-global-message/dot-global-message.module';
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
        DotShowHideFeatureDirective,
        TemplateBuilderModule,
        DotGlobalMessageModule
    ],
    exports: [DotEditLayoutComponent]
})
export class DotEditLayoutModule {}
