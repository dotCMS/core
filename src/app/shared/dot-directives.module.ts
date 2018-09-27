import { CommonModule } from '@angular/common';
import { MessageKeysModule } from '@directives/message-keys/message-keys.module';
import { RippleEffectModule } from '@directives/ripple/ripple-effect.module';
import { MdInputTextModule } from '@directives/md-inputtext/md-input-text.module';
import { NgModule } from '@angular/core';
import { LowercasePipe } from '../view/pipes';

@NgModule({
    declarations: [LowercasePipe],
    imports: [CommonModule, MessageKeysModule, RippleEffectModule, MdInputTextModule],
    exports: []
})
export class DotDirectivesModule {}
