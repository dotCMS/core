import { AsyncPipe, NgFor, NgStyle } from '@angular/common';
import { NgModule } from '@angular/core';

import { DividerModule } from 'primeng/divider';
import { DialogService, DynamicDialogModule, DynamicDialogRef } from 'primeng/dynamicdialog';
import { ToolbarModule } from 'primeng/toolbar';

import { DotMessagePipeModule } from '@dotcms/ui';

import { AddStyleClassesDialogComponent } from './components/template-builder/components/add-style-classes-dialog/add-style-classes-dialog.component';
import { DotAddStyleClassesDialogStore } from './components/template-builder/components/add-style-classes-dialog/store/add-style-classes-dialog.store';
import { TemplateBuilderActionsComponent } from './components/template-builder/components/template-builder-actions/template-builder-actions.component';
import { TemplateBuilderBackgroundColumnsComponent } from './components/template-builder/components/template-builder-background-columns/template-builder-background-columns.component';
import { TemplateBuilderComponentsModule } from './components/template-builder/components/template-builder-components.module';
import { TemplateBuilderSectionComponent } from './components/template-builder/components/template-builder-section/template-builder-section.component';
import { DotTemplateBuilderStore } from './components/template-builder/store/template-builder.store';
import { TemplateBuilderComponent } from './components/template-builder/template-builder.component';

@NgModule({
    imports: [
        NgFor,
        AsyncPipe,
        DotMessagePipeModule,
        TemplateBuilderBackgroundColumnsComponent,
        TemplateBuilderSectionComponent,
        AddStyleClassesDialogComponent,
        DynamicDialogModule,
        TemplateBuilderActionsComponent,
        NgStyle,
        ToolbarModule,
        DividerModule,
        TemplateBuilderComponentsModule
    ],
    declarations: [TemplateBuilderComponent],
    providers: [
        DotTemplateBuilderStore,
        DialogService,
        DynamicDialogRef,
        DotAddStyleClassesDialogStore
    ],
    exports: [TemplateBuilderComponent]
})
export class TemplateBuilderModule {}
