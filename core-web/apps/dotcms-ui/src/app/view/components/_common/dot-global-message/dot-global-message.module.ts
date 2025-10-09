import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotGlobalMessageService } from '@dotcms/data-access';
import { DotSpinnerComponent } from '@dotcms/ui';

import { DotGlobalMessageComponent } from './dot-global-message.component';

@NgModule({
    imports: [CommonModule, DotSpinnerComponent],
    declarations: [DotGlobalMessageComponent],
    exports: [DotGlobalMessageComponent],
    providers: [DotGlobalMessageService]
})
export class DotGlobalMessageModule {}
