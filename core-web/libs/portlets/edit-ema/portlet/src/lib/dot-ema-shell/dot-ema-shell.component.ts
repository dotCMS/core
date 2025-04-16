import { EMPTY } from 'rxjs';

import { CommonModule, Location } from '@angular/common';
import { ChangeDetectorRef, Component, effect, inject, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, Params, Router, RouterModule } from '@angular/router';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogService } from 'primeng/dynamicdialog';
import { ToastModule } from 'primeng/toast';

import { catchError, map, skip, take, tap } from 'rxjs/operators';

import {
    DotESContentService,
    DotExperimentsService,
    DotFavoritePageService,
    DotLanguagesService,
    DotPageLayoutService,
    DotPageRenderService,
    DotSeoMetaTagsService,
    DotSeoMetaTagsUtilService,
    DotWorkflowsActionsService,
    DotAnalyticsTrackerService,
    DotMessageService
} from '@dotcms/data-access';
import { SiteService } from '@dotcms/dotcms-js';
import { DotPageToolsSeoComponent } from '@dotcms/portlets/dot-ema/ui';
import { DotInfoPageComponent, DotNotLicenseComponent } from '@dotcms/ui';
import { WINDOW } from '@dotcms/utils';
import { UVE_MODE } from '@dotcms/uve/types';

import { EditEmaNavigationBarComponent } from './components/edit-ema-navigation-bar/edit-ema-navigation-bar.component';

import { DotEmaDialogComponent } from '../components/dot-ema-dialog/dot-ema-dialog.component';
import { DotEditorDialogService } from '../components/dot-ema-dialog/services/dot-ema-dialog.service';
import { DotActionUrlService } from '../services/dot-action-url/dot-action-url.service';
import { DotPageApiService, UVEPageParams } from '../services/dot-page-api.service';
import { NG_CUSTOM_EVENTS, UVE_STATUS } from '../shared/enums';
import { DialogAction, DotPageAssetParams } from '../shared/models';
import { UVEStore } from '../store/dot-uve.store';
import { DotUveViewParams } from '../store/models';
import {
    checkClientHostAccess,
    getAllowedPageParams,
    getTargetUrl,
    insertContentletInContainer,
    normalizeQueryParams,
    sanitizeURL,
    shouldNavigate
} from '../utils';

@Component({
    selector: 'dot-ema-shell',
    standalone: true,
    providers: [
        UVEStore,
        DotPageApiService,
        DotActionUrlService,
        DotLanguagesService,
        MessageService,
        DotPageLayoutService,
        ConfirmationService,
        DotFavoritePageService,
        DotESContentService,
        DialogService,
        DotPageRenderService,
        DotSeoMetaTagsService,
        DotSeoMetaTagsUtilService,
        DotWorkflowsActionsService,
        {
            provide: WINDOW,
            useValue: window
        },
        DotExperimentsService,
        DotAnalyticsTrackerService
    ],
    templateUrl: './dot-ema-shell.component.html',
    styleUrls: ['./dot-ema-shell.component.scss'],
    imports: [
        CommonModule,
        ConfirmDialogModule,
        ToastModule,
        EditEmaNavigationBarComponent,
        RouterModule,
        DotPageToolsSeoComponent,
        DotEmaDialogComponent,
        DotInfoPageComponent,
        DotNotLicenseComponent
    ]
})
export class DotEmaShellComponent implements OnInit {
    @ViewChild('pageTools') pageTools!: DotPageToolsSeoComponent;

    readonly uveStore = inject(UVEStore);

    readonly #activatedRoute = inject(ActivatedRoute);
    readonly #router = inject(Router);
    readonly #siteService = inject(SiteService);
    readonly #location = inject(Location);
    readonly #dialogService = inject(DotEditorDialogService);
    readonly #messageService = inject(MessageService);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #dotPageApiService = inject(DotPageApiService);
    readonly #cd = inject(ChangeDetectorRef);

    protected readonly $shellProps = this.uveStore.$shellProps;

    /**
     * Handle the update of the page params
     * When the page params change, we update the location
     *
     * @memberof DotEmaShellComponent
     */
    readonly $updateQueryParamsEffect = effect(() => {
        const params = this.uveStore.$friendlyParams();

        const { data } = this.#activatedRoute.snapshot;

        const baseClientHost = data?.uveConfig?.url;

        const cleanedParams = normalizeQueryParams(params, baseClientHost);

        this.#updateLocation(cleanedParams);
    });

