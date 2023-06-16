import { AsyncPipe, NgFor, NgStyle } from '@angular/common';
import { NgModule } from '@angular/core';

import { DividerModule } from 'primeng/divider';
import { ToolbarModule } from 'primeng/toolbar';

import { DotMessagePipeModule } from '@dotcms/ui';

import { TemplateBuilderComponentsModule } from './components/template-builder/components/template-builder-components.module';
import { DotTemplateBuilderStore } from './components/template-builder/store/template-builder.store';
import { TemplateBuilderComponent } from './components/template-builder/template-builder.component';

@NgModule({
    imports: [
        NgFor,
        AsyncPipe,
        DotMessagePipeModule,
        NgStyle,
        ToolbarModule,
        DividerModule,
        TemplateBuilderComponentsModule
    ],
    declarations: [TemplateBuilderComponent],
    providers: [DotTemplateBuilderStore],
    exports: [TemplateBuilderComponent]
})
export class TemplateBuilderModule {}
