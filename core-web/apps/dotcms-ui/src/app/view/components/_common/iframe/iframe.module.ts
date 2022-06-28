import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgModule } from '@angular/core';
import { SearchableDropDownModule } from '../searchable-dropdown/searchable-dropdown.module';
import { IframeComponent } from './iframe-component';
import { DotLoadingIndicatorModule } from './dot-loading-indicator/dot-loading-indicator.module';
import { IframePortletLegacyComponent } from './iframe-porlet-legacy';
import { IframeOverlayService } from './service/iframe-overlay.service';
import { DotCustomEventHandlerService } from '@services/dot-custom-event-handler/dot-custom-event-handler.service';
import { RouterModule } from '@angular/router';
import { DotOverlayMaskModule } from '@components/_common/dot-overlay-mask/dot-overlay-mask.module';
import { DotIframePortletLegacyResolver } from './service/dot-iframe-porlet-legacy-resolver.service';
import { NotLicensedModule } from '@components/not-licensed/not-licensed.module';
import { DotPipesModule } from '@pipes/dot-pipes.module';

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
        DotPipesModule
    ],
    providers: [IframeOverlayService, DotCustomEventHandlerService, DotIframePortletLegacyResolver]
})
export class IFrameModule {}
