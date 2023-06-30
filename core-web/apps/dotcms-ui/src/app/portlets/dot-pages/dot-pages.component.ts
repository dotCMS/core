import { Subject, Subscription } from 'rxjs';

import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import {
    AfterViewInit,
    Component,
    ElementRef,
    HostListener,
    OnDestroy,
    ViewChild
} from '@angular/core';

import { Menu } from 'primeng/menu';

import { Observable } from 'rxjs/internal/Observable';
import { filter, take, takeUntil } from 'rxjs/operators';

import { DotCreateContentletComponent } from '@components/dot-contentlet-editor/components/dot-create-contentlet/dot-create-contentlet.component';
import { DotMessageSeverity, DotMessageType } from '@components/dot-message-display/model';
import { DotMessageDisplayService } from '@components/dot-message-display/services';
import { DotHttpErrorManagerService } from '@dotcms/app/api/services/dot-http-error-manager/dot-http-error-manager.service';
import { DotRouterService } from '@dotcms/app/api/services/dot-router/dot-router.service';
import { DotEventsService, DotPageRenderService } from '@dotcms/data-access';
import { HttpCode } from '@dotcms/dotcms-js';
import { ComponentStatus, DotCMSContentlet } from '@dotcms/dotcms-models';

import {
    DotPagesState,
    DotPageStore,
    FAVORITE_PAGE_LIMIT
} from './dot-pages-store/dot-pages.store';

export interface DotActionsMenuEventParams {
    event: MouseEvent;
    actionMenuDomId: string;
    item: DotCMSContentlet;
}

@Component({
    providers: [DotPageStore],
    selector: 'dot-pages',
    styleUrls: ['./dot-pages.component.scss'],
    templateUrl: './dot-pages.component.html'
})
export class DotPagesComponent implements AfterViewInit, OnDestroy {
    @ViewChild('menu') menu: Menu;
    vm$: Observable<DotPagesState> = this.store.vm$;

    private domIdMenuAttached = '';
    private destroy$: Subject<boolean> = new Subject<boolean>();
    private contentletDialogShutdown: Subscription;

    constructor(
        private store: DotPageStore,
        private dotRouterService: DotRouterService,
        private dotMessageDisplayService: DotMessageDisplayService,
        private dotEventsService: DotEventsService,
        private dotHttpErrorManagerService: DotHttpErrorManagerService,
        private dotPageRenderService: DotPageRenderService,
        private element: ElementRef
    ) {
        this.store.setInitialStateData(FAVORITE_PAGE_LIMIT);
    }

    /**
     * Event to redirect to Edit Page when Page selected
     *
     * @param {string} url
     * @memberof DotPagesComponent
     */
    goToUrl(url: string): void {
        this.store.setPortletStatus(ComponentStatus.LOADING);

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
                        this.store.setPortletStatus(ComponentStatus.LOADED);
                    }
                },
                (error: HttpErrorResponse) => {
                    this.dotHttpErrorManagerService.handle(error);
                    this.store.setPortletStatus(ComponentStatus.LOADED);
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
            this.store.clearMenuActions();
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
        this.store.clearMenuActions();
        this.menu.hide();

        this.store.showActionsMenu({ item, actionMenuDomId });
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
        this.store.actionMenuDomId$
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

                this.store.updateSinglePageData({ identifier, isFavoritePage });

                this.dotMessageDisplayService.push({
                    life: 3000,
                    message: evt.data['value'],
                    severity: DotMessageSeverity.SUCCESS,
                    type: DotMessageType.SIMPLE_MESSAGE
                });
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
     * Subscribe to the shutdown event of the contentlet dialog
     *
     * @param {*} componentRef
     * @return {*}
     * @memberof DotPagesComponent
     */
    subscribeToShutdown(componentRef: Component): void {
        if (!(componentRef instanceof DotCreateContentletComponent)) return;

        this.contentletDialogShutdown = componentRef.shutdown.subscribe(() => {
            this.store.getPages({
                offset: 0
            });
        });
    }

    /**
     * Unsubscribe to the shutdown event of the contentlet dialog
     *
     * @memberof DotPagesComponent
     */
    unsubscribeToShutdown() {
        if (this.contentletDialogShutdown) this.contentletDialogShutdown.unsubscribe();
    }
}
