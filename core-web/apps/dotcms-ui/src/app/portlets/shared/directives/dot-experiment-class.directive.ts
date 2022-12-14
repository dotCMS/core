import { Directive, ElementRef, Optional, Renderer2, Self } from '@angular/core';
import { ActivatedRoute, Params } from '@angular/router';
import { DotEditPageNavComponent } from '@portlets/dot-edit-page/main/dot-edit-page-nav/dot-edit-page-nav.component';

const EDIT_PAGE_VARIANT = 'edit-page-variant-mode';

@Directive({
    standalone: true,
    selector: '[dotExperimentClass]'
})
export class DotExperimentClassDirective {
    constructor(
        private readonly route: ActivatedRoute,
        private renderer: Renderer2,
        hostElement: ElementRef,
        @Optional() @Self() private readonly dotEditPageNavComponent: DotEditPageNavComponent
    ) {
        this.route.queryParams.subscribe((queryParams) => {
            if (this.isEditPageVariant(queryParams)) {
                renderer.addClass(hostElement.nativeElement, EDIT_PAGE_VARIANT);
                this.setNavBarInIsVariantMode(true);
            } else {
                renderer.removeClass(hostElement.nativeElement, EDIT_PAGE_VARIANT);
                this.setNavBarInIsVariantMode(false);
            }
        });
    }

    private isEditPageVariant(queryParams: Params) {
        const { editPageTab, variationName, experimentId } = queryParams;

        return !!experimentId && !!editPageTab && !!variationName;
    }

    private setNavBarInIsVariantMode(state: boolean) {
        if (this.dotEditPageNavComponent) {
            this.dotEditPageNavComponent.isVariantMode = state;
        }
    }
}
