import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/primeng';

import { DotAppsConfigurationDetailComponent } from './dot-apps-configuration-detail.component';
import { DotAppsService } from '@services/dot-apps/dot-apps.service';
import { DotAppsConfigurationDetailResolver } from './dot-apps-configuration-detail-resolver.service';
import { DotCopyButtonModule } from '@components/dot-copy-button/dot-copy-button.module';
import { DotAppsConfigurationDetailFormModule } from './dot-apps-configuration-detail-form/dot-apps-configuration-detail-form.module';
import { DotKeyValueModule } from '@components/dot-key-value-ng/dot-key-value-ng.module';
import { DotAppsConfigurationHeaderModule } from '../dot-apps-configuration-header/dot-apps-configuration-header.module';
import { DotPipesModule } from '@pipes/dot-pipes.module';

@NgModule({
    imports: [
        ButtonModule,
        CommonModule,
        DotKeyValueModule,
        DotCopyButtonModule,
        DotAppsConfigurationHeaderModule,
        DotAppsConfigurationDetailFormModule,
        DotPipesModule
    ],
    declarations: [DotAppsConfigurationDetailComponent],
    exports: [DotAppsConfigurationDetailComponent],
    providers: [DotAppsService, DotAppsConfigurationDetailResolver]
})
export class DotAppsConfigurationDetailModule {}
