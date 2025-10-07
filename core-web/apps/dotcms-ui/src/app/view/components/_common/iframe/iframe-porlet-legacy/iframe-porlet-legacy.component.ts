import { BehaviorSubject, Subject } from 'rxjs';

import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { ActivatedRoute, UrlSegment } from '@angular/router';

import { map, mergeMap, pluck, takeUntil, withLatestFrom } from 'rxjs/operators';

import { DotContentTypeService, DotIframeService, DotRouterService } from '@dotcms/data-access';
import { DotcmsEventsService, LoggerService, SiteService } from '@dotcms/dotcms-js';
import { UI_STORAGE_KEY } from '@dotcms/dotcms-models';
import { DotLoadingIndicatorService } from '@dotcms/utils';

import { DotCustomEventHandlerService } from '../../../../../api/services/dot-custom-event-handler/dot-custom-event-handler.service';
import { DotMenuService } from '../../../../../api/services/dot-menu.service';

@Component({
    selector: 'dot-iframe-porlet',
    styleUrls: ['./iframe-porlet-legacy.component.scss'],
    templateUrl: 'iframe-porlet-legacy.component.html',
    standalone: false
})
export class IframePortletLegacyComponent implements OnInit, OnDestroy {
    private contentletService = inject(DotContentTypeService);
    private dotLoadingIndicatorService = inject(DotLoadingIndicatorService);
    private dotMenuService = inject(DotMenuService);
    private dotRouterService = inject(DotRouterService);
    private route = inject(ActivatedRoute);
    private dotCustomEventHandlerService = inject(DotCustomEventHandlerService);
    loggerService = inject(LoggerService);
    siteService = inject(SiteService);
    private dotcmsEventsService = inject(DotcmsEventsService);
    private dotIframeService = inject(DotIframeService);

    canAccessPortlet: boolean;
    url: BehaviorSubject<string> = new BehaviorSubject('');
    isLoading = false;

    private destroy$: Subject<boolean> = new Subject<boolean>();

    ngOnInit(): void {
        this.dotRouterService.portletReload$.subscribe((portletId: string) => {
            if (this.dotRouterService.isJSPPortlet()) {
                this.reloadIframePortlet(portletId);
            }
        });
        /**
         *  skip first - to avoid subscription when page loads due login user subscription:
         *  https://github.com/dotCMS/core-web/blob/main/projects/dotcms-js/src/lib/core/site.service.ts#L58
         */
        this.siteService.switchSite$.pipe(takeUntil(this.destroy$)).subscribe(() => {
            if (this.url.getValue() !== '') {
                this.reloadIframePortlet();
            }
        });

        this.route.data
            .pipe(pluck('canAccessPortlet'), takeUntil(this.destroy$))
            .subscribe((canAccessPortlet: boolean) => {
                if (canAccessPortlet) {
                    this.setIframeSrc();
                }

                this.canAccessPortlet = canAccessPortlet;
            });

        this.subscribeToAIGeneration();

        // Workaroud to remove edit-content ui state
        this.#initContentEditSessionStorage();
    }
    #initContentEditSessionStorage() {
        sessionStorage.removeItem(UI_STORAGE_KEY);
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Handle the custom events emmited by the iframe
     *
     * @param CustomEvent $event
     * @memberof IframePortletLegacyComponent
     */
    onCustomEvent($event: CustomEvent): void {
        this.dotCustomEventHandlerService.handle($event);
    }

    /**
     * Tigger when the current site is changed, this method reload the iframe if is neccesary
     * @memberof IframePortletLegacyComponent
     */
    reloadIframePortlet(portletId?: string): void {
        this.dotLoadingIndicatorService.show();
        if (portletId) {
            this.dotMenuService.getUrlById(portletId).subscribe((url: string) => {
                this.setUrl(url);
            });
        } else {
            this.setUrl(this.url.getValue());
        }
    }

    private setIframeSrc(): void {
        // We use the query param to load a page in edit mode in the iframe
        const queryUrl$ = this.route.queryParams.pipe(
            pluck('url'),
            map((url: string) => url)
        );

        queryUrl$.subscribe((queryUrl: string) => {
            if (queryUrl) {
                this.setUrl(queryUrl);
            } else {
                this.setPortletUrl();
            }
        });
    }

    private setPortletUrl(): void {
        const portletId$ = this.route.params.pipe(
            pluck('id'),
            map((id: string) => id)
        );

        portletId$
            .pipe(
                withLatestFrom(
                    this.route.parent.url.pipe(
                        map((urlSegment: UrlSegment[]) => urlSegment[0].path)
                    )
                ),
                mergeMap(([id, url]) =>
                    url === 'add'
                        ? this.contentletService.getUrlById(id)
                        : this.dotMenuService.getUrlById(id)
                )
            )
            .subscribe((url: string) => {
                this.setUrl(url);
            });
    }

    /**
     * This function set isLoading to true, to remove the Legacy Iframe from the DOM while the src attribute is updated.
     * @param string nextUrl
     */
    private setUrl(nextUrl: string): void {
        this.dotLoadingIndicatorService.show();
        this.isLoading = true;
        this.url.next(nextUrl);
        // Need's this time to update the iFrame src.
        setTimeout(() => {
            this.isLoading = false;
        }, 0);
    }

    /**
       This function subscribes to the AI_CONTENT_PROMPT, that indicate that the
        backend has generated a new AI content, and the resutls need to be reloaded
        with the function refreshFakeJax defined in view_contentlets_js_inc.jsp.
     */
    private subscribeToAIGeneration(): void {
        this.dotcmsEventsService
            .subscribeTo('AI_CONTENT_PROMPT')
            .pipe(takeUntil(this.destroy$))
            .subscribe(() => {
                this.dotIframeService.run({ name: 'refreshFakeJax' });
            });
    }
}
