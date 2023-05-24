import { AsyncPipe, NgFor } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotTemplateBuilderStore } from './components/template-builder/store/template-builder.store';
import { TemplateBuilderComponent } from './components/template-builder/template-builder.component';

@NgModule({
    imports: [NgFor, AsyncPipe],
    declarations: [TemplateBuilderComponent],
    providers: [DotTemplateBuilderStore],
    exports: [TemplateBuilderComponent]
})
export class TemplateBuilderModule {}
