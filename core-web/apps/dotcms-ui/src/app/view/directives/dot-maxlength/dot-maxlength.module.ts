import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotMaxlengthDirective } from './dot-maxlength.directive';

@NgModule({
    imports: [CommonModule],
    declarations: [DotMaxlengthDirective],
    exports: [DotMaxlengthDirective]
})
export class DotMaxlengthModule {}
