import { Directive, ViewContainerRef, inject } from '@angular/core';

@Directive({
    selector: '[dotContainerReference]'
})
export class DotContainerReferenceDirective {
    viewContainerRef = inject(ViewContainerRef);
}
