import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { MultiSelectModule } from 'primeng/multiselect';

import { DotCurrentUserService, PushPublishService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { PushPublishEnvSelectorComponent } from './dot-push-publish-env-selector.component';

import { DotPipesModule } from '../../../pipes/dot-pipes.module';

@NgModule({
    declarations: [PushPublishEnvSelectorComponent],
    exports: [PushPublishEnvSelectorComponent],
    imports: [
        CommonModule,
        ButtonModule,
        FormsModule,
        MultiSelectModule,
        DotPipesModule,
        DotMessagePipe
    ],
    providers: [PushPublishService, DotCurrentUserService]
})
export class PushPublishEnvSelectorModule {}
