import { Subject } from 'rxjs';

import { CommonModule } from '@angular/common';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import {
    AfterViewInit,
    Component,
    ElementRef,
    HostListener,
    inject,
    OnDestroy,
    ViewChild
} from '@angular/core';
import { RouterModule } from '@angular/router';

import { DialogService } from 'primeng/dynamicdialog';
import { Menu, MenuModule } from 'primeng/menu';
import { ProgressSpinnerModule } from 'primeng/progressspinner';

import { Observable } from 'rxjs/internal/Observable';
import { filter, take, takeUntil } from 'rxjs/operators';

import {
    DotESContentService,
    DotEventsService,
    DotFavoritePageService,
    DotHttpErrorManagerService,
    DotMessageDisplayService,
    DotPageRenderService,
    DotPageTypesService,
    DotPageWorkflowsActionsService,
    DotRouterService,
    DotTempFileUploadService,
    DotWorkflowActionsFireService,
    DotWorkflowEventHandlerService,
    DotWorkflowsActionsService
} from '@dotcms/data-access';
import { HttpCode, SiteService } from '@dotcms/dotcms-js';
import {
    ComponentStatus,
    DotCMSContentlet,
    DotMessageSeverity,
    DotMessageType
} from '@dotcms/dotcms-models';
import { DotAddToBundleComponent } from '@dotcms/ui';

import { DotPagesFavoritePanelComponent } from './dot-pages-favorite-panel/dot-pages-favorite-panel.component';
import { DotPagesListingPanelComponent } from './dot-pages-listing-panel/dot-pages-listing-panel.component';
import {
    DotPagesState,
    DotPageStore,
    FAVORITE_PAGE_LIMIT
} from './dot-pages-store/dot-pages.store';
import { DotCMSPagesStore } from './store/store';

export interface DotActionsMenuEventParams {
    event: MouseEvent;
    actionMenuDomId: string;
    item: DotCMSContentlet;
}

@Component({
    providers: [
        DotPageStore,
        DialogService,
        DotESContentService,
        DotPageRenderService,
        DotPageTypesService,
        DotTempFileUploadService,
        DotWorkflowsActionsService,
        DotPageWorkflowsActionsService,
        DotWorkflowActionsFireService,
        DotWorkflowEventHandlerService,
        DotRouterService,
        DotFavoritePageService,
        DotCMSPagesStore
    ],
    selector: 'dot-pages',
    styleUrls: ['./dot-pages.component.scss'],
    templateUrl: './dot-pages.component.html',
    imports: [
        CommonModule,
        RouterModule,
        DotAddToBundleComponent,
        DotPagesFavoritePanelComponent,
        DotPagesListingPanelComponent,
        MenuModule,
        ProgressSpinnerModule
    ]
})
export class DotPagesComponent implements AfterViewInit, OnDestroy {
    private dotRouterService = inject(DotRouterService);
    private dotMessageDisplayService = inject(DotMessageDisplayService);
    private dotEventsService = inject(DotEventsService);
    private dotHttpErrorManagerService = inject(DotHttpErrorManagerService);
    private dotPageRenderService = inject(DotPageRenderService);
    private element = inject(ElementRef);
    private dotSiteService = inject(SiteService);

    readonly #store = inject(DotPageStore);
    readonly #dotCMSPagesStore = inject(DotCMSPagesStore);

    @ViewChild('menu') menu: Menu;
    vm$: Observable<DotPagesState> = this.#store.vm$;

    private domIdMenuAttached = '';
    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor() {
        this.#store.setInitialStateData(FAVORITE_PAGE_LIMIT);
        this.#dotCMSPagesStore.getPages();
    }

