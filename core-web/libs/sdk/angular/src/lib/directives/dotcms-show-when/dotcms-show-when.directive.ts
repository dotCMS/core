import { Directive, Input, ViewContainerRef, TemplateRef, inject } from '@angular/core';

import { UVE_MODE, UVEState } from '@dotcms/types';
import { getUVEState } from '@dotcms/uve';

/**
 * Directive to show a template when the UVE is in a specific mode.
 *
 * @example
 * <div *dotCMSShowWhen="UVE_MODE.EDIT">
 *     This will be shown when the UVE is in edit mode.
 * </div>
 *
 * @export
 * @class DotCMSShowWhenDirective
 */
@Directive({
    selector: '[dotCMSShowWhen]'
})
export class DotCMSShowWhenDirective {
    #when: UVE_MODE = UVE_MODE.EDIT;
    #hasView = false;

    @Input() set dotCMSShowWhen(value: UVE_MODE) {
        this.#when = value;
        this.updateViewContainer();
    }

    #viewContainerRef = inject(ViewContainerRef);
    #templateRef = inject(TemplateRef);

    private updateViewContainer() {
        const state: UVEState | undefined = getUVEState();

        const shouldShow = state?.mode === this.#when;

        if (shouldShow && !this.#hasView) {
            this.#viewContainerRef.createEmbeddedView(this.#templateRef);
            this.#hasView = true;
        } else if (!shouldShow && this.#hasView) {
            this.#viewContainerRef.clear();
            this.#hasView = false;
        }
    }
}
