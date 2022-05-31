import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotWhatsChangedComponent } from './dot-whats-changed.component';
import { IFrameModule } from '@components/_common/iframe';
import { DotDiffPipeModule } from '@pipes/dot-diff/dot-diff.pipe.module';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';

@NgModule({
    imports: [CommonModule, IFrameModule, DotDiffPipeModule, DotMessagePipeModule],
    declarations: [DotWhatsChangedComponent],
    exports: [DotWhatsChangedComponent]
})
export class DotWhatsChangedModule {}
