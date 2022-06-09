import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotBulkInformationComponent } from '@components/_common/dot-bulk-information/dot-bulk-information.component';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';

@NgModule({
    imports: [CommonModule, DotMessagePipeModule],
    exports: [DotBulkInformationComponent],
    declarations: [DotBulkInformationComponent]
})
export class DotBulkInformationModule {}