    ngOnInit(): void {
        const params = this.#getPageParams();
        const viewParams = this.#getViewParams(params.mode);

        this.uveStore.patchViewParams(viewParams);

        this.uveStore.loadPageAsset({ url: params.url, params: params as UVEPageParams });

        // We need to skip one because it's the initial value
        this.#siteService.switchSite$
            .pipe(skip(1))
            .subscribe(() => this.#router.navigate(['/pages']));
    }

    /**
     * Handle actions from nav bar
     *
     * @param {string} itemId
     * @memberof DotEmaShellComponent
     */
    handleItemAction(itemId: string) {
        if (itemId === 'page-tools') {
            this.pageTools.toggleDialog();
        } else if (itemId === 'properties') {
            const page = this.uveStore.pageAPIResponse().page;

            this.#dialogService.editContentlet({
                inode: page.inode,
                title: page.title,
                identifier: page.identifier,
                contentType: page.contentType,
                angularCurrentPortlet: 'edit-page'
            });
        }
    }

    /**
     * Reloads the component from the dialog.
     */
    reloadFromDialog() {
        this.uveStore.reloadCurrentPage();
    }

    /**
     * Get the query params from the Router
     *
     * @return {*}  {DotPageApiParams}
     * @memberof DotEmaShellComponent
     */
    #getPageParams(): DotPageAssetParams {
        const { queryParams, data } = this.#activatedRoute.snapshot;
        const uveConfig = data?.uveConfig;
        const allowedDevURLs = uveConfig?.options?.allowedDevURLs;

        // Clone queryParams to avoid mutation errors
        const params = getAllowedPageParams(queryParams);
        const validHost = checkClientHostAccess(params.clientHost, allowedDevURLs);

        //Sanitize the url
        params.url = sanitizeURL(params.url);

        if (!validHost) {
            delete params.clientHost;
        }

        if (uveConfig?.url && !validHost) {
            params.clientHost = uveConfig.url;
        }

        // If the editor mode is not valid, set it to edit mode
        const UVE_MODES = Object.values(UVE_MODE);

        if (!params.mode || !UVE_MODES.includes(params.mode)) {
            params.mode = UVE_MODE.EDIT;
        }

        if (params.mode !== UVE_MODE.LIVE && params.publishDate) {
            delete params?.['publishDate'];
        }

        if (queryParams['personaId']) {
            params['com.dotmarketing.persona.id'] = queryParams['personaId'];
        }

        return params;
    }

    #getViewParams(uveMode: UVE_MODE): DotUveViewParams {
        const { queryParams } = this.#activatedRoute.snapshot;

        const isPreviewMode = uveMode === UVE_MODE.PREVIEW || uveMode === UVE_MODE.LIVE;

        const viewParams: DotUveViewParams = {
            device: queryParams.device,
            orientation: queryParams.orientation,
            seo: queryParams.seo
        };

        return isPreviewMode
            ? viewParams
            : { device: undefined, orientation: undefined, seo: undefined };
    }

    /**
     * Update the location with the new query params
     *
     * Note: This method does not trigger a navigation event
     *
     * @param {Params} queryParams
     * @memberof DotEmaShellComponent
     */
    #updateLocation(queryParams: Params = {}): void {
        const urlTree = this.#router.createUrlTree([], { queryParams });
        this.#location.go(urlTree.toString());
    }

    protected handleNgEvent({ event, actionPayload }: DialogAction) {
        const { detail } = event;

        switch (event.detail.name) {
            case NG_CUSTOM_EVENTS.UPDATE_WORKFLOW_ACTION: {
                const pageInode = this.uveStore.pageAPIResponse().page.inode;
                this.uveStore.getWorkflowActions(pageInode);
                break;
            }

            case NG_CUSTOM_EVENTS.CONTENT_SEARCH_SELECT: {
                const { pageContainers, didInsert } = insertContentletInContainer({
                    ...actionPayload,
                    newContentletId: detail.data.identifier
                });

                if (!didInsert) {
                    this.handleDuplicatedContentlet();

                    return;
                }

                this.uveStore.savePage(pageContainers);
                break;
            }

            case NG_CUSTOM_EVENTS.CREATE_CONTENTLET: {
                this.#dialogService.createContentlet({
                    contentType: detail.data.contentType,
                    url: detail.data.url,
                    actionPayload
                });

                this.#cd.detectChanges();
                break;
            }

            case NG_CUSTOM_EVENTS.CANCEL_SAVING_MENU_ORDER: {
                this.#dialogService.resetDialog();
                this.#cd.detectChanges();
                break;
            }

            case NG_CUSTOM_EVENTS.SAVE_MENU_ORDER: {
                this.#messageService.add({
                    severity: 'success',
                    summary: this.#dotMessageService.get(
                        'editpage.content.contentlet.menu.reorder.title'
                    ),
                    detail: this.#dotMessageService.get('message.menu.reordered'),
                    life: 2000
                });

                this.uveStore.reloadCurrentPage();
                this.#dialogService.resetDialog();
                break;
            }

            case NG_CUSTOM_EVENTS.LANGUAGE_IS_CHANGED: {
                const htmlPageReferer = event.detail.payload?.htmlPageReferer;
                const url = new URL(htmlPageReferer, window.location.origin); // Add base for relative URLs
                const targetUrl = getTargetUrl(
                    url.pathname,
                    this.uveStore.pageAPIResponse().urlContentMap
                );
                const languageId = url.searchParams.get('com.dotmarketing.htmlpage.language');
                const currentURL = this.uveStore.pageAPIResponse().page.pageURI;

                if (shouldNavigate(targetUrl, currentURL)) {
                    // Navigate to the new URL if it's different from the current one
                    this.uveStore.loadPageAsset({ url: targetUrl, params: { languageId } });

                    return;
                }

                this.uveStore.reloadCurrentPage({
                    params: {
                        languageId
                    }
                });

                break;
            }

            case NG_CUSTOM_EVENTS.ERROR_SAVING_MENU_ORDER: {
                this.#messageService.add({
                    severity: 'error',
                    summary: this.#dotMessageService.get(
                        'editpage.content.contentlet.menu.reorder.title'
                    ),
                    detail: this.#dotMessageService.get(
                        'error.menu.reorder.user_has_not_permission'
                    ),
                    life: 2000
                });

                break;
            }

            case NG_CUSTOM_EVENTS.FORM_SELECTED: {
                const formId = detail.data.identifier;

                this.#dotPageApiService
                    .getFormIndetifier(actionPayload.container.identifier, formId)
                    .pipe(
                        tap(() => {
                            this.uveStore.setUveStatus(UVE_STATUS.LOADING);
                        }),
                        map((newFormId: string) => {
                            return {
                                ...actionPayload,
                                newContentletId: newFormId
                            };
                        }),
                        catchError(() => EMPTY),
                        take(1)
                    )
                    .subscribe((response) => {
                        const { pageContainers, didInsert } = insertContentletInContainer(response);

                        if (!didInsert) {
                            this.handleDuplicatedContentlet();
                            this.uveStore.setUveStatus(UVE_STATUS.LOADED);
                        } else {
                            this.uveStore.savePage(pageContainers);
                        }
                    });

                break;
            }

            case NG_CUSTOM_EVENTS.SAVE_PAGE: {
                const { shouldReloadPage, contentletIdentifier } = detail.payload ?? {};
                const pageIdentifier = this.uveStore.pageAPIResponse().page.identifier;

                // Add this: reloadURLContentMapPage
                // if (shouldReloadPage) {
                //     this.reloadURLContentMapPage(contentletIdentifier);
                //     return;
                // }

                if (contentletIdentifier === pageIdentifier || shouldReloadPage || !actionPayload) {
                    this.handleReloadPage(event);

                    return;
                }

                this.handleContentSave(event, actionPayload);

                break;
            }
        }
    }

    /**
     * Handles the save page event triggered from the dialog.
     *
     * @param {CustomEvent} event - The event object containing details about the save action.
     * @return {void}
     */
    private handleReloadPage(event: CustomEvent): void {
        // Move this to the GetTargetUrl function
        const htmlPageReferer = event.detail.payload?.htmlPageReferer;
        const url = new URL(htmlPageReferer, window.location.origin);
        const targetUrl = getTargetUrl(url.pathname, this.uveStore.pageAPIResponse().urlContentMap);
        const currentURL = this.uveStore.pageAPIResponse()?.page.pageURI;
        // END

        if (shouldNavigate(targetUrl, currentURL)) {
            // Navigate to the new URL if it's different from the current one
            this.uveStore.loadPageAsset({ url: targetUrl });

            return;
        }

        this.uveStore.reloadCurrentPage();
    }

    private handleContentSave(event: CustomEvent, actionPayload) {
        const newContentletId = event.detail.payload?.newContentletId ?? '';

        const { pageContainers, didInsert } = insertContentletInContainer({
            ...actionPayload,
            newContentletId
        });

        if (!didInsert) {
            this.handleDuplicatedContentlet();

            return;
        }

        this.uveStore.savePage(pageContainers);
    }

    private handleDuplicatedContentlet() {
        this.#messageService.add({
            severity: 'info',
            summary: this.#dotMessageService.get('editpage.content.add.already.title'),
            detail: this.#dotMessageService.get('editpage.content.add.already.message'),
            life: 2000
        });

        this.uveStore.resetEditorProperties();
        this.#dialogService.resetDialog();
    }
}
