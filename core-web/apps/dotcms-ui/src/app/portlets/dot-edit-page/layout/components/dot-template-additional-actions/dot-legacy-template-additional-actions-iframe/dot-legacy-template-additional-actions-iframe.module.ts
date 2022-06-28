import { NgModule } from '@angular/core';
import { DotMenuService } from '@services/dot-menu.service';
import { IFrameModule } from '@components/_common/iframe/iframe.module';
import { DotLegacyTemplateAdditionalActionsComponent } from './dot-legacy-template-additional-actions-iframe.component';
import { CommonModule } from '@angular/common';

@NgModule({
    declarations: [DotLegacyTemplateAdditionalActionsComponent],
    imports: [IFrameModule, CommonModule],
    exports: [DotLegacyTemplateAdditionalActionsComponent],
    providers: [DotMenuService]
})
export class DotTemplateAdditionalActionsIframeModule {}
