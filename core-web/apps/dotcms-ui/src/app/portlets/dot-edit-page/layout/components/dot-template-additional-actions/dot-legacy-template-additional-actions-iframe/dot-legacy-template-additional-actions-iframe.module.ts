import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { IFrameModule } from '@components/_common/iframe/iframe.module';
import { DotMenuService } from '@dotcms/app/api/services/dot-menu.service';

import { DotLegacyTemplateAdditionalActionsComponent } from './dot-legacy-template-additional-actions-iframe.component';

@NgModule({
    declarations: [DotLegacyTemplateAdditionalActionsComponent],
    imports: [IFrameModule, CommonModule],
    exports: [DotLegacyTemplateAdditionalActionsComponent],
    providers: [DotMenuService]
})
export class DotTemplateAdditionalActionsIframeModule {}
