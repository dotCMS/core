import { MenuModule } from 'primeng/primeng';
import { DotMenuService } from '../../../../api/services/dot-menu.service';
import { NgModule } from '@angular/core';
import { IFrameModule } from '../../../../view/components/_common/iframe/index';
import { DotTemplateAdditionalActionsMenuModule } from './dot-template-additional-actions-menu/dot-template-additional-actions-menu.module';
import {
    DotTemplateAdditionalActionsIframeModule
} from './dot-legacy-template-additional-actions-iframe/dot-legacy-template-additional-actions-iframe.module';
import {
    DotLegacyTemplateAdditionalActionsComponent
} from './dot-legacy-template-additional-actions-iframe/dot-legacy-template-additional-actions-iframe.component';
import {
    DotTemplateAdditionalActionsMenuComponent
} from './dot-template-additional-actions-menu/dot-template-additional-actions-menu.component';

@NgModule({
    declarations: [],
    imports: [
        DotTemplateAdditionalActionsMenuModule,
        DotTemplateAdditionalActionsIframeModule
    ],
    exports: [
        DotLegacyTemplateAdditionalActionsComponent,
        DotTemplateAdditionalActionsMenuComponent
    ],
    providers: []
})
export class DotTemplateAdditionalActionsModule {}
