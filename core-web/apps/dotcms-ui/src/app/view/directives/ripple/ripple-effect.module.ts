import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { DotRippleEffectDirective } from './ripple-effect.directive';

@NgModule({
    imports: [CommonModule],
    declarations: [DotRippleEffectDirective],
    exports: [DotRippleEffectDirective]
})
export class RippleEffectModule {}
