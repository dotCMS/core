import { Subject } from 'rxjs';

import { Directive, ElementRef, OnDestroy, Renderer2, inject } from '@angular/core';
import { ActivatedRoute, Params } from '@angular/router';

import { DotEditPageNavComponent } from '../../dot-edit-page/main/dot-edit-page-nav/dot-edit-page-nav.component';

const EDIT_PAGE_VARIANT = 'edit-page-variant-mode';

/**
 * Directive to detect is Edit Page is rendering a Variant
 * Do
 * 1. Add a class to the host
 * 2. If is assigned to DotEditPageNavComponent set the component in isVariantMode
 */
@Directive({
    standalone: true,
    selector: '[dotExperimentClass]'
})
export class DotExperimentClassDirective implements OnDestroy {
    private readonly route = inject(ActivatedRoute);
    private renderer = inject(Renderer2);
    private readonly dotEditPageNavComponent = inject(DotEditPageNavComponent, {
        optional: true,
        self: true
    });

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor() {
        const renderer = this.renderer;
        const hostElement = inject(ElementRef);

        this.route.queryParams.subscribe((queryParams) => {
            if (this.isEditPageVariant(queryParams)) {
                renderer.addClass(hostElement.nativeElement, EDIT_PAGE_VARIANT);
                this.setNavBarComponentIsVariantMode(true);
            } else {
                renderer.removeClass(hostElement.nativeElement, EDIT_PAGE_VARIANT);
                this.setNavBarComponentIsVariantMode(false);
            }
        });
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    private isEditPageVariant(queryParams: Params) {
        const { mode, variantName, experimentId } = queryParams;

        return !!experimentId && !!mode && !!variantName;
    }

    private setNavBarComponentIsVariantMode(state: boolean) {
        if (this.dotEditPageNavComponent) {
            this.dotEditPageNavComponent.isVariantMode = state;
        }
    }
}
