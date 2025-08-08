import { Subject } from 'rxjs';

import { Directive, ElementRef, OnDestroy, Renderer2, inject } from '@angular/core';
import { ActivatedRoute, NavigationEnd, Router } from '@angular/router';

import { filter, takeUntil } from 'rxjs/operators';

import { DotEditPageNavComponent } from '../dot-edit-page-nav.component';

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
    private readonly dotEditPageNavComponent = inject(DotEditPageNavComponent, {
        optional: true,
        self: true
    });
    private readonly router = inject(Router);
    private readonly route = inject(ActivatedRoute);
    private renderer = inject(Renderer2);
    private hostElement = inject(ElementRef);

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor() {
        const dotEditPageNavComponent = this.dotEditPageNavComponent;
        const router = this.router;

        if (dotEditPageNavComponent) {
            router.events
                .pipe(
                    filter((event) => event instanceof NavigationEnd),
                    takeUntil(this.destroy$)
                )
                .subscribe((event: NavigationEnd) => {
                    this.addPortletClass(event.urlAfterRedirects);
                });
            //when the page is refreshed by the user, the router event is not triggered
            this.addPortletClass(this.router.url);
        } else {
            console.warn('DotNavbarDirective is for use with DotEditPageNavComponent');
        }
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    private addPortletClass(url: string) {
        const key = Object.keys(urlPortletRules).find((key) => url.includes(key));
        if (key) {
            this.removePortletsClasses();
            this.renderer.addClass(this.hostElement.nativeElement, urlPortletRules[key].clazz);
        }
    }

    private removePortletsClasses() {
        Object.keys(urlPortletRules).forEach((key) =>
            this.renderer.removeClass(this.hostElement.nativeElement, urlPortletRules[key].clazz)
        );
    }
}
