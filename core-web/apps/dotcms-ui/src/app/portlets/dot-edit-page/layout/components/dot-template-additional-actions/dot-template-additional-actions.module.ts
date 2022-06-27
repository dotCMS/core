import { NgModule } from '@angular/core';

import { DotTemplateAdditionalActionsIframeModule } from './dot-legacy-template-additional-actions-iframe/dot-legacy-template-additional-actions-iframe.module';
import { DotLegacyTemplateAdditionalActionsComponent } from './dot-legacy-template-additional-actions-iframe/dot-legacy-template-additional-actions-iframe.component';
import { IFrameModule } from '@components/_common/iframe';

@NgModule({
    declarations: [],
    imports: [DotTemplateAdditionalActionsIframeModule, IFrameModule],
    exports: [DotLegacyTemplateAdditionalActionsComponent],
    providers: []
})
export class DotTemplateAdditionalActionsModule {}
