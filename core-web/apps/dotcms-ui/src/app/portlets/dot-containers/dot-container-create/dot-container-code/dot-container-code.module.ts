import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotContentEditorComponent } from './dot-container-code.component';
import { TabViewModule } from 'primeng/tabview';
import { MenuModule } from 'primeng/menu';
import { DotTextareaContentModule } from '@components/_common/dot-textarea-content/dot-textarea-content.module';
import { DotMessagePipeModule } from '@dotcms/app/view/pipes/dot-message/dot-message-pipe.module';
import { ReactiveFormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { DialogService, DynamicDialogModule } from 'primeng/dynamicdialog';
import { DotAddVariableModule } from './dot-add-variable/dot-add-variable.module';
import { DotIconModule } from '@dotcms/ui';

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
        DotIconModule
    ],
    exports: [DotContentEditorComponent],
    providers: [DialogService]
})
export class DotContentEditorModule {}
