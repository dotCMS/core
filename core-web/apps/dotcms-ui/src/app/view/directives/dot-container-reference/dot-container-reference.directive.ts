import { Directive, ViewContainerRef } from '@angular/core';

@Directive({
    selector: '[dotContainerReference]',
    standalone: false
})
export class DotContainerReferenceDirective {
    constructor(public viewContainerRef: ViewContainerRef) {}
}
