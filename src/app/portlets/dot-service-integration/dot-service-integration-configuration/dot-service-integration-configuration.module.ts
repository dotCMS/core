import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { InputTextModule, ButtonModule } from 'primeng/primeng';

import { DotServiceIntegrationConfigurationComponent } from './dot-service-integration-configuration.component';
import { DotServiceIntegrationService } from '@services/dot-service-integration/dot-service-integration.service';
import { DotServiceIntegrationConfigurationResolver } from './dot-service-integration-configuration-resolver.service';
import { DotAvatarModule } from '@components/_common/dot-avatar/dot-avatar.module';
import { DotActionButtonModule } from '@components/_common/dot-action-button/dot-action-button.module';
import { DotCopyButtonModule } from '@components/dot-copy-button/dot-copy-button.module';
import { DotServiceIntegrationConfigurationListModule } from './dot-service-integration-configuration-list/dot-service-integration-configuration-list.module';

@NgModule({
    imports: [
        InputTextModule,
        ButtonModule,
        CommonModule,
        DotAvatarModule,
        DotActionButtonModule,
        DotCopyButtonModule,
        DotServiceIntegrationConfigurationListModule,
    ],
    declarations: [DotServiceIntegrationConfigurationComponent],
    exports: [DotServiceIntegrationConfigurationComponent],
    providers: [DotServiceIntegrationService, DotServiceIntegrationConfigurationResolver]
})
export class DotServiceIntegrationConfigurationModule {}
