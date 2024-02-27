import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { DialogModule } from 'primeng/dialog';
import { DialogService } from 'primeng/dynamicdialog';
import { TooltipModule } from 'primeng/tooltip';

import { DotOverlayMaskModule } from '@components/_common/dot-overlay-mask/dot-overlay-mask.module';
import { DotLoadingIndicatorModule } from '@components/_common/iframe/dot-loading-indicator/dot-loading-indicator.module';
import { IframeOverlayService } from '@components/_common/iframe/service/iframe-overlay.service';
import { DotContentletEditorModule } from '@components/dot-contentlet-editor/dot-contentlet-editor.module';
import { DotCustomEventHandlerService } from '@dotcms/app/api/services/dot-custom-event-handler/dot-custom-event-handler.service';
import { DotPaletteModule } from '@dotcms/app/portlets/dot-edit-page/components/dot-palette/dot-palette.module';
import { DotShowHideFeatureDirective } from '@dotcms/app/shared/directives/dot-show-hide-feature/dot-show-hide-feature.directive';
import {
    DotEditPageService,
    DotESContentService,
    DotLicenseService,
    DotPageRenderService,
    DotSessionStorageService,
    DotWorkflowActionsFireService,
    DotWorkflowService
} from '@dotcms/data-access';
import { DotCopyContentModalService, DotIconModule } from '@dotcms/ui';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { DotDirectivesModule } from '@shared/dot-directives.module';

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
import { DotSeoMetaTagsUtilService } from './services/html/dot-seo-meta-tags-util.service';
import { DotSeoMetaTagsService } from './services/html/dot-seo-meta-tags.service';

import { DotEditPageToolbarSeoComponent } from '../seo/components/dot-edit-page-toolbar-seo/dot-edit-page-toolbar-seo.component';
import { DotResultsSeoToolComponent } from '../seo/components/dot-results-seo-tool/dot-results-seo-tool.component';
import { DotSelectSeoToolComponent } from '../seo/components/dot-select-seo-tool/dot-select-seo-tool.component';
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
        DotIconModule,
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
