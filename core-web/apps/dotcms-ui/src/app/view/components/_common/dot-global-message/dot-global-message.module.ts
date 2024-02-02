import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotGlobalMessageService } from '@dotcms/data-access';
import { DotSpinnerModule } from '@dotcms/ui';

import { DotGlobalMessageComponent } from './dot-global-message.component';

@NgModule({
    imports: [CommonModule, DotSpinnerModule],
    declarations: [DotGlobalMessageComponent],
    exports: [DotGlobalMessageComponent],
    providers: [DotGlobalMessageService]
})
export class DotGlobalMessageModule {}
