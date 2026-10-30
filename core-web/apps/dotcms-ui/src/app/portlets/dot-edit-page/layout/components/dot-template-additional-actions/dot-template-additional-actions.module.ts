import { NgModule } from '@angular/core';

import { DotLegacyTemplateAdditionalActionsComponent } from './dot-legacy-template-additional-actions-iframe/dot-legacy-template-additional-actions-iframe.component';
import { DotTemplateAdditionalActionsIframeModule } from './dot-legacy-template-additional-actions-iframe/dot-legacy-template-additional-actions-iframe.module';

import { IFrameModule } from '../../../../../view/components/_common/iframe/iframe.module';

@NgModule({
    declarations: [],
    imports: [DotTemplateAdditionalActionsIframeModule, IFrameModule],
    exports: [DotLegacyTemplateAdditionalActionsComponent],
    providers: []
})
export class DotTemplateAdditionalActionsModule {}
