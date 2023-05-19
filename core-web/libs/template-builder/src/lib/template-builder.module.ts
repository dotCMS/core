import { AsyncPipe, NgFor } from '@angular/common';
import { NgModule } from '@angular/core';

import { TemplateBuilderComponent } from './components/template-builder/template-builder.component';

@NgModule({
    imports: [NgFor, AsyncPipe],
    declarations: [TemplateBuilderComponent],
    exports: [TemplateBuilderComponent]
})
export class TemplateBuilderModule {}
