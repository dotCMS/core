import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotEditPageMainComponent } from './dot-edit-page-main.component';
import { RouterModule } from '@angular/router';
import { DotEditPageNavModule } from '../dot-edit-page-nav/dot-edit-page-nav.module';
import { DotContentletEditorModule } from '@components/dot-contentlet-editor/dot-contentlet-editor.module';
import { DotRouterService } from '@dotcms/dotcms-js';
import { DotCustomEventHandlerService } from '@services/dot-custom-event-handler/dot-custom-event-handler.service';
import { DotEditBlockEditorModule } from '@portlets/dot-edit-page/components/dot-edit-block-editor/dot-edit-block-editor.module';

@NgModule({
    imports: [
        CommonModule,
        RouterModule,
        DotEditPageNavModule,
        DotContentletEditorModule,
        DotEditBlockEditorModule
    ],
    providers: [DotRouterService, DotCustomEventHandlerService],
    declarations: [DotEditPageMainComponent]
})
export class DotEditPageMainModule {}
