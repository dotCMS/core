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
import { DotInfoPageComponent, DotNotLicenseComponent } from '@dotcms/ui';
import { WINDOW } from '@dotcms/utils';
import { UVE_MODE } from '@dotcms/uve/types';

import { EditEmaNavigationBarComponent } from './components/edit-ema-navigation-bar/edit-ema-navigation-bar.component';

import { DotEmaDialogComponent } from '../components/dot-ema-dialog/dot-ema-dialog.component';
import { DotEditorDialogService } from '../components/dot-ema-dialog/services/dot-ema-dialog.service';
import { DotActionUrlService } from '../services/dot-action-url/dot-action-url.service';
import { DotUVENgEvenHandlerService } from '../services/dot-ng-event-handler/dot-ng-event-handler.service';
import { DotPageApiService } from '../services/dot-page-api.service';
import { DialogAction, DotPageAssetParams } from '../shared/models';
import { UVEStore } from '../store/dot-uve.store';
import { DotUveViewParams } from '../store/models';
import {
    checkClientHostAccess,
    getAllowedPageParams,
    normalizeQueryParams,
    sanitizeURL
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
        DotUVENgEvenHandlerService,
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

    readonly #router = inject(Router);
    readonly #location = inject(Location);
    readonly #siteService = inject(SiteService);
    readonly #activatedRoute = inject(ActivatedRoute);
    readonly #dialogService = inject(DotEditorDialogService);
    readonly #dotNgEventHandlerService = inject(DotUVENgEvenHandlerService);

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

    /**
     * Handle the event triggered from the dialog.
     *
     * @param {DialogAction} event - The event object containing details about the action.
     * @memberof DotEmaShellComponent
     */
    protected handleNgEvent(event: DialogAction) {
        this.#dotNgEventHandlerService.handleNgEvent(event);
    }
}
