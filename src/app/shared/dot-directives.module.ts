import { CommonModule } from '@angular/common';
// import { MessageKeysModule } from '@directives/message-keys/message-keys.module';
// import { RippleEffectModule } from '@directives/ripple/ripple-effect.module';
// import { MdInputTextModule } from '@directives/md-inputtext/md-input-text.module';
import { NgModule } from '@angular/core';
import { MessageKeyDirective } from '@directives/message-keys/message-keys.directive';
import { DotRippleEffectDirective } from '@directives/ripple/ripple-effect.directive';
import { MaterialDesignTextfieldDirective } from '@directives/md-inputtext/md-input-text.directive';

@NgModule({
    declarations: [MessageKeyDirective, DotRippleEffectDirective, MaterialDesignTextfieldDirective],
    imports: [
        CommonModule
        // MessageKeysModule,
        // RippleEffectModule,
        // MdInputTextModule
    ],
    exports: [MessageKeyDirective, DotRippleEffectDirective, MaterialDesignTextfieldDirective]
})
export class DotDirectivesModule {}
