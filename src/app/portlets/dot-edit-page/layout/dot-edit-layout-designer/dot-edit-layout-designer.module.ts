import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { ButtonModule, CheckboxModule, InputTextModule, DialogModule, ConfirmationService } from 'primeng/primeng';

import { DotActionButtonModule } from '../../../../view/components/_common/dot-action-button/dot-action-button.module';
import { DotConfirmationService } from '../../../../api/services/dot-confirmation';
import { DotEditLayoutDesignerComponent } from '../dot-edit-layout-designer/dot-edit-layout-designer.component';
import { DotEditLayoutGridModule } from '../components/dot-edit-layout-grid/dot-edit-layout-grid.module';
import { DotEditLayoutService } from '../../shared/services/dot-edit-layout.service';
import { DotLayoutPropertiesModule } from '../components/dot-layout-properties/dot-layout-properties.module';
import { DotSidebarPropertiesModule } from '../components/dot-sidebar-properties/dot-sidebar-properties.module';
import { DotTemplateAdditionalActionsModule } from '../components/dot-template-additional-actions/dot-template-additional-actions.module';
import { TemplateContainersCacheService } from '../../template-containers-cache.service';
import { PageViewService } from '../../../../api/services/page-view/page-view.service';

@NgModule({
    declarations: [DotEditLayoutDesignerComponent],
    imports: [
        ButtonModule,
        CheckboxModule,
        CommonModule,
        DotActionButtonModule,
        DotEditLayoutGridModule,
        DotTemplateAdditionalActionsModule,
        FormsModule,
        ReactiveFormsModule,
        InputTextModule,
        DotLayoutPropertiesModule,
        DialogModule,
        DotSidebarPropertiesModule
    ],
    exports: [DotEditLayoutDesignerComponent],
    providers: [
        ConfirmationService,
        DotConfirmationService,
        DotConfirmationService,
        DotEditLayoutService,
        DotEditLayoutService,
        PageViewService,
        TemplateContainersCacheService
    ]
})
export class DotEditLayoutDesignerModule {}
