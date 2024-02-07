import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { DialogModule } from 'primeng/dialog';
import { OverlayPanelModule } from 'primeng/overlaypanel';

import { DotContentletEditorModule } from '@components/dot-contentlet-editor/dot-contentlet-editor.module';
import { DotCustomEventHandlerService } from '@dotcms/app/api/services/dot-custom-event-handler/dot-custom-event-handler.service';
import { DotRouterService } from '@dotcms/dotcms-js';
import { DotBlockEditorSidebarModule } from '@portlets/dot-edit-page/components/dot-block-editor-sidebar/dot-block-editor-sidebar.module';
import { DotEditPageNavDirective } from '@portlets/dot-edit-page/main/dot-edit-page-nav/directives/dot-edit-page-nav.directive';
import { DotExperimentClassDirective } from '@portlets/shared/directives/dot-experiment-class.directive';

import { DotEditPageMainComponent } from './dot-edit-page-main.component';

import { DotEditPageNavModule } from '../dot-edit-page-nav/dot-edit-page-nav.module';

@NgModule({
    imports: [
        CommonModule,
        RouterModule,
        DotContentletEditorModule,
        DotBlockEditorSidebarModule,
        DotEditPageNavDirective,
        DotEditPageNavModule,
        DotExperimentClassDirective,
        OverlayPanelModule,
        DialogModule
    ],
    providers: [DotRouterService, DotCustomEventHandlerService],
    declarations: [DotEditPageMainComponent]
})
export class DotEditPageMainModule {}
