import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgxTiptapModule } from '@dotcms/block-editor';
import { CommonModule } from '@angular/common';
import { DotEditBlockEditorComponent } from '@portlets/dot-edit-page/components/dot-edit-block-editor/dot-edit-block-editor.component';
import { SidebarModule } from 'primeng/sidebar';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { ButtonModule } from 'primeng/button';

@NgModule({
    declarations: [DotEditBlockEditorComponent],
    exports: [DotEditBlockEditorComponent],
    imports: [
        FormsModule,
        NgxTiptapModule,
        CommonModule,
        SidebarModule,
        DotMessagePipeModule,
        ButtonModule
    ]
})
export class DotEditBlockEditorModule {}
