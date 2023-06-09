import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotBulkInformationComponent } from '@components/_common/dot-bulk-information/dot-bulk-information.component';
import { DotMessagePipeModule } from '@dotcms/ui';

@NgModule({
    imports: [CommonModule, DotMessagePipeModule],
    exports: [DotBulkInformationComponent],
    declarations: [DotBulkInformationComponent]
})
export class DotBulkInformationModule {}
