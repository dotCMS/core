import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { InputTextModule, ButtonModule } from 'primeng/primeng';

import { DotServiceIntegrationConfigurationListComponent } from './dot-service-integration-configuration-list.component';
import { DotServiceIntegrationService } from '@services/dot-service-integration/dot-service-integration.service';
import { DotServiceIntegrationConfigurationListResolver } from './dot-service-integration-configuration-list-resolver.service';
import { DotAvatarModule } from '@components/_common/dot-avatar/dot-avatar.module';
import { DotActionButtonModule } from '@components/_common/dot-action-button/dot-action-button.module';
import { DotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { DotCopyButtonModule } from '@components/dot-copy-button/dot-copy-button.module';

@NgModule({
    imports: [
        InputTextModule,
        ButtonModule,
        CommonModule,
        DotAvatarModule,
        DotActionButtonModule,
        DotCopyButtonModule,
        DotIconButtonModule
    ],
    declarations: [DotServiceIntegrationConfigurationListComponent],
    exports: [DotServiceIntegrationConfigurationListComponent],
    providers: [DotServiceIntegrationService, DotServiceIntegrationConfigurationListResolver]
})
export class DotServiceIntegrationConfigurationListModule {}
