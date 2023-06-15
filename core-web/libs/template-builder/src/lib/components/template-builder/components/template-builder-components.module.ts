import { NgModule } from '@angular/core';

import { AddWidgetComponent } from './add-widget/add-widget.component';
import { DotLayoutPropertiesModule } from './dot-layout-properties/dot-layout-properties.module';
import { RemoveConfirmDialogComponent } from './remove-confirm-dialog/remove-confirm-dialog.component';
import { TemplateBuilderActionsComponent } from './template-builder-actions/template-builder-actions.component';
import { TemplateBuilderBackgroundColumnsComponent } from './template-builder-background-columns/template-builder-background-columns.component';
import { TemplateBuilderBoxComponent } from './template-builder-box/template-builder-box.component';
import { TemplateBuilderRowComponent } from './template-builder-row/template-builder-row.component';
import { TemplateBuilderSectionComponent } from './template-builder-section/template-builder-section.component';

@NgModule({
    imports: [
        AddWidgetComponent,
        RemoveConfirmDialogComponent,
        TemplateBuilderActionsComponent,
        TemplateBuilderBackgroundColumnsComponent,
        TemplateBuilderBoxComponent,
        TemplateBuilderRowComponent,
        TemplateBuilderSectionComponent,
        DotLayoutPropertiesModule
    ],
    exports: [
        AddWidgetComponent,
        RemoveConfirmDialogComponent,
        TemplateBuilderActionsComponent,
        TemplateBuilderBackgroundColumnsComponent,
        TemplateBuilderBoxComponent,
        TemplateBuilderRowComponent,
        TemplateBuilderSectionComponent
    ]
})
export class TemplateBuilderComponentsModule {}
