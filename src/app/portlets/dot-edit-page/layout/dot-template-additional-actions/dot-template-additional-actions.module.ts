import { MenuModule } from 'primeng/primeng';
import { DotMenuService } from '../../../../api/services/dot-menu.service';
import { NgModule } from '@angular/core';
import { IFrameModule } from '../../../../view/components/_common/iframe/index';
import { DotTemplateAdditionalActionsMenuModule } from './dot-template-additional-actions-menu/dot-template-additional-actions-menu.module';
import {
    DotTemplateAdditionalActionsIFrameModule
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
        DotTemplateAdditionalActionsIFrameModule
    ],
    exports: [
        DotLegacyTemplateAdditionalActionsComponent,
        DotTemplateAdditionalActionsMenuComponent
    ],
    providers: []
})
export class DotTemplateAdditionalActionsModule {}
