import { CommonModule } from '@angular/common';
import { DotcmsConfig, SiteService } from 'dotcms-js/dotcms-js';
import { FormsModule } from '@angular/forms';
import { NgModule } from '@angular/core';
import { SearchableDropDownModule } from '../searchable-dropdown/searchable-dropdown.module';
import { IframeComponent } from './iframe-component';
import { DotLoadingIndicatorModule } from './dot-loading-indicator/dot-loading-indicator.module';
import { IFramePortletLegacyComponent } from './iframe-porlet-legacy';
import { IframeOverlayService } from './service/iframe-overlay-service';

@NgModule({
    declarations: [
        IframeComponent,
        IFramePortletLegacyComponent
    ],
    exports: [
        DotLoadingIndicatorModule,
        IframeComponent,
        IFramePortletLegacyComponent
    ],
    imports: [
        CommonModule,
        FormsModule,
        SearchableDropDownModule,
        DotLoadingIndicatorModule
    ],
    providers: [
        IframeOverlayService,
    ],
})

export class IFrameModule {}
