import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotEditLayoutComponent } from './dot-edit-layout.component';
import { DotEditLayoutGridModule } from '../dot-edit-layout-grid/dot-edit-layout-grid.module';
import { DotEditLayoutService } from '../../shared/services/dot-edit-layout.service';
import { CheckboxModule, ButtonModule, InputTextModule } from 'primeng/primeng';
import { DotActionButtonModule } from '../../../../view/components/_common/dot-action-button/dot-action-button.module';
import { DotTemplateAdditionalActionsModule } from '../dot-template-additional-actions/dot-template-additional-actions.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { DotLayoutPropertiesModule } from '../dot-layout-properties/dot-layout-properties.module';
import { Routes, RouterModule } from '@angular/router';
import { PageViewResolver } from '../../dot-edit-page-resolver.service';

const routes: Routes = [
    {
        component: DotEditLayoutComponent,
        path: '',
        resolve: {
            pageView: PageViewResolver
        }
    },
];

@NgModule({
    declarations: [DotEditLayoutComponent],
    imports: [
        RouterModule.forChild(routes),
        ButtonModule,
        CheckboxModule,
        CommonModule,
        DotActionButtonModule,
        DotEditLayoutGridModule,
        DotTemplateAdditionalActionsModule,
        FormsModule,
        ReactiveFormsModule,
        InputTextModule,
        DotLayoutPropertiesModule
    ],
    exports: [DotEditLayoutComponent],
    providers: [DotEditLayoutService]
})
export class DotEditLayoutModule {}
