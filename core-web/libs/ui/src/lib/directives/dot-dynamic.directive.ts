import { Directive, ViewContainerRef, inject } from '@angular/core';

@Directive({
    standalone: true,
    selector: '[dotDynamic]'
})
export class DotDynamicDirective {
    viewContainerRef = inject(ViewContainerRef);
}
