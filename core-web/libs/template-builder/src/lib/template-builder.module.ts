import { AsyncPipe, NgFor, NgStyle } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotMessagePipeModule } from '@dotcms/ui';

import { AddWidgetComponent } from './components/template-builder/components/add-widget/add-widget.component';
import { RemoveConfirmDialogComponent } from './components/template-builder/components/remove-confirm-dialog/remove-confirm-dialog.component';
import { TemplateBuilderBackgroundColumnsComponent } from './components/template-builder/components/template-builder-background-columns/template-builder-background-columns.component';
import { TemplateBuilderBoxComponent } from './components/template-builder/components/template-builder-box/template-builder-box.component';
import { TemplateBuilderRowComponent } from './components/template-builder/components/template-builder-row/template-builder-row.component';
import { DotTemplateBuilderStore } from './components/template-builder/store/template-builder.store';
import { TemplateBuilderComponent } from './components/template-builder/template-builder.component';
import { TemplateBuilderSectionComponent } from './components/template-builder/components/template-builder-section/template-builder-section.component';

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
        NgStyle
    ],
    declarations: [TemplateBuilderComponent],
    providers: [DotTemplateBuilderStore],
    exports: [TemplateBuilderComponent]
})
export class TemplateBuilderModule {}
