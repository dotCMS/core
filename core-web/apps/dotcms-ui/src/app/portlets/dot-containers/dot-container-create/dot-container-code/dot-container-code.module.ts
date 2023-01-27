import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DialogService, DynamicDialogModule } from 'primeng/dynamicdialog';
import { MenuModule } from 'primeng/menu';
import { SkeletonModule } from 'primeng/skeleton';
import { TabViewModule } from 'primeng/tabview';

import { DotTextareaContentModule } from '@components/_common/dot-textarea-content/dot-textarea-content.module';
import { DotMessagePipeModule } from '@dotcms/app/view/pipes/dot-message/dot-message-pipe.module';
import { DotIconModule } from '@dotcms/ui';

import { DotAddVariableModule } from './dot-add-variable/dot-add-variable.module';
import { DotContentEditorComponent } from './dot-container-code.component';

@NgModule({
    declarations: [DotContentEditorComponent],
    imports: [
        CommonModule,
        TabViewModule,
        MenuModule,
        DotTextareaContentModule,
        DotMessagePipeModule,
        ReactiveFormsModule,
        ButtonModule,
        DynamicDialogModule,
        DotAddVariableModule,
        DotIconModule,
        SkeletonModule
    ],
    exports: [DotContentEditorComponent],
    providers: [DialogService]
})
export class DotContentEditorModule {}
