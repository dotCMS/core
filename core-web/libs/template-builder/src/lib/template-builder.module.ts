import { AsyncPipe, NgFor } from '@angular/common';
import { NgModule } from '@angular/core';

import { AddWidgetComponent } from './components/template-builder/components/add-widget/add-widget.component';
import { TemplateBuilderRowComponent } from './components/template-builder/components/template-builder-row/template-builder-row.component';
import { DotTemplateBuilderStore } from './components/template-builder/store/template-builder.store';
import { TemplateBuilderComponent } from './components/template-builder/template-builder.component';

@NgModule({
    imports: [NgFor, AsyncPipe, TemplateBuilderRowComponent, AddWidgetComponent],
    declarations: [TemplateBuilderComponent],
    providers: [DotTemplateBuilderStore],
    exports: [TemplateBuilderComponent]
})
export class TemplateBuilderModule {}
