import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotMaxlengthDirective } from '@directives/dot-maxlength/dot-maxlength.directive';

@NgModule({
    imports: [CommonModule],
    declarations: [DotMaxlengthDirective],
    exports: [DotMaxlengthDirective]
})
export class DotMaxlengthModule {}
