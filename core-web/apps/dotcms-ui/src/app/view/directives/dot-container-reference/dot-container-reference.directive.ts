import { Directive, ViewContainerRef, inject } from '@angular/core';

@Directive({
    selector: '[dotContainerReference]',
    standalone: false
})
export class DotContainerReferenceDirective {
    viewContainerRef = inject(ViewContainerRef);
}
