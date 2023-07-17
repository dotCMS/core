import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { ToolbarModule } from 'primeng/toolbar';
import { TooltipModule } from 'primeng/tooltip';

import { DotActionButtonModule } from '@components/_common/dot-action-button/dot-action-button.module';
import { DotGlobalMessageModule } from '@components/_common/dot-global-message/dot-global-message.module';
import { DotContainerSelectorModule } from '@components/dot-container-selector/dot-container-selector.module';
import { DotSecondaryToolbarModule } from '@components/dot-secondary-toolbar';
import { DotPageLayoutService } from '@dotcms/data-access';
import { TemplateBuilderModule } from '@dotcms/template-builder';
import { DotMessagePipe, UiDotIconButtonModule } from '@dotcms/ui';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { DotEditPageInfoModule } from '@portlets/dot-edit-page/components/dot-edit-page-info/dot-edit-page-info.module';

import { DotLayoutDesignerModule } from './components/dot-layout-designer/dot-layout-designer.module';
import { DotSidebarPropertiesModule } from './components/dot-sidebar-properties/dot-sidebar-properties.module';
import { DotThemeSelectorModule } from './components/dot-theme-selector/dot-theme-selector.module';
import { DotEditLayoutDesignerComponent } from './dot-edit-layout-designer.component';

import { DotDialogModule } from '../dot-dialog/dot-dialog.module';

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
        UiDotIconButtonModule,
        DotSidebarPropertiesModule,
        DotThemeSelectorModule,
        FormsModule,
        InputTextModule,
        ReactiveFormsModule,
        ToolbarModule,
        TooltipModule,
        DotSecondaryToolbarModule,
        DotPipesModule,
        DotLayoutDesignerModule,
        DotDialogModule,
        TemplateBuilderModule,
        DotMessagePipe
    ],
    exports: [DotEditLayoutDesignerComponent],
    providers: [DotPageLayoutService]
})
export class DotEditLayoutDesignerModule {}
