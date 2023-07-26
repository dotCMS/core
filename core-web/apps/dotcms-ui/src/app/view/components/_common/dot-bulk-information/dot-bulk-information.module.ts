import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotBulkInformationComponent } from '@components/_common/dot-bulk-information/dot-bulk-information.component';
import { DotMessagePipe } from '@dotcms/ui';

@NgModule({
    imports: [CommonModule, DotMessagePipe],
    exports: [DotBulkInformationComponent],
    declarations: [DotBulkInformationComponent]
})
export class DotBulkInformationModule {}
