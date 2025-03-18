import { Directive, Input, OnInit, ViewContainerRef, TemplateRef } from '@angular/core';

import { getUVEState } from '@dotcms/uve';
import { UVE_MODE, UVEState } from '@dotcms/uve/types';

@Directive({
    selector: '[dotCMSShowInUVE]',
    standalone: true
})
export class DotShowInUVEDirective implements OnInit {
    @Input() when: UVE_MODE;

    constructor(
        private viewContainer: ViewContainerRef,
        private templateRef: TemplateRef<unknown>
    ) {
        this.when = UVE_MODE.EDIT;
    }

    ngOnInit(): void {
        const state: UVEState | undefined = getUVEState();
        const shouldShow = state?.mode === this.when;

        if (shouldShow) {
            this.viewContainer.createEmbeddedView(this.templateRef);
        }
    }
}
