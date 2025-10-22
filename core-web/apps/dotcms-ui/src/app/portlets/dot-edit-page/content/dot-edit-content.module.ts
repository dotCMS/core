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
import { DotFormSelectorComponent } from './components/dot-form-selector/dot-form-selector.component';
import { DotWhatsChangedComponent } from './components/dot-whats-changed/dot-whats-changed.component';
import { DotEditContentComponent } from './dot-edit-content.component';
import { DotContainerContentletService } from './services/dot-container-contentlet.service';
import { DotEditContentHtmlService } from './services/dot-edit-content-html/dot-edit-content-html.service';
import { DotDOMHtmlUtilService } from './services/html/dot-dom-html-util.service';
import { DotDragDropAPIHtmlService } from './services/html/dot-drag-drop-api-html.service';
import { DotEditContentToolbarHtmlService } from './services/html/dot-edit-content-toolbar-html.service';

import { DotCustomEventHandlerService } from '../../../api/services/dot-custom-event-handler/dot-custom-event-handler.service';
import { DotShowHideFeatureDirective } from '../../../shared/directives/dot-show-hide-feature/dot-show-hide-feature.directive';
import { DotDirectivesModule } from '../../../shared/dot-directives.module';
import { DotOverlayMaskComponent } from '../../../view/components/_common/dot-overlay-mask/dot-overlay-mask.component';
import { DotLoadingIndicatorComponent } from '../../../view/components/_common/iframe/dot-loading-indicator/dot-loading-indicator.component';
import { IframeOverlayService } from '../../../view/components/_common/iframe/service/iframe-overlay.service';
import { DotEditContentletComponent } from '../../../view/components/dot-contentlet-editor/components/dot-edit-contentlet/dot-edit-contentlet.component';
import { DotPaletteComponent } from '../components/dot-palette/dot-palette.component';
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
        DotEditContentletComponent,
        DotSafeHtmlPipe,
        DotDirectivesModule,
        DotWhatsChangedComponent,
        DotFormSelectorComponent,
        TooltipModule,
        DotEditContentletComponent,
        DotLoadingIndicatorComponent,
        DotEditPageToolbarModule,
        DotEditPageViewAsControllerModule,
        DotEditPageStateControllerModule,
        DotOverlayMaskComponent,
        DotPaletteComponent,
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
