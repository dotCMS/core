import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { NgModule } from '@angular/core';

import { ButtonModule, CheckboxModule, InputTextModule, DialogModule, ToolbarModule } from 'primeng/primeng';

import { DotActionButtonModule } from '../../../../view/components/_common/dot-action-button/dot-action-button.module';
import { DotEditLayoutDesignerComponent } from '../dot-edit-layout-designer/dot-edit-layout-designer.component';
import { DotEditLayoutGridModule } from '../components/dot-edit-layout-grid/dot-edit-layout-grid.module';
import { DotEditLayoutSidebarModule } from '../components/dot-edit-layout-sidebar/dot-edit-layout-sidebar.module';
import { DotEditLayoutService } from '../../shared/services/dot-edit-layout.service';
import { DotLayoutPropertiesModule } from '../components/dot-layout-properties/dot-layout-properties.module';
import { DotSidebarPropertiesModule } from '../components/dot-sidebar-properties/dot-sidebar-properties.module';
import { DotTemplateAdditionalActionsModule } from '../components/dot-template-additional-actions/dot-template-additional-actions.module';
import { TemplateContainersCacheService } from '../../template-containers-cache.service';
import { PageViewService } from '../../../../api/services/page-view/page-view.service';
import { DotLayoutDesignerComponent } from './components/dot-layout-designer/dot-layout-designer.component';
import { DotContainerSelectorModule } from '../../../../view/components/dot-container-selector/dot-container-selector.module';
import { DotEditPageInfoModule } from '../../components/dot-edit-page-info/dot-edit-page-info.module';
import { DotThemeSelectorModule } from '../components/dot-theme-selector/dot-theme-selector.module';

@NgModule({
    declarations: [DotEditLayoutDesignerComponent, DotLayoutDesignerComponent],
    imports: [
        ButtonModule,
        CheckboxModule,
        CommonModule,
        DotActionButtonModule,
        DotEditLayoutGridModule,
        DotEditLayoutSidebarModule,
        DotTemplateAdditionalActionsModule,
        FormsModule,
        ReactiveFormsModule,
        InputTextModule,
        DotLayoutPropertiesModule,
        DialogModule,
        DotSidebarPropertiesModule,
        DotContainerSelectorModule,
        DotEditPageInfoModule,
        DotThemeSelectorModule,
        ToolbarModule
    ],
    exports: [DotEditLayoutDesignerComponent],
    providers: [DotEditLayoutService, DotEditLayoutService, PageViewService, TemplateContainersCacheService]
})
export class DotEditLayoutDesignerModule {}
