import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { InputTextModule } from 'primeng/primeng';

import { DotServiceIntegrationListComponent } from './dot-service-integration-list.component';
import { DotServiceIntegrationCardModule } from './dot-service-integration-card/dot-service-integration-card.module';
import { DotServiceIntegrationService } from '@services/dot-service-integration/dot-service-integration.service';
import { DotServiceIntegrationListResolver } from './dot-service-integration-list-resolver.service';

@NgModule({
    imports: [InputTextModule, CommonModule, DotServiceIntegrationCardModule],
    declarations: [DotServiceIntegrationListComponent],
    exports: [DotServiceIntegrationListComponent],
    providers: [DotServiceIntegrationService, DotServiceIntegrationListResolver]
})
export class DotServiceIntegrationListModule {}
