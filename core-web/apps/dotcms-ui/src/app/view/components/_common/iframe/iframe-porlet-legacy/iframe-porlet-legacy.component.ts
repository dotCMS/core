import { BehaviorSubject, Subject } from 'rxjs';

import { AsyncPipe } from '@angular/common';
import {
    Component,
    Injector,
    OnDestroy,
    OnInit,
    inject,
    signal,
    ChangeDetectionStrategy
} from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterModule, UrlSegment } from '@angular/router';

import {
    distinctUntilChanged,
    filter,
    map,
    mergeMap,
    skip,
    takeUntil,
    withLatestFrom
} from 'rxjs/operators';

import {
    DotContentChangeService,
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
import { refusePanelUrl } from '../../../../../api/util/open-edit-content-panel';
import { PORTLET_FOLDER_PARAM, portletPanelUrl$ } from '../../../../../api/util/portlet-panel-url';
import { IframeComponent } from '../iframe-component/iframe.component';

@Component({
    selector: 'dot-iframe-porlet',
    styleUrls: ['./iframe-porlet-legacy.component.scss'],
    templateUrl: 'iframe-porlet-legacy.component.html',
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [RouterModule, IframeComponent, DotNotLicenseComponent, AsyncPipe]
})
export class IframePortletLegacyComponent implements OnInit, OnDestroy {
    private contentletService = inject(DotContentTypeService);
    private dotLoadingIndicatorService = inject(DotLoadingIndicatorService);
    private dotMenuService = inject(DotMenuService);
    private dotRouterService = inject(DotRouterService);
    private route = inject(ActivatedRoute);
    readonly #router = inject(Router);
    private dotCustomEventHandlerService = inject(DotCustomEventHandlerService);
    loggerService = inject(LoggerService);
    readonly #globalStore = inject(GlobalStore);
    readonly #siteChange$ = toObservable(this.#globalStore.siteDetails);
    private dotEventsSocket = inject(DotEventsSocket);
    private dotIframeService = inject(DotIframeService);
    readonly #contentChanges = inject(DotContentChangeService);
    readonly #injector = inject(Injector);

    /** Whether the panel params in the URL are being followed; from the first access granted on. */
    #followingPanelUrl = false;

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
        this.#siteChange$
            .pipe(
                skip(1),
                filter(Boolean),
                distinctUntilChanged((a, b) => a.identifier === b.identifier),
                takeUntil(this.destroy$)
            )
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
                    this.#followPanelUrl();
                }

                this.canAccessPortlet = canAccessPortlet;
            });

        this.subscribeToAIGeneration();
        this.#reloadOnContentChange();

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

    /**
     * Reloads the portlet's list on every save and delete in a side panel, at any depth, for as long
     * as the portlet is shown (#38303, T174): content saved from a relationship inside the panel a
     * row opened is in the list too. The portlet is the current one when the change arrives, as
     * when the panel's close reloads it (`DotCustomEventHandlerService.openContent`).
     */
    #reloadOnContentChange(): void {
        this.#contentChanges.edits$.pipe(takeUntil(this.destroy$)).subscribe(() => {
            const portletId = this.dotRouterService.currentPortlet.id;

            if (portletId) {
                this.dotIframeService.reloadData(portletId);
            }
        });
    }

    /**
     * Opens what the portlet's URL names in the side panel (#38303, FR-049), on load and on every
     * navigation of the app's own that changes it: a deep link redirected here, a pasted link. The
     * panel opens through the same path as the portlet's own events, so it is written to the URL and
     * reloads the portlet the same way. `Location` writes, which is how the panel URL service
     * writes, never arrive here.
     *
     * A navigation the browser's Back or Forward started opens nothing: those belong to the panel
     * URL service (FR-048), and the router replays an entry that may name a panel still open. A URL
     * whose panel did not change opens nothing again, since an app navigation that changes other
     * params carries the panel params the router last saw. A link the author cannot have meant (a
     * panel param of the wrong shape, or two panels) opens nothing, shows the standard error, and
     * loses its params with the create's folder. While a Workflow task's detail is shown, the detail
     * opens it instead ({@link #showsTask}).
     */
    #followPanelUrl(): void {
        if (this.#followingPanelUrl) {
            return;
        }

        this.#followingPanelUrl = true;
        portletPanelUrl$(this.route, this.#router, () => !this.#showsTask())
            .pipe(takeUntil(this.destroy$))
            .subscribe(({ request, refused }) => {
                if (request) {
                    this.dotCustomEventHandlerService.openContent(request);
                } else if (refused) {
                    void refusePanelUrl(this.#injector, [PORTLET_FOLDER_PARAM]);
                }
            });
    }

    /**
     * Whether a Workflow task's detail is shown (`/c/workflow/<task>`). It opens what the URL names
     * itself, once its dialog has shown, so the panel always lands above the dialog (#38303).
     */
    #showsTask(): boolean {
        return (
            this.route.snapshot?.params?.['id'] === 'workflow' &&
            !!this.route.firstChild?.snapshot?.params?.['asset']
        );
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
