import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { SidebarModule } from 'primeng/sidebar';

import { BlockEditorModule } from '@dotcms/block-editor';
import { DotMessagePipe } from '@dotcms/ui';
import { DotBlockEditorSidebarComponent } from '@portlets/dot-edit-page/components/dot-block-editor-sidebar/dot-block-editor-sidebar.component';

@NgModule({
    declarations: [DotBlockEditorSidebarComponent],
    exports: [DotBlockEditorSidebarComponent],
    imports: [
        FormsModule,
        BlockEditorModule,
        CommonModule,
        SidebarModule,
        DotMessagePipe,
        ButtonModule
    ]
})
export class DotBlockEditorSidebarModule {}
