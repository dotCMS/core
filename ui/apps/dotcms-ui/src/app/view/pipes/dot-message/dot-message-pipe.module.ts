import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotMessagePipe } from './dot-message.pipe';

@NgModule({
    imports: [CommonModule],
    declarations: [DotMessagePipe],
    exports: [DotMessagePipe]
})
export class DotMessagePipeModule {}
