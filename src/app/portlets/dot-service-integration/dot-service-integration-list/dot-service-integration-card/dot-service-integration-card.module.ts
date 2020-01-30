import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CardModule } from 'primeng/primeng';

import { DotServiceIntegrationCardComponent } from './dot-service-integration-card.component';
import { DotAvatarModule } from '@components/_common/dot-avatar/dot-avatar.module';

@NgModule({
    imports: [CommonModule, CardModule, DotAvatarModule],
    declarations: [DotServiceIntegrationCardComponent],
    exports: [DotServiceIntegrationCardComponent]
})
export class DotServiceIntegrationCardModule {}
