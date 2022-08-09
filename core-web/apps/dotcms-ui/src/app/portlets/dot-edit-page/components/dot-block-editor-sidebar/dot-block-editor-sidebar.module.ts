import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgxTiptapModule } from '@dotcms/block-editor';
import { CommonModule } from '@angular/common';
import { SidebarModule } from 'primeng/sidebar';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { ButtonModule } from 'primeng/button';
import { DotBlockEditorSidebarComponent } from '@portlets/dot-edit-page/components/dot-block-editor-sidebar/dot-block-editor-sidebar.component';

@NgModule({
    declarations: [DotBlockEditorSidebarComponent],
    exports: [DotBlockEditorSidebarComponent],
    imports: [
        FormsModule,
        NgxTiptapModule,
        CommonModule,
        SidebarModule,
        DotMessagePipeModule,
        ButtonModule
    ]
})
export class DotBlockEditorSidebarModule {}
