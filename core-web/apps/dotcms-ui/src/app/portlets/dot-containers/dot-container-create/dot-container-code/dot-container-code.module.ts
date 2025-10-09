import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DialogService, DynamicDialogModule } from 'primeng/dynamicdialog';
import { MenuModule } from 'primeng/menu';
import { SkeletonModule } from 'primeng/skeleton';
import { TabViewModule } from 'primeng/tabview';

import { DotFieldRequiredDirective, DotIconComponent, DotMessagePipe } from '@dotcms/ui';

import { DotAddVariableModule } from './dot-add-variable/dot-add-variable.module';
import { DotContentEditorComponent } from './dot-container-code.component';

import { DotTextareaContentComponent } from '../../../../view/components/_common/dot-textarea-content/dot-textarea-content.component';

@NgModule({
    declarations: [DotContentEditorComponent],
    imports: [
        CommonModule,
        TabViewModule,
        MenuModule,
        DotTextareaContentComponent,
        DotMessagePipe,
        ReactiveFormsModule,
        ButtonModule,
        DynamicDialogModule,
        DotAddVariableModule,
        DotIconComponent,
        SkeletonModule,
        DotFieldRequiredDirective
    ],
    exports: [DotContentEditorComponent],
    providers: [DialogService]
})
export class DotContentEditorModule {}
