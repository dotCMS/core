import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotAutofocusDirective } from '@directives/dot-autofocus/dot-autofocus.directive';

@NgModule({
    imports: [CommonModule],
    declarations: [DotAutofocusDirective],
    exports: [DotAutofocusDirective]
})
export class DotAutofocusModule {}
