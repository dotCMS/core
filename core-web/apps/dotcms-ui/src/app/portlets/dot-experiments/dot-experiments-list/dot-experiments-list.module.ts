import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { DotExperimentsListComponent } from './dot-experiments-list.component';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';

@NgModule({
    declarations: [DotExperimentsListComponent],
    imports: [CommonModule, DotMessagePipeModule],
    exports: [DotExperimentsListComponent]
})
export class DotExperimentsListModule {}
