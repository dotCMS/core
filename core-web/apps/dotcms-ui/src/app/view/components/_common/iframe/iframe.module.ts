import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

import { DotOverlayMaskModule } from '@components/_common/dot-overlay-mask/dot-overlay-mask.module';
import { DotSafeUrlPipe } from '@components/_common/iframe/pipes/dot-safe-url/dot-safe-url.pipe';
import { NotLicensedModule } from '@components/not-licensed/not-licensed.module';
import { DotCustomEventHandlerService } from '@dotcms/app/api/services/dot-custom-event-handler/dot-custom-event-handler.service';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { DotLoadingIndicatorModule } from './dot-loading-indicator/dot-loading-indicator.module';
import { IframeComponent } from './iframe-component';
import { IframePortletLegacyComponent } from './iframe-porlet-legacy';
import { DotIframePortletLegacyResolver } from './service/dot-iframe-porlet-legacy-resolver.service';
import { IframeOverlayService } from './service/iframe-overlay.service';

import { SearchableDropDownModule } from '../searchable-dropdown/searchable-dropdown.module';

@NgModule({
    declarations: [IframeComponent, IframePortletLegacyComponent],
    exports: [DotLoadingIndicatorModule, IframeComponent, IframePortletLegacyComponent],
    imports: [
        CommonModule,
        FormsModule,
        SearchableDropDownModule,
        DotLoadingIndicatorModule,
        RouterModule,
        DotOverlayMaskModule,
        NotLicensedModule,
        DotPipesModule,
        DotSafeUrlPipe
    ],
    providers: [IframeOverlayService, DotCustomEventHandlerService, DotIframePortletLegacyResolver]
})
export class IFrameModule {}
