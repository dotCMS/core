import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotRippleEffectDirective } from './ripple-effect.directive';

@NgModule({
    imports: [CommonModule],
    declarations: [DotRippleEffectDirective],
    exports: [DotRippleEffectDirective]
})
export class RippleEffectModule {}
