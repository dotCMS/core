import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/primeng';
import { NgModule } from '@angular/core';
import { PushPublishEnvSelectorComponent } from './dot-push-publish-env-selector.component';
import { DataListModule, MultiSelectModule } from 'primeng/primeng';
import { PushPublishService } from '@services/push-publish/push-publish.service';
import { DotCurrentUserService } from '@services/dot-current-user/dot-current-user.service';
import { DotPipesModule } from '@pipes/dot-pipes.module';

@NgModule({
    declarations: [PushPublishEnvSelectorComponent],
    exports: [PushPublishEnvSelectorComponent],
    imports: [
        CommonModule,
        DataListModule,
        ButtonModule,
        FormsModule,
        MultiSelectModule,
        DotPipesModule
    ],
    providers: [PushPublishService, DotCurrentUserService]
})
export class PushPublishEnvSelectorModule {}
