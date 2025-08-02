import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotMessagePipe } from '@dotcms/ui';

import { DotBulkInformationComponent } from './dot-bulk-information.component';

@NgModule({
    imports: [CommonModule, DotMessagePipe],
    exports: [DotBulkInformationComponent],
    declarations: [DotBulkInformationComponent]
})
export class DotBulkInformationModule {}
