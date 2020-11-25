import { DotIconButtonModule } from '../_common/dot-icon-button/dot-icon-button.module';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { ToolbarModule } from 'primeng/toolbar';
import { TooltipModule } from 'primeng/tooltip';

import { DotEditLayoutDesignerComponent } from './dot-edit-layout-designer.component';
import { DotLayoutDesignerModule } from './components/dot-layout-designer/dot-layout-designer.module';
import { DotLayoutPropertiesModule } from './components/dot-layout-properties/dot-layout-properties.module';
import { DotSidebarPropertiesModule } from './components/dot-sidebar-properties/dot-sidebar-properties.module';
import { DotThemeSelectorModule } from './components/dot-theme-selector/dot-theme-selector.module';

// @components
import { DotContainerSelectorModule } from '@components/dot-container-selector/dot-container-selector.module';
import { DotActionButtonModule } from '@components/_common/dot-action-button/dot-action-button.module';
import { DotGlobalMessageModule } from '@components/_common/dot-global-message/dot-global-message.module';
import { DotSecondaryToolbarModule } from '@components/dot-secondary-toolbar';

// @pipes
import { DotPipesModule } from '@pipes/dot-pipes.module';

// @services
import { DotPageLayoutService } from '@services/dot-page-layout/dot-page-layout.service';

// @portlets
import { DotTemplateContainersCacheService } from '@services/dot-template-containers-cache/dot-template-containers-cache.service';
import { DotEditPageInfoModule } from '@portlets/dot-edit-page/components/dot-edit-page-info/dot-edit-page-info.module';
import { DotEditLayoutService } from '@services/dot-edit-layout/dot-edit-layout.service';

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
        DotThemeSelectorModule,
        FormsModule,
        InputTextModule,
        ReactiveFormsModule,
        ToolbarModule,
        TooltipModule,
        DotSecondaryToolbarModule,
        DotPipesModule,
        DotLayoutDesignerModule
    ],
    exports: [DotEditLayoutDesignerComponent],
    providers: [DotEditLayoutService, DotPageLayoutService, DotTemplateContainersCacheService]
})
export class DotEditLayoutDesignerModule {}
