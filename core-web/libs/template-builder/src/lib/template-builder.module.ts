import { AsyncPipe, NgClass, NgFor, NgIf, NgStyle } from '@angular/common';
import { NgModule } from '@angular/core';

import { DividerModule } from 'primeng/divider';
import { DialogService, DynamicDialogModule, DynamicDialogRef } from 'primeng/dynamicdialog';
import { ToolbarModule } from 'primeng/toolbar';

import { DotContainersService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { DotAddStyleClassesDialogStore } from './components/template-builder/components/add-style-classes-dialog/store/add-style-classes-dialog.store';
import { DotLayoutPropertiesComponent } from './components/template-builder/components/dot-layout-properties/dot-layout-properties.component';
import { TemplateBuilderComponentsModule } from './components/template-builder/components/template-builder-components.module';
import { DotTemplateBuilderStore } from './components/template-builder/store/template-builder.store';
import { TemplateBuilderComponent } from './components/template-builder/template-builder.component';

@NgModule({
    imports: [
        NgIf,
        NgFor,
        AsyncPipe,
        DotMessagePipe,
        DynamicDialogModule,
        NgStyle,
        NgClass,
        ToolbarModule,
        DividerModule,
        TemplateBuilderComponentsModule
    ],
    declarations: [TemplateBuilderComponent],
    providers: [
        DotTemplateBuilderStore,
        DialogService,
        DynamicDialogRef,
        DotAddStyleClassesDialogStore,
        DotContainersService
    ],
    exports: [TemplateBuilderComponent, DotLayoutPropertiesComponent]
})
export class TemplateBuilderModule {}
