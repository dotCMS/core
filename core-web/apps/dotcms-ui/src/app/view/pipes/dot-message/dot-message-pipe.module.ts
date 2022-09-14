import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotMessagePipe } from './dot-message.pipe';

@NgModule({
    imports: [CommonModule],
    declarations: [DotMessagePipe, DotMessagePipe],
    exports: [DotMessagePipe, DotMessagePipe]
})
export class DotMessagePipeModule {}
