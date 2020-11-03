import { DotIconButtonModule } from './../../../../view/components/_common/dot-icon-button/dot-icon-button.module';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { NgModule } from '@angular/core';

import { DotActionButtonModule } from '@components/_common/dot-action-button/dot-action-button.module';
import { DotEditLayoutDesignerComponent } from '../dot-edit-layout-designer/dot-edit-layout-designer.component';
import { DotEditLayoutService } from 'src/app/portlets/dot-edit-page/shared/services/dot-edit-layout.service';
import { DotLayoutPropertiesModule } from '../components/dot-layout-properties/dot-layout-properties.module';
import { DotSidebarPropertiesModule } from '../components/dot-sidebar-properties/dot-sidebar-properties.module';
import { DotTemplateAdditionalActionsModule } from '../components/dot-template-additional-actions/dot-template-additional-actions.module';
import { TemplateContainersCacheService } from '../../template-containers-cache.service';
import { DotPageLayoutService } from '@services/dot-page-layout/dot-page-layout.service';
import { DotContainerSelectorModule } from '@components/dot-container-selector/dot-container-selector.module';
import { DotEditPageInfoModule } from '../../components/dot-edit-page-info/dot-edit-page-info.module';
import { DotThemeSelectorModule } from '../components/dot-theme-selector/dot-theme-selector.module';
import { DotGlobalMessageModule } from '@components/_common/dot-global-message/dot-global-message.module';
import { DotEditPageViewAsControllerModule } from '@portlets/dot-edit-page/content/components/dot-edit-page-view-as-controller/dot-edit-page-view-as-controller.module';
import { DotSecondaryToolbarModule } from '@components/dot-secondary-toolbar';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { ToolbarModule } from 'primeng/toolbar';
import { TooltipModule } from 'primeng/tooltip';
import { DotLayoutDesignerModule } from './components/dot-layout-designer/dot-layout-designer.module';

@NgModule({
    declarations: [DotEditLayoutDesignerComponent],
    imports: [
        ButtonModule,
        CheckboxModule,
        CommonModule,
        DialogModule,
        DotActionButtonModule,
        DotContainerSelectorModule,
        DotEditPageInfoModule,
        DotGlobalMessageModule,
        DotIconButtonModule,
        DotLayoutPropertiesModule,
        DotSidebarPropertiesModule,
        DotTemplateAdditionalActionsModule,
        DotThemeSelectorModule,
        FormsModule,
        InputTextModule,
        ReactiveFormsModule,
        ToolbarModule,
        TooltipModule,
        DotSecondaryToolbarModule,
        DotEditPageViewAsControllerModule,
        DotPipesModule,
        DotLayoutDesignerModule
    ],
    exports: [DotEditLayoutDesignerComponent],
    providers: [DotEditLayoutService, DotPageLayoutService, TemplateContainersCacheService]
})
export class DotEditLayoutDesignerModule {}
