import { Directive, Input, OnInit, ViewContainerRef, TemplateRef } from '@angular/core';

import { getUVEState } from '@dotcms/uve';
import { UVE_MODE, UVEState } from '@dotcms/uve/types';

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
 * @implements {OnInit}
 */
@Directive({
    selector: '[dotCMSShowWhen]',
    standalone: true
})
export class DotCMSShowWhenDirective implements OnInit {
    #mode: UVE_MODE = UVE_MODE.EDIT;

    @Input() set mode(value: UVE_MODE) {
        this.#mode = value;
        this.updateViewContainer();
    }

    get shouldShow(): boolean {
        const state: UVEState | undefined = getUVEState();

        return state?.mode === this.#mode;
    }

    constructor(
        private viewContainer: ViewContainerRef,
        private templateRef: TemplateRef<unknown>
    ) {}

    ngOnInit(): void {
        this.updateViewContainer();
    }

    private updateViewContainer() {
        if (this.shouldShow) {
            this.viewContainer.createEmbeddedView(this.templateRef);
        } else {
            this.viewContainer.clear();
        }
    }
}
