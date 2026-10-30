import { Directive, TemplateRef } from '@angular/core';

/**
 * Get the content TemplateRef of the Option
 */
@Directive({
    selector: '[dotOptionContent]',
    standalone: false
})
export class DotExperimentOptionContentDirective {
    constructor(public templateRef: TemplateRef<unknown>) {}
}
