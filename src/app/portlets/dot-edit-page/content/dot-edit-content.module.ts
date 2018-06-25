import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { DialogModule, TooltipModule } from 'primeng/primeng';

import { DotContainerContentletService } from './services/dot-container-contentlet.service';
import { DotDOMHtmlUtilService } from './services/html/dot-dom-html-util.service';
import { DotDirectivesModule } from '../../../shared/dot-directives.module';
import { DotDragDropAPIHtmlService } from './services/html/dot-drag-drop-api-html.service';
import { DotEditContentComponent } from './dot-edit-content.component';
import { DotEditContentHtmlService } from './services/dot-edit-content-html/dot-edit-content-html.service';
import { DotEditContentToolbarHtmlService } from './services/html/dot-edit-content-toolbar-html.service';
import { DotEditPageToolbarModule } from './components/dot-edit-page-toolbar/dot-edit-page-toolbar.module';
import { DotLoadingIndicatorModule } from '../../../view/components/_common/iframe/dot-loading-indicator/dot-loading-indicator.module';
import { DotRenderHTMLService } from '../../../api/services/dot-render-html/dot-render-html.service';
import { DotWorkflowService } from '../../../api/services/dot-workflow/dot-workflow.service';
import { DotEditPageService } from '../../../api/services/dot-edit-page/dot-edit-page.service';
import { DotEditContentViewAsToolbarModule } from './components/dot-edit-content-view-as-toolbar/dot-edit-content-view-as-toolbar.module';
import { DotWhatsChangedModule } from './components/dot-whats-changed/dot-whats-changed.module';

import { DotFormSelectorModule } from './components/dot-form-selector/dot-form-selector.module';
import { DotContentletEditorModule } from '../../../view/components/dot-contentlet-editor/dot-contentlet-editor.module';
import { DotEditPageInfoModule } from '../components/dot-edit-page-info/dot-edit-page-info.module';

const routes: Routes = [
    {
        component: DotEditContentComponent,
        path: ''
    }
];

@NgModule({
    declarations: [DotEditContentComponent],
    imports: [
        CommonModule,
        DialogModule,
        RouterModule.forChild(routes),
        DotContentletEditorModule,
        DotDirectivesModule,
        DotEditContentViewAsToolbarModule,
        DotWhatsChangedModule,
        DotFormSelectorModule,
        TooltipModule,
        DotContentletEditorModule,
        DotEditPageInfoModule,
        DotEditPageToolbarModule,
        DotLoadingIndicatorModule,
    ],
    exports: [DotEditContentComponent],
    providers: [
        DotContainerContentletService,
        DotDOMHtmlUtilService,
        DotDragDropAPIHtmlService,
        DotEditContentHtmlService,
        DotEditContentToolbarHtmlService,
        DotEditPageService,
        DotRenderHTMLService,
        DotWorkflowService
    ]
})
export class DotEditContentModule {}