    /**
     * Event to redirect to Edit Page when Page selected
     *
     * @param {string} url
     * @memberof DotPagesComponent
     */
    goToUrl(url: string): void {
        this.#store.setPortletStatus(ComponentStatus.LOADING);

        const splittedUrl = url.split('?');
        const urlParams = { url: splittedUrl[0] };
        const searchParams = new URLSearchParams(splittedUrl[1]);

        for (const entry of searchParams) {
            urlParams[entry[0]] = entry[1];
        }

        this.dotPageRenderService
            .checkPermission(urlParams)
            .pipe(take(1))
            .subscribe(
                (hasPermission: boolean) => {
                    if (hasPermission) {
                        this.dotRouterService.goToEditPage(urlParams);
                    } else {
                        const error = new HttpErrorResponse(
                            new HttpResponse({
                                body: null,
                                status: HttpCode.FORBIDDEN,
                                headers: null,
                                url: ''
                            })
                        );
                        this.dotHttpErrorManagerService.handle(error);
                        this.#store.setPortletStatus(ComponentStatus.LOADED);
                    }
                },
                (error: HttpErrorResponse) => {
                    this.dotHttpErrorManagerService.handle(error);
                    this.#store.setPortletStatus(ComponentStatus.LOADED);
                }
            );
    }

    /**
     * Closes the menu when the user clicks outside of it
     *
     * @memberof DotPagesComponent
     */
    @HostListener('window:click')
    closeMenu(): void {
        if (this.menuIsLoaded(this.domIdMenuAttached)) {
            this.menu.hide();
            this.#store.clearMenuActions();
        }
    }

    /**
     * Event to show/hide actions menu when each contentlet is clicked
     *
     * @param {DotActionsMenuEventParams} params
     * @memberof DotPagesComponent
     */
    showActionsMenu({ event, actionMenuDomId, item }: DotActionsMenuEventParams): void {
        event.stopPropagation();
        this.#store.clearMenuActions();
        this.menu.hide();

        this.#store.showActionsMenu({ item, actionMenuDomId });
    }

    /**
     * Event to reset status of menu actions when closed
     *
     * @memberof DotPagesComponent
     */
    closedActionsMenu() {
        this.domIdMenuAttached = '';
    }

    ngAfterViewInit(): void {
        this.#store.actionMenuDomId$
            .pipe(
                takeUntil(this.destroy$),
                filter((actionMenuDomId) => !!actionMenuDomId)
            )
            .subscribe((actionMenuDomId: string) => {
                const target = this.element.nativeElement.querySelector(`#${actionMenuDomId}`);
                if (target && this.menuIsLoaded(actionMenuDomId)) {
                    this.menu.show({ currentTarget: target });
                    this.domIdMenuAttached = actionMenuDomId;

                    // To hide when the contextMenu is opened
                } else this.menu.hide();
            });

        this.dotEventsService
            .listen('save-page')
            .pipe(takeUntil(this.destroy$))
            .subscribe((evt) => {
                const identifier =
                    evt.data['payload']?.identifier || evt.data['payload']?.contentletIdentifier;

                const isFavoritePage =
                    evt.data['payload']?.contentType === 'dotFavoritePage' ||
                    evt.data['payload']?.contentletType === 'dotFavoritePage';

                this.#store.updateSinglePageData({ identifier, isFavoritePage });

                this.dotMessageDisplayService.push({
                    life: 3000,
                    message: evt.data['value'],
                    severity: DotMessageSeverity.SUCCESS,
                    type: DotMessageType.SIMPLE_MESSAGE
                });
            });

        this.dotSiteService.switchSite$.pipe(takeUntil(this.destroy$)).subscribe(() => {
            this.#store.getPages({ offset: 0 });
            this.scrollToTop(); // To reset the scroll so it shows the data it retrieves
        });
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Check if the menu is loaded
     *
     * @private
     * @param {string} menuDOMID
     * @return {*}  {boolean}
     * @memberof DotPagesComponent
     */
    private menuIsLoaded(menuDOMID: string): boolean {
        return (
            menuDOMID.includes('pageActionButton') || menuDOMID.includes('favoritePageActionButton')
        );
    }

    /**
     * Load pages on deactivation
     *
     * @memberof DotPagesComponent
     */
    loadPagesOnDeactivation() {
        this.#store.getPages({
            offset: 0
        });
    }

    /**
     * Scroll to top of the page
     *
     * @memberof DotPagesComponent
     */
    scrollToTop(): void {
        this.element.nativeElement?.scroll({
            top: 0,
            left: 0
        });
    }
}
