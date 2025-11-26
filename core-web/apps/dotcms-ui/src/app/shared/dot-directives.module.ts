import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotRippleEffectDirective } from '../view/directives/ripple/ripple-effect.directive';

@NgModule({
    declarations: [],
    imports: [CommonModule, DotRippleEffectDirective],
    exports: [DotRippleEffectDirective]
})
export class DotDirectivesModule {}
