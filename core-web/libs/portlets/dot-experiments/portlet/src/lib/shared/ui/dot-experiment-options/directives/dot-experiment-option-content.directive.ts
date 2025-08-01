import { Directive, TemplateRef, inject } from '@angular/core';

/**
 * Get the content TemplateRef of the Option
 */
@Directive({
    selector: '[dotOptionContent]'
})
export class DotExperimentOptionContentDirective {
    templateRef = inject<TemplateRef<unknown>>(TemplateRef);
}
