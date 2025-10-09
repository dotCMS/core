import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { DialogModule } from 'primeng/dialog';
import { DialogService } from 'primeng/dynamicdialog';
import { TooltipModule } from 'primeng/tooltip';

import {
    DotEditPageService,
    DotESContentService,
    DotLicenseService,
    DotPageRenderService,
    DotSeoMetaTagsService,
    DotSeoMetaTagsUtilService,
    DotSessionStorageService,
    DotWorkflowActionsFireService,
    DotWorkflowService
} from '@dotcms/data-access';
import { DotResultsSeoToolComponent, DotSelectSeoToolComponent } from '@dotcms/portlets/dot-ema/ui';
import { DotCopyContentModalService, DotIconComponent, DotSafeHtmlPipe } from '@dotcms/ui';

import { DotEditPageStateControllerModule } from './components/dot-edit-page-state-controller/dot-edit-page-state-controller.module';
import { DotEditPageToolbarModule } from './components/dot-edit-page-toolbar/dot-edit-page-toolbar.module';
import { DotEditPageViewAsControllerModule } from './components/dot-edit-page-view-as-controller/dot-edit-page-view-as-controller.module';
import { DotFormSelectorModule } from './components/dot-form-selector/dot-form-selector.module';
import { DotWhatsChangedModule } from './components/dot-whats-changed/dot-whats-changed.module';
import { DotEditContentComponent } from './dot-edit-content.component';
import { DotContainerContentletService } from './services/dot-container-contentlet.service';
import { DotEditContentHtmlService } from './services/dot-edit-content-html/dot-edit-content-html.service';
import { DotDOMHtmlUtilService } from './services/html/dot-dom-html-util.service';
import { DotDragDropAPIHtmlService } from './services/html/dot-drag-drop-api-html.service';
import { DotEditContentToolbarHtmlService } from './services/html/dot-edit-content-toolbar-html.service';

import { DotCustomEventHandlerService } from '../../../api/services/dot-custom-event-handler/dot-custom-event-handler.service';
import { DotShowHideFeatureDirective } from '../../../shared/directives/dot-show-hide-feature/dot-show-hide-feature.directive';
import { DotDirectivesModule } from '../../../shared/dot-directives.module';
import { DotOverlayMaskModule } from '../../../view/components/_common/dot-overlay-mask/dot-overlay-mask.module';
import { DotLoadingIndicatorModule } from '../../../view/components/_common/iframe/dot-loading-indicator/dot-loading-indicator.module';
import { IframeOverlayService } from '../../../view/components/_common/iframe/service/iframe-overlay.service';
import { DotContentletEditorModule } from '../../../view/components/dot-contentlet-editor/dot-contentlet-editor.module';
import { DotPaletteModule } from '../components/dot-palette/dot-palette.module';
import { DotEditPageToolbarSeoComponent } from '../seo/components/dot-edit-page-toolbar-seo/dot-edit-page-toolbar-seo.component';
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
        DotSafeHtmlPipe,
        DotDirectivesModule,
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
        DotIconComponent,
        DotEditPageToolbarSeoComponent,
        DotShowHideFeatureDirective,
        DotResultsSeoToolComponent,
        DotSelectSeoToolComponent
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
        DotLicenseService,
        DialogService,
        DotCopyContentModalService,
        DotSeoMetaTagsService,
        DotSeoMetaTagsUtilService
    ]
})
export class DotEditContentModule {}
