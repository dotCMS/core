import { Directive, ViewContainerRef } from '@angular/core';

@Directive({
    standalone: true,
    selector: '[dotDynamic]'
})
export class DotDynamicDirective {
    constructor(public viewContainerRef: ViewContainerRef) {}
}
