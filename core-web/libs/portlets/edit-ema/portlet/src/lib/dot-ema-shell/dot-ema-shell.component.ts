import { Location } from '@angular/common';
import { Component, computed, DestroyRef, effect, inject, OnInit, signal, ViewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Params, Router, RouterModule } from '@angular/router';

import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { MessagesModule } from 'primeng/messages';
import { ToastModule } from 'primeng/toast';

import { SiteService } from '@dotcms/dotcms-js';
import { DotPageToolUrlParams } from '@dotcms/dotcms-models';
import { DotPageToolsSeoComponent } from '@dotcms/portlets/dot-ema/ui';
import { GlobalStore } from '@dotcms/store';
import { UVE_MODE } from '@dotcms/types';
import { DotInfoPageComponent, DotMessagePipe, DotNotLicenseComponent, InfoPage } from '@dotcms/ui';

import { EditEmaNavigationBarComponent } from './components/edit-ema-navigation-bar/edit-ema-navigation-bar.component';

import { DotEmaDialogComponent } from '../components/dot-ema-dialog/dot-ema-dialog.component';
import { PERSONA_KEY } from '../shared/consts';
import { NG_CUSTOM_EVENTS, UVE_STATUS } from '../shared/enums';
import { DialogAction, DotPageAssetParams, NavigationBarItem } from '../shared/models';
import { UVEStore } from '../store/dot-uve.store';
import { DotUveViewParams } from '../store/models';
import {
    checkClientHostAccess,
    getErrorPayload,
    getRequestHostName,
    getTargetUrl,
    normalizeQueryParams,
    sanitizeURL,
    shouldNavigate
} from '../utils';

@Component({
    selector: 'dot-ema-shell',
    templateUrl: './dot-ema-shell.component.html',
    styleUrls: ['./dot-ema-shell.component.scss'],
    imports: [
        ConfirmDialogModule,
        ToastModule,
        EditEmaNavigationBarComponent,
        RouterModule,
        DotPageToolsSeoComponent,
        DotEmaDialogComponent,
        DotInfoPageComponent,
        DotNotLicenseComponent,
        MessagesModule,
        DotMessagePipe
    ]
})
export class DotEmaShellComponent implements OnInit {
    @ViewChild('dialog') dialog!: DotEmaDialogComponent;
    @ViewChild('pageTools') pageTools!: DotPageToolsSeoComponent;

    readonly uveStore = inject(UVEStore);
    readonly destroyRef = inject(DestroyRef);
    readonly #activatedRoute = inject(ActivatedRoute);
    readonly #router = inject(Router);
    readonly #siteService = inject(SiteService);
    readonly #location = inject(Location);
    readonly #globalStore = inject(GlobalStore);
    protected readonly $toggleLockOptions = this.uveStore.$toggleLockOptions;

    protected readonly $showBanner = signal<boolean>(true);

    // Component builds its own menu items (Phase 2.1: Move view models from store to components)
    protected readonly $menuItems = computed<NavigationBarItem[]>(() => {
        const pageAPIResponse = this.uveStore.pageAPIResponse();
        const page = pageAPIResponse?.page;
        const template = pageAPIResponse?.template;
        const isLoading = this.uveStore.status() === UVE_STATUS.LOADING;
        const isEnterpriseLicense = this.uveStore.isEnterprise();
        const templateDrawed = template?.drawed;
        const isLayoutDisabled = !page?.canEdit || !templateDrawed;
        const canSeeRulesExists = page && 'canSeeRules' in page;

        return [
            {
                icon: 'pi-file',
                label: 'editema.editor.navbar.content',
                href: 'content',
                id: 'content'
            },
            {
                icon: 'pi-table',
                label: 'editema.editor.navbar.layout',
                href: 'layout',
                id: 'layout',
                isDisabled: isLayoutDisabled || !isEnterpriseLicense,
                tooltip: templateDrawed
                    ? null
                    : 'editema.editor.navbar.layout.tooltip.cannot.edit.advanced.template'
            },
            {
                icon: 'pi-sliders-h',
                label: 'editema.editor.navbar.rules',
                id: 'rules',
                href: `rules/${page?.identifier}`,
                isDisabled:
                    (canSeeRulesExists && !page.canSeeRules) ||
                    !page?.canEdit ||
                    !isEnterpriseLicense
            },
            {
                iconURL: 'experiments',
                label: 'editema.editor.navbar.experiments',
                href: `experiments/${page?.identifier}`,
                id: 'experiments',
                isDisabled: !page?.canEdit || !isEnterpriseLicense
            },
            {
                icon: 'pi-th-large',
                label: 'editema.editor.navbar.page-tools',
                id: 'page-tools'
            },
            {
                icon: 'pi-ellipsis-v',
                label: 'editema.editor.navbar.properties',
                id: 'properties',
                isDisabled: isLoading
            }
        ];
    });

    // Component builds SEO params locally
    protected readonly $seoParams = computed<DotPageToolUrlParams>(() => {
        const pageAPIResponse = this.uveStore.pageAPIResponse();
        const url = sanitizeURL(pageAPIResponse?.page.pageURI);
        const currentUrl = url.startsWith('/') ? url : '/' + url;
        const requestHostName = getRequestHostName(this.uveStore.pageParams());

        return {
            siteId: pageAPIResponse?.site?.identifier,
            languageId: pageAPIResponse?.viewAs.language.id,
            currentUrl,
            requestHostName
        };
    });

    // Component builds error display locally
    protected readonly $errorDisplay = computed<{ code: number; pageInfo: InfoPage } | null>(() => {
        const errorCode = this.uveStore.errorCode();
        if (!errorCode) return null;

        return getErrorPayload(errorCode);
    });

    // Component determines read permissions locally
    protected readonly $canRead = computed<boolean>(() => {
        const pageAPIResponse = this.uveStore.pageAPIResponse();
        return pageAPIResponse?.page?.canRead ?? false;
    });

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

    readonly $updateBreadcrumbEffect = effect(() => {
        const pageAPIResponse = this.uveStore.pageAPIResponse();

        if (pageAPIResponse) {
            this.#globalStore.addNewBreadcrumb({
                label: pageAPIResponse?.page.title,
                url: this.uveStore.pageParams().url
            });
        }
    });

    ngOnInit(): void {
        const params = this.#getPageParams();
        const viewParams = this.#getViewParams(params.mode);

        this.uveStore.patchViewParams(viewParams);

        // Check if we already have page data loaded with matching params
        // This prevents reloading when navigating between child routes (content <-> layout)
        const currentPageParams = this.uveStore.pageParams();
        const hasPageData = !!this.uveStore.pageAPIResponse();
        const paramsMatch = currentPageParams &&
            currentPageParams.url === params.url &&
            currentPageParams.language_id === params.language_id &&
            currentPageParams.mode === params.mode;

        // Only load if we don't have data or params have changed
        if (!hasPageData || !paramsMatch) {
            this.uveStore.loadPageAsset(params);
        }

        this.#siteService.switchSite$
            .pipe(takeUntilDestroyed(this.destroyRef))
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
     * Handles closing the banner message by setting showBanner to false
     */
    onCloseMessage() {
        this.$showBanner.set(false);
    }

    /**
     * Toggles the lock state of the current page
     * Gets lock options from toggleLockOptions signal and calls store method to handle the lock/unlock
     */
    toggleLock() {
        const { inode, isLocked, isLockedByCurrentUser } = this.$toggleLockOptions();
        this.uveStore.toggleLock(inode, isLocked, isLockedByCurrentUser);
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
