import { AsyncPipe, NgFor, NgStyle } from '@angular/common';
import { NgModule } from '@angular/core';

import { DialogService, DynamicDialogModule, DynamicDialogRef } from 'primeng/dynamicdialog';
import { DividerModule } from 'primeng/divider';
import { ToolbarModule } from 'primeng/toolbar';

import { DotMessagePipeModule } from '@dotcms/ui';

import { AddStyleClassesDialogComponent } from './components/template-builder/components/add-style-classes-dialog/add-style-classes-dialog.component';
import { AddWidgetComponent } from './components/template-builder/components/add-widget/add-widget.component';
import { RemoveConfirmDialogComponent } from './components/template-builder/components/remove-confirm-dialog/remove-confirm-dialog.component';
import { TemplateBuilderActionsComponent } from './components/template-builder/components/template-builder-actions/template-builder-actions.component';
import { TemplateBuilderBackgroundColumnsComponent } from './components/template-builder/components/template-builder-background-columns/template-builder-background-columns.component';
import { TemplateBuilderBoxComponent } from './components/template-builder/components/template-builder-box/template-builder-box.component';
import { TemplateBuilderRowComponent } from './components/template-builder/components/template-builder-row/template-builder-row.component';
import { TemplateBuilderSectionComponent } from './components/template-builder/components/template-builder-section/template-builder-section.component';
import { DotTemplateBuilderStore } from './components/template-builder/store/template-builder.store';
import { TemplateBuilderComponent } from './components/template-builder/template-builder.component';

@NgModule({
    imports: [
        NgFor,
        AsyncPipe,
        TemplateBuilderRowComponent,
        AddWidgetComponent,
        TemplateBuilderBoxComponent,
        RemoveConfirmDialogComponent,
        DotMessagePipeModule,
        TemplateBuilderBackgroundColumnsComponent,
        TemplateBuilderSectionComponent,
        AddStyleClassesDialogComponent,
        DynamicDialogModule,
        TemplateBuilderActionsComponent,
        NgStyle,
        ToolbarModule,
        DividerModule
    ],
    declarations: [TemplateBuilderComponent],
    providers: [DotTemplateBuilderStore, DialogService, DynamicDialogRef],
    exports: [TemplateBuilderComponent]
})
export class TemplateBuilderModule {}
