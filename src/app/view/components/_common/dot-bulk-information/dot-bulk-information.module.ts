import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotBulkInformationComponent } from '@components/_common/dot-bulk-information/dot-bulk-information.component';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { DotMessageService } from '@services/dot-message/dot-messages.service';

@NgModule({
    imports: [CommonModule, DotMessagePipeModule],
    exports: [DotBulkInformationComponent],
    providers: [DotMessageService],
    declarations: [DotBulkInformationComponent]
})
export class DotBulkInformationModule {}
