import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/primeng';
import { NgModule } from '@angular/core';
import { PushPublishEnvSelectorComponent } from './dot-push-publish-env-selector.component';
import { DataListModule, MultiSelectModule} from 'primeng/primeng';
import { PushPublishService } from '../../../../api/services/push-publish/push-publish.service';


@NgModule({
    declarations: [PushPublishEnvSelectorComponent],
    exports: [PushPublishEnvSelectorComponent],
    imports: [CommonModule, DataListModule, ButtonModule, FormsModule, MultiSelectModule],
    providers: [PushPublishService]
})
export class PushPublishEnvSelectorModule {}
