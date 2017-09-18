import { CommonModule } from '@angular/common';
import { DotcmsConfig, SiteService } from 'dotcms-js/dotcms-js';
import { FormsModule } from '@angular/forms';
import { NgModule } from '@angular/core';
import { SearchableDropDownModule } from '../searchable-dropdown/searchable-dropdown.module';
import { IframeComponent } from './iframe-component';
import { DotLoadingIndicatorComponent } from './dot-loading-indicator';
import { IFramePortletLegacyComponent } from './iframe-porlet-legacy';
import { IframeOverlayService } from './service/iframe-overlay-service';

@NgModule({
    declarations: [
        DotLoadingIndicatorComponent,
        IframeComponent,
        IFramePortletLegacyComponent
    ],
    exports: [
        DotLoadingIndicatorComponent,
        IframeComponent,
        IFramePortletLegacyComponent
    ],
    imports: [
        CommonModule,
        FormsModule,
        SearchableDropDownModule
    ],
    providers: [
        IframeOverlayService,
    ],
})

export class IFrameModule {}
