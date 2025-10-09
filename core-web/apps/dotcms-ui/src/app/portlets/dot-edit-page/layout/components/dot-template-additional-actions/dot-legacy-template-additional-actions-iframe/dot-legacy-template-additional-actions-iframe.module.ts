import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotLegacyTemplateAdditionalActionsComponent } from './dot-legacy-template-additional-actions-iframe.component';

import { DotMenuService } from '../../../../../../api/services/dot-menu.service';
import { IframeComponent } from '../../../../../../view/components/_common/iframe/iframe-component/iframe.component';

@NgModule({
    declarations: [DotLegacyTemplateAdditionalActionsComponent],
    imports: [IframeComponent, CommonModule],
    exports: [DotLegacyTemplateAdditionalActionsComponent],
    providers: [DotMenuService]
})
export class DotTemplateAdditionalActionsIframeModule {}
