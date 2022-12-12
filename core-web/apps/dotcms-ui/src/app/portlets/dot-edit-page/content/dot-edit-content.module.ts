import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { DotContainerContentletService } from './services/dot-container-contentlet.service';
import { DotDOMHtmlUtilService } from './services/html/dot-dom-html-util.service';
import { DotDirectivesModule } from '@shared/dot-directives.module';
import { DotDragDropAPIHtmlService } from './services/html/dot-drag-drop-api-html.service';
import { DotEditContentComponent } from './dot-edit-content.component';
import { DotEditContentHtmlService } from './services/dot-edit-content-html/dot-edit-content-html.service';
import { DotEditContentToolbarHtmlService } from './services/html/dot-edit-content-toolbar-html.service';
import { DotLoadingIndicatorModule } from '@components/_common/iframe/dot-loading-indicator/dot-loading-indicator.module';
import { DotPageRenderService } from '@services/dot-page-render/dot-page-render.service';
import { DotWorkflowService } from '@services/dot-workflow/dot-workflow.service';
import { DotEditPageService } from '@services/dot-edit-page/dot-edit-page.service';
import { DotWhatsChangedModule } from './components/dot-whats-changed/dot-whats-changed.module';

import { DotFormSelectorModule } from './components/dot-form-selector/dot-form-selector.module';
import { DotContentletEditorModule } from '@components/dot-contentlet-editor/dot-contentlet-editor.module';
import { DotEditPageViewAsControllerModule } from './components/dot-edit-page-view-as-controller/dot-edit-page-view-as-controller.module';
import { DotEditPageStateControllerModule } from './components/dot-edit-page-state-controller/dot-edit-page-state-controller.module';
import { DotEditPageToolbarModule } from './components/dot-edit-page-toolbar/dot-edit-page-toolbar.module';
import { IframeOverlayService } from '@components/_common/iframe/service/iframe-overlay.service';
import { DotOverlayMaskModule } from '@components/_common/dot-overlay-mask/dot-overlay-mask.module';
import { DotCustomEventHandlerService } from '@services/dot-custom-event-handler/dot-custom-event-handler.service';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { CheckboxModule } from 'primeng/checkbox';
import { TooltipModule } from 'primeng/tooltip';
import { DotWorkflowActionsFireService } from '@services/dot-workflow-actions-fire/dot-workflow-actions-fire.service';
import { DotLicenseService } from '@services/dot-license/dot-license.service';
import { DotPaletteModule } from '@dotcms/app/portlets/dot-edit-page/components/dot-palette/dot-palette.module';
import { DotIconModule } from '@dotcms/ui';
import { DotESContentService } from '@dotcms/app/api/services/dot-es-content/dot-es-content.service';
import { DotSessionStorageService } from '@shared/services/dot-session-storage.service';

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
        ButtonModule,
        DialogModule,
        CheckboxModule,
        RouterModule.forChild(routes),
        DotContentletEditorModule,
        DotDirectivesModule,
        DotPipesModule,
        DotWhatsChangedModule,
        DotFormSelectorModule,
        TooltipModule,
        DotContentletEditorModule,
        DotLoadingIndicatorModule,
        DotEditPageToolbarModule,
        DotEditPageViewAsControllerModule,
        DotEditPageStateControllerModule,
        DotOverlayMaskModule,
        DotPaletteModule,
        DotIconModule
    ],
    exports: [DotEditContentComponent],
    providers: [
        DotContainerContentletService,
        DotDOMHtmlUtilService,
        DotDragDropAPIHtmlService,
        DotEditContentHtmlService,
        DotEditContentToolbarHtmlService,
        DotSessionStorageService,
        DotEditPageService,
        DotESContentService,
        DotPageRenderService,
        DotWorkflowService,
        IframeOverlayService,
        DotCustomEventHandlerService,
        DotWorkflowActionsFireService,
        DotLicenseService
    ]
})
export class DotEditContentModule {}
