import { NgModule } from '@angular/core';

import { IFrameModule } from '@components/_common/iframe';

import { DotLegacyTemplateAdditionalActionsComponent } from './dot-legacy-template-additional-actions-iframe/dot-legacy-template-additional-actions-iframe.component';
import { DotTemplateAdditionalActionsIframeModule } from './dot-legacy-template-additional-actions-iframe/dot-legacy-template-additional-actions-iframe.module';

@NgModule({
    declarations: [],
    imports: [DotTemplateAdditionalActionsIframeModule, IFrameModule],
    exports: [DotLegacyTemplateAdditionalActionsComponent],
    providers: []
})
export class DotTemplateAdditionalActionsModule {}
