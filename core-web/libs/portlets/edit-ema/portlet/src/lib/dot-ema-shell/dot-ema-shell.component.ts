import { CommonModule, Location } from '@angular/common';
import { Component, effect, inject, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, Params, Router, RouterModule } from '@angular/router';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogService } from 'primeng/dynamicdialog';
import { ToastModule } from 'primeng/toast';

import { skip } from 'rxjs/operators';

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
    DotAnalyticsTrackerService
} from '@dotcms/data-access';
import { SiteService } from '@dotcms/dotcms-js';
import { DotPageToolsSeoComponent } from '@dotcms/portlets/dot-ema/ui';
import { UVE_MODE } from '@dotcms/types';
import { DotInfoPageComponent, DotNotLicenseComponent } from '@dotcms/ui';
import { WINDOW } from '@dotcms/utils';

import { EditEmaNavigationBarComponent } from './components/edit-ema-navigation-bar/edit-ema-navigation-bar.component';

import { DotEmaDialogComponent } from '../components/dot-ema-dialog/dot-ema-dialog.component';
import { DotActionUrlService } from '../services/dot-action-url/dot-action-url.service';
import { DotPageApiService } from '../services/dot-page-api.service';
import { PERSONA_KEY } from '../shared/consts';
import { NG_CUSTOM_EVENTS } from '../shared/enums';
import { DialogAction, DotPageAssetParams } from '../shared/models';
import { UVEStore } from '../store/dot-uve.store';
import { DotUveViewParams } from '../store/models';
import {
    checkClientHostAccess,
    getTargetUrl,
    normalizeQueryParams,
    sanitizeURL,
    shouldNavigate
} from '../utils';
@Component({
    selector: 'dot-ema-shell',
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
    @ViewChild('dialog') dialog!: DotEmaDialogComponent;
    @ViewChild('pageTools') pageTools!: DotPageToolsSeoComponent;

    readonly uveStore = inject(UVEStore);

    readonly #activatedRoute = inject(ActivatedRoute);
    readonly #router = inject(Router);
    readonly #siteService = inject(SiteService);
    readonly #location = inject(Location);

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

        this.uveStore.loadPageAsset(params);

        // We need to skip one because it's the initial value
        this.#siteService.switchSite$
            .pipe(skip(1))
            .subscribe(() => this.#router.navigate(['/pages']));
    }

    handleNgEvent({ event }: DialogAction) {
        switch (event.detail.name) {
            case NG_CUSTOM_EVENTS.UPDATE_WORKFLOW_ACTION: {
                const pageAPIResponse = this.uveStore.pageAPIResponse();
                this.uveStore.getWorkflowActions(pageAPIResponse.page.inode);
                break;
            }

            case NG_CUSTOM_EVENTS.SAVE_PAGE: {
                this.handleSavePageEvent(event);
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
    private handleSavePageEvent(event: CustomEvent): void {
        const htmlPageReferer = event.detail.payload?.htmlPageReferer;
        const url = new URL(htmlPageReferer, window.location.origin); // Add base for relative URLs
        const targetUrl = getTargetUrl(url.pathname, this.uveStore.pageAPIResponse().urlContentMap);

        if (shouldNavigate(targetUrl, this.uveStore.pageParams().url)) {
            // Navigate to the new URL if it's different from the current one
            this.uveStore.loadPageAsset({ url: targetUrl });

            return;
        }

        this.uveStore.reloadCurrentPage();
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

            this.dialog.editContentlet({
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
        const params = { ...queryParams };
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
            params[PERSONA_KEY] = queryParams['personaId'];
            delete params['personaId'];
        }

        return params as DotPageAssetParams;
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
}
