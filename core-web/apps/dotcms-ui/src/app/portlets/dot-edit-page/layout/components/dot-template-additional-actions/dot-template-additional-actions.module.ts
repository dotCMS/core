import { NgModule } from '@angular/core';

import { DotLegacyTemplateAdditionalActionsComponent } from './dot-legacy-template-additional-actions-iframe/dot-legacy-template-additional-actions-iframe.component';
import { DotTemplateAdditionalActionsIframeModule } from './dot-legacy-template-additional-actions-iframe/dot-legacy-template-additional-actions-iframe.module';

import { IframeComponent } from '../../../../../view/components/_common/iframe/iframe-component/iframe.component';

@NgModule({
    declarations: [],
    imports: [DotTemplateAdditionalActionsIframeModule, IframeComponent],
    exports: [DotLegacyTemplateAdditionalActionsComponent],
    providers: []
})
export class DotTemplateAdditionalActionsModule {}
