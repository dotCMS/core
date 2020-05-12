import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { InputTextModule, ButtonModule } from 'primeng/primeng';

import { DotAppsConfigurationComponent } from './dot-apps-configuration.component';
import { DotAppsService } from '@services/dot-apps/dot-apps.service';
import { DotAppsConfigurationResolver } from './dot-apps-configuration-resolver.service';
import { DotAvatarModule } from '@components/_common/dot-avatar/dot-avatar.module';
import { DotActionButtonModule } from '@components/_common/dot-action-button/dot-action-button.module';
import { DotCopyButtonModule } from '@components/dot-copy-button/dot-copy-button.module';
import { DotAppsConfigurationListModule } from './dot-apps-configuration-list/dot-apps-configuration-list.module';
import { NgxMdModule } from 'ngx-md';
import { DotAppsConfigurationHeaderModule } from '../dot-apps-configuration-header/dot-apps-configuration-header.module';

@NgModule({
    imports: [
        InputTextModule,
        ButtonModule,
        CommonModule,
        DotAvatarModule,
        DotActionButtonModule,
        DotCopyButtonModule,
        DotAppsConfigurationHeaderModule,
        DotAppsConfigurationListModule,
        NgxMdModule
    ],
    declarations: [DotAppsConfigurationComponent],
    exports: [DotAppsConfigurationComponent],
    providers: [DotAppsService, DotAppsConfigurationResolver]
})
export class DotAppsConfigurationModule {}
