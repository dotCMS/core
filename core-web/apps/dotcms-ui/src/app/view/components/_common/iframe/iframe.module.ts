import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

import { DotNotLicenseComponent, DotSafeHtmlPipe } from '@dotcms/ui';

import { DotLoadingIndicatorModule } from './dot-loading-indicator/dot-loading-indicator.module';
import { IframeComponent } from './iframe-component';
import { IframePortletLegacyComponent } from './iframe-porlet-legacy';
import { DotSafeUrlPipe } from './pipes/dot-safe-url/dot-safe-url.pipe';
import { DotIframePortletLegacyResolver } from './service/dot-iframe-porlet-legacy-resolver.service';
import { IframeOverlayService } from './service/iframe-overlay.service';

import { DotCustomEventHandlerService } from '../../../../api/services/dot-custom-event-handler/dot-custom-event-handler.service';
import { DotOverlayMaskModule } from '../dot-overlay-mask/dot-overlay-mask.module';
import { SearchableDropdownComponent } from '../searchable-dropdown/component/searchable-dropdown.component';

@NgModule({
    declarations: [IframeComponent, IframePortletLegacyComponent],
    exports: [DotLoadingIndicatorModule, IframeComponent, IframePortletLegacyComponent],
    imports: [
        CommonModule,
        FormsModule,
        SearchableDropdownComponent,
        DotLoadingIndicatorModule,
        RouterModule,
        DotOverlayMaskModule,
        DotNotLicenseComponent,
        DotSafeHtmlPipe,
        DotSafeUrlPipe
    ],
    providers: [IframeOverlayService, DotCustomEventHandlerService, DotIframePortletLegacyResolver]
})
export class IFrameModule {}
