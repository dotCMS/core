import { AsyncPipe, NgFor } from '@angular/common';
import { NgModule } from '@angular/core';
import { provideAnimations } from '@angular/platform-browser/animations';

import { AddWidgetComponent } from './components/template-builder/components/add-widget/add-widget.component';
import { RemoveRowComponent } from './components/template-builder/components/remove-row/remove-row.component';
import { TemplateBuilderBoxComponent } from './components/template-builder/components/template-builder-box/template-builder-box.component';
import { TemplateBuilderRowComponent } from './components/template-builder/components/template-builder-row/template-builder-row.component';
import { DotTemplateBuilderStore } from './components/template-builder/store/template-builder.store';
import { TemplateBuilderComponent } from './components/template-builder/template-builder.component';

@NgModule({
    imports: [
        NgFor,
        AsyncPipe,
        RemoveRowComponent,
        TemplateBuilderRowComponent,
        AddWidgetComponent,
        TemplateBuilderBoxComponent
    ],
    declarations: [TemplateBuilderComponent],
    providers: [DotTemplateBuilderStore, provideAnimations()],
    exports: [TemplateBuilderComponent]
})
export class TemplateBuilderModule {}
