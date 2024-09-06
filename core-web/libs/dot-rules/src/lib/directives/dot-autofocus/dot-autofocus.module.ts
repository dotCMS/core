import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotAutofocusDirective } from './dot-autofocus.directive';

@NgModule({
    imports: [CommonModule],
    declarations: [DotAutofocusDirective],
    exports: [DotAutofocusDirective]
})
export class DotAutofocusModule {}
