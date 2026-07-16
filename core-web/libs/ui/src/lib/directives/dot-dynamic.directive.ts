import { Directive, ViewContainerRef, inject } from '@angular/core';

@Directive({
    selector: '[dotDynamic]'
})
export class DotDynamicDirective {
    viewContainerRef = inject(ViewContainerRef);
}
