import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgModule } from '@angular/core';
import { SearchableDropDownModule } from '../searchable-dropdown/searchable-dropdown.module';
import { IframeComponent } from './iframe-component';
import { DotLoadingIndicatorModule } from './dot-loading-indicator/dot-loading-indicator.module';
import { IframePortletLegacyComponent } from './iframe-porlet-legacy';
import { IframeOverlayService } from './service/iframe-overlay.service';
import { PIPES } from '../../../../components';
import { DotIframeEventsHandler } from './iframe-porlet-legacy/services/iframe-events-handler.service';

@NgModule({
    declarations: [IframeComponent, IframePortletLegacyComponent, ...PIPES],
    exports: [DotLoadingIndicatorModule, IframeComponent, IframePortletLegacyComponent],
    imports: [CommonModule, FormsModule, SearchableDropDownModule, DotLoadingIndicatorModule],
    providers: [IframeOverlayService, DotIframeEventsHandler]
})
export class IFrameModule {}
