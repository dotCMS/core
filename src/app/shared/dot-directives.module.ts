import { CommonModule } from '@angular/common';
// import { MessageKeysModule } from '../view/directives/message-keys/message-keys.module';
// import { RippleEffectModule } from '../view/directives/ripple/ripple-effect.module';
// import { MdInputTextModule } from '../view/directives/md-inputtext/md-input-text.module';
import { NgModule } from '@angular/core';
import { MessageKeyDirective } from '../view/directives/message-keys/message-keys.directive';
import { DotRippleEffectDirective } from '../view/directives/ripple/ripple-effect.directive';
import { MaterialDesignTextfieldDirective } from '../view/directives/md-inputtext/md-input-text.directive';

@NgModule({
    declarations: [
        MessageKeyDirective,
        DotRippleEffectDirective,
        MaterialDesignTextfieldDirective
    ],
    imports: [
        CommonModule
        // MessageKeysModule,
        // RippleEffectModule,
        // MdInputTextModule
    ],
    exports: [
        MessageKeyDirective,
        DotRippleEffectDirective,
        MaterialDesignTextfieldDirective
    ]
})
export class DotDirectivesModule {}

