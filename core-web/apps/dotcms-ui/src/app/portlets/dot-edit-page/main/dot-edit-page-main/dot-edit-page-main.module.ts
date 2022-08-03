import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotEditPageMainComponent } from './dot-edit-page-main.component';
import { RouterModule } from '@angular/router';
import { DotEditPageNavModule } from '../dot-edit-page-nav/dot-edit-page-nav.module';
import { DotContentletEditorModule } from '@components/dot-contentlet-editor/dot-contentlet-editor.module';
import { DotRouterService } from '@dotcms/dotcms-js';
import { DotCustomEventHandlerService } from '@services/dot-custom-event-handler/dot-custom-event-handler.service';
import { SidebarModule } from 'primeng/sidebar';
import { DotEditBlockEditorModule } from '@portlets/dot-edit-page/components/dot-edit-block-editor/dot-edit-block-editor.module';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { ButtonModule } from 'primeng/button';

@NgModule({
    imports: [
        CommonModule,
        RouterModule,
        DotEditPageNavModule,
        DotContentletEditorModule,
        SidebarModule,
        DotEditBlockEditorModule,
        DotMessagePipeModule,
        ButtonModule
    ],
    providers: [DotRouterService, DotCustomEventHandlerService],
    declarations: [DotEditPageMainComponent]
})
export class DotEditPageMainModule {}
