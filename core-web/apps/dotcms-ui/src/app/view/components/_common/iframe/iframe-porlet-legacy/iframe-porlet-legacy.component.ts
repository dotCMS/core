import { BehaviorSubject, Subject } from 'rxjs';

import { AsyncPipe } from '@angular/common';
import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterModule, UrlSegment } from '@angular/router';

import { map, mergeMap, takeUntil, withLatestFrom } from 'rxjs/operators';

import {
    DotContentTypeService,
    DotEventsSocket,
    DotIframeService,
    DotRouterService
} from '@dotcms/data-access';
import { LoggerService } from '@dotcms/dotcms-js';
import { UI_STORAGE_KEY } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';
import { DotNotLicenseComponent } from '@dotcms/ui';
import { DotLoadingIndicatorService } from '@dotcms/utils';

import { DotCustomEventHandlerService } from '../../../../../api/services/dot-custom-event-handler/dot-custom-event-handler.service';
import { DotMenuService } from '../../../../../api/services/dot-menu.service';
import { IframeComponent } from '../iframe-component/iframe.component';

@Component({
    selector: 'dot-iframe-porlet',
    styleUrls: ['./iframe-porlet-legacy.component.scss'],
    templateUrl: 'iframe-porlet-legacy.component.html',
    imports: [RouterModule, IframeComponent, DotNotLicenseComponent, AsyncPipe]
})
export class IframePortletLegacyComponent implements OnInit, OnDestroy {
    private contentletService = inject(DotContentTypeService);
    private dotLoadingIndicatorService = inject(DotLoadingIndicatorService);
    private dotMenuService = inject(DotMenuService);
    private dotRouterService = inject(DotRouterService);
    private route = inject(ActivatedRoute);
    private dotCustomEventHandlerService = inject(DotCustomEventHandlerService);
    loggerService = inject(LoggerService);
    readonly #globalStore = inject(GlobalStore);
    private dotEventsSocket = inject(DotEventsSocket);
    private dotIframeService = inject(DotIframeService);

    canAccessPortlet: boolean;
    url: BehaviorSubject<string> = new BehaviorSubject('');
    isLoading = signal(false);

    private destroy$: Subject<boolean> = new Subject<boolean>();

    ngOnInit(): void {
        this.dotRouterService.portletReload$.subscribe((portletId: string) => {
            if (this.dotRouterService.isJSPPortlet()) {
                this.reloadIframePortlet(portletId);
            }
        });
        this.#globalStore
            .switchSiteEvent$()
            .pipe(takeUntil(this.destroy$))
            .subscribe(() => {
                if (this.url.getValue() !== '') {
                    this.reloadIframePortlet();
                }
            });

        this.route.data
            .pipe(
                map((x) => x?.canAccessPortlet),
                takeUntil(this.destroy$)
            )
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
            map((x) => x?.url),
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
            map((x) => x?.id),
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
        this.isLoading.set(true);
        this.url.next(nextUrl);
        // Need's this time to update the iFrame src.
        setTimeout(() => {
            this.isLoading.set(false);
        }, 0);
    }

    /**
       This function subscribes to the AI_CONTENT_PROMPT, that indicate that the
        backend has generated a new AI content, and the resutls need to be reloaded
        with the function refreshFakeJax defined in view_contentlets_js_inc.jsp.
     */
    private subscribeToAIGeneration(): void {
        this.dotEventsSocket
            .on<void>('AI_CONTENT_PROMPT')
            .pipe(takeUntil(this.destroy$))
            .subscribe(() => {
                this.dotIframeService.run({ name: 'refreshFakeJax' });
            });
    }
}
