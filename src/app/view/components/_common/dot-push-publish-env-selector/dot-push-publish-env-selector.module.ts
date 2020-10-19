import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { NgModule } from '@angular/core';
import { PushPublishEnvSelectorComponent } from './dot-push-publish-env-selector.component';
import { PushPublishService } from '@services/push-publish/push-publish.service';
import { DotCurrentUserService } from '@services/dot-current-user/dot-current-user.service';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { MultiSelectModule } from 'primeng/multiselect';

@NgModule({
    declarations: [PushPublishEnvSelectorComponent],
    exports: [PushPublishEnvSelectorComponent],
    imports: [
        CommonModule,
        ButtonModule,
        FormsModule,
        MultiSelectModule,
        DotPipesModule
    ],
    providers: [PushPublishService, DotCurrentUserService]
})
export class PushPublishEnvSelectorModule {}
