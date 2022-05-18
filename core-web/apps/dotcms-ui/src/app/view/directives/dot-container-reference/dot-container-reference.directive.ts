import { Directive, ViewContainerRef } from '@angular/core';

@Directive({
    selector: '[dotContainerReference]'
})
export class DotContainerReferenceDirective {
    constructor(public viewContainerRef: ViewContainerRef) {}
}
