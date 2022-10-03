import { NgModule } from '@angular/core';
import { DotContentEditorComponent } from '@portlets/dot-containers/dot-container-create/dot-content-editor/dot-content-editor.component';
import { CommonModule } from '@angular/common';
import { TabViewModule } from 'primeng/tabview';
import { MenuModule } from 'primeng/menu';
import { DotTextareaContentModule } from '@components/_common/dot-textarea-content/dot-textarea-content.module';
import { DotMessagePipeModule } from '@dotcms/app/view/pipes/dot-message/dot-message-pipe.module';

@NgModule({
    declarations: [DotContentEditorComponent],
    imports: [
        CommonModule,
        TabViewModule,
        MenuModule,
        DotTextareaContentModule,
        DotMessagePipeModule
    ],
    exports: [DotContentEditorComponent]
})
export class DotContentEditorModule {}
