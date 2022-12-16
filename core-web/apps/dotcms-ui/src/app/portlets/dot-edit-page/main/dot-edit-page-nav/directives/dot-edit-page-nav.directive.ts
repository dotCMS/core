import { Directive, ElementRef, OnDestroy, Optional, Renderer2, Self } from '@angular/core';
import { ActivatedRoute, NavigationEnd, Router } from '@angular/router';
import { filter, takeUntil } from 'rxjs/operators';
import { DotEditPageNavComponent } from '@portlets/dot-edit-page/main/dot-edit-page-nav/dot-edit-page-nav.component';
import { Subject } from 'rxjs';

const urlPortletRules = {
    content: { clazz: 'portlet-content' },
    layout: { clazz: 'portlet-layout' },
    rules: { clazz: 'portlet-rules' },
    experiments: { clazz: 'portlet-experiments' }
};

/**
 * Directive to add a class depending on the current route
 */
@Directive({
    standalone: true,
    selector: '[dotNavbar]'
})
export class DotEditPageNavDirective implements OnDestroy {
    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        @Optional() @Self() private readonly dotEditPageNavComponent: DotEditPageNavComponent,
        private readonly router: Router,
        private readonly route: ActivatedRoute,
        private renderer: Renderer2,
        private hostElement: ElementRef
    ) {
        if (dotEditPageNavComponent) {
            router.events
                .pipe(
                    filter((event) => event instanceof NavigationEnd),
                    takeUntil(this.destroy$)
                )
                .subscribe((event: NavigationEnd) => {
                    const key = Object.keys(urlPortletRules).find((key) =>
                        event.urlAfterRedirects.includes(key)
                    );
                    this.removePortletsClasess();
                    renderer.addClass(hostElement.nativeElement, urlPortletRules[key].clazz);
                });
        } else {
            console.warn('DotNavbarDirective is for use with DotEditPageNavComponent');
        }
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    private removePortletsClasess() {
        Object.keys(urlPortletRules).forEach((key) =>
            this.renderer.removeClass(this.hostElement.nativeElement, urlPortletRules[key].clazz)
        );
    }
}
