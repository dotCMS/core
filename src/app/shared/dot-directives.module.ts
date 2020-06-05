import { CommonModule } from '@angular/common';
import { RippleEffectModule } from '@directives/ripple/ripple-effect.module';
import { MdInputTextModule } from '@directives/md-inputtext/md-input-text.module';
import { NgModule } from '@angular/core';
import { LowercasePipe, DotMessagePipe } from '../view/pipes';

@NgModule({
    declarations: [LowercasePipe, DotMessagePipe],
    imports: [CommonModule, RippleEffectModule, MdInputTextModule],
    exports: [DotMessagePipe]
})
export class DotDirectivesModule {}
