import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { DotExperimentsListComponent } from './dot-experiments-list.component';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { DotExperimentsListRoutingModule } from './dot-experiments-list-routing.module';

@NgModule({
    declarations: [DotExperimentsListComponent],
    imports: [CommonModule, DotExperimentsListRoutingModule, DotMessagePipeModule],
    exports: [DotExperimentsListComponent]
})
export class DotExperimentsListModule {}
