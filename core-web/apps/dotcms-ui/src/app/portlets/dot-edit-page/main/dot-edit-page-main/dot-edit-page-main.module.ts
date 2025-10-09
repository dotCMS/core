import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { DialogModule } from 'primeng/dialog';
import { OverlayPanelModule } from 'primeng/overlaypanel';

import { DotRouterService } from '@dotcms/dotcms-js';

import { DotEditPageMainComponent } from './dot-edit-page-main.component';

import { DotCustomEventHandlerService } from '../../../../api/services/dot-custom-event-handler/dot-custom-event-handler.service';
import { DotEditContentletComponent } from '../../../../view/components/dot-contentlet-editor/components/dot-edit-contentlet/dot-edit-contentlet.component';
import { DotExperimentClassDirective } from '../../../shared/directives/dot-experiment-class.directive';
import { DotBlockEditorSidebarModule } from '../../components/dot-block-editor-sidebar/dot-block-editor-sidebar.module';
import { DotEditPageNavDirective } from '../dot-edit-page-nav/directives/dot-edit-page-nav.directive';
import { DotEditPageNavModule } from '../dot-edit-page-nav/dot-edit-page-nav.module';

@NgModule({
    imports: [
        CommonModule,
        RouterModule,
        DotEditContentletComponent,
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
