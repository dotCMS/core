import { Directive, Input, OnInit, ViewContainerRef, TemplateRef } from '@angular/core';

import { getUVEState } from '@dotcms/uve';
import { UVE_MODE, UVEState } from '@dotcms/uve/types';

/**
 * Directive to show a template when the UVE is in a specific mode.
 *
 * @example
 * <div *dotCMSShowInUVEMode="UVE_MODE.EDIT">
 *     This will be shown when the UVE is in edit mode.
 * </div>
 *
 * @export
 * @class DotShowInUVEModeDirective
 * @implements {OnInit}
 */
@Directive({
    selector: '[dotCMSShowInUVEMode]',
    standalone: true
})
export class DotShowInUVEModeDirective implements OnInit {
    @Input() mode: UVE_MODE;

    constructor(
        private viewContainer: ViewContainerRef,
        private templateRef: TemplateRef<unknown>
    ) {
        this.mode = UVE_MODE.EDIT;
    }

    ngOnInit(): void {
        const state: UVEState | undefined = getUVEState();
        const shouldShow = state?.mode === this.mode;

        if (shouldShow) {
            this.viewContainer.createEmbeddedView(this.templateRef);
        }
    }
}
