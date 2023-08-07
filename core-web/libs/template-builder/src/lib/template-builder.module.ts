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
import { DotItemDragScrollDirective } from './components/template-builder/directives/dot-item-drag-scroll/dot-item-drag-scroll.directive';
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
        TemplateBuilderComponentsModule,
        DotItemDragScrollDirective
    ],
    declarations: [TemplateBuilderComponent],
    providers: [
        DialogService,
        DynamicDialogRef,
        DotAddStyleClassesDialogStore,
        DotContainersService
    ],
    exports: [TemplateBuilderComponent, DotLayoutPropertiesComponent]
})
export class TemplateBuilderModule {}
