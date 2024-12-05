import { Subject } from 'rxjs';

import { CommonModule, Location } from '@angular/common';
import { Component, effect, inject, OnDestroy, OnInit, ViewChild } from '@angular/core';
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
    DotMessageService,
    DotPageLayoutService,
    DotPageRenderService,
    DotSeoMetaTagsService,
    DotSeoMetaTagsUtilService
} from '@dotcms/data-access';
import { SiteService } from '@dotcms/dotcms-js';
import { DotLanguage } from '@dotcms/dotcms-models';
import { DotPageToolsSeoComponent } from '@dotcms/portlets/dot-ema/ui';
import { DotInfoPageComponent, DotNotLicenseComponent } from '@dotcms/ui';

import { EditEmaNavigationBarComponent } from './components/edit-ema-navigation-bar/edit-ema-navigation-bar.component';

import { DotEmaDialogComponent } from '../components/dot-ema-dialog/dot-ema-dialog.component';
import { DotActionUrlService } from '../services/dot-action-url/dot-action-url.service';
import { DotPageApiParams, DotPageApiService } from '../services/dot-page-api.service';
import { WINDOW } from '../shared/consts';
import { FormStatus, NG_CUSTOM_EVENTS } from '../shared/enums';
import { DialogAction, DotPage } from '../shared/models';
import { UVEStore } from '../store/dot-uve.store';
import { checkClientHostAccess, compareUrlPaths, getAllowedPageParams } from '../utils';

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
        {
            provide: WINDOW,
            useValue: window
        },
        DotExperimentsService
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
export class DotEmaShellComponent implements OnInit, OnDestroy {
    @ViewChild('dialog') dialog!: DotEmaDialogComponent;
    @ViewChild('pageTools') pageTools!: DotPageToolsSeoComponent;

    readonly uveStore = inject(UVEStore);

    readonly #activatedRoute = inject(ActivatedRoute);
    readonly #router = inject(Router);
    readonly #siteService = inject(SiteService);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #confirmationService = inject(ConfirmationService);
    readonly #location = inject(Location);

    protected readonly $shellProps = this.uveStore.$shellProps;

    readonly #destroy$ = new Subject<boolean>();

    readonly $translatePageEffect = effect(() => {
        const { page, currentLanguage } = this.uveStore.$translateProps();

        if (currentLanguage && !currentLanguage?.translated) {
            this.createNewTranslation(currentLanguage, page);
        }
    });

    /**
     * Handle the update of the page params
     * When the page params change, we update the location
     *
     * @memberof DotEmaShellComponent
     */
    readonly $updateQueryParamsEffect = effect(() => {
        const pageParams = this.uveStore.pageParams();
        const viewParams = this.uveStore.viewParams();

        if (!pageParams && !viewParams) {
            return;
        }

        const queryParams = {
            ...(pageParams ?? {}),
            ...(viewParams ?? {})
        };
        this.#updateLocation(queryParams);
    });

    ngOnInit(): void {
        const params = this.#getPageParams();
        this.uveStore.loadPageAsset(params);

        // We need to skip one because it's the initial value
        this.#siteService.switchSite$
            .pipe(skip(1))
            .subscribe(() => this.#router.navigate(['/pages']));
    }

    ngOnDestroy(): void {
        this.#destroy$.next(true);
        this.#destroy$.complete();
    }

    handleNgEvent({ event, form }: DialogAction) {
        const { isTranslation, status } = form;

        const isSaved = status === FormStatus.SAVED;

        switch (event.detail.name) {
            case NG_CUSTOM_EVENTS.DIALOG_CLOSED: {
                if (!isSaved && isTranslation) {
                    this.#goBackToCurrentLanguage();
                }

                break;
            }

            case NG_CUSTOM_EVENTS.URL_IS_CHANGED: {
                this.handleSavePageEvent(event);
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
        const targetUrl = this.getTargetUrl(url.pathname);

        if (this.shouldNavigate(targetUrl)) {
            // Navigate to the new URL if it's different from the current one
            this.uveStore.loadPageAsset({ url: targetUrl });

            return;
        }

        this.uveStore.reloadCurrentPage();
    }

    /**
     * Determines the target URL for navigation.
     *
     * If `urlContentMap` is present and contains a `URL_MAP_FOR_CONTENT`, it will be used.
     * Otherwise, it falls back to the URL extracted from the event.
     *
     * @param {string | undefined} url - The URL extracted from the event.
     * @returns {string | undefined} - The final target URL for navigation, or undefined if none.
     */
    private getTargetUrl(url: string | undefined): string | undefined {
        const urlContentMap = this.uveStore.pageAPIResponse().urlContentMap;

        // Return URL from content map or fallback to the provided URL
        return urlContentMap?.URL_MAP_FOR_CONTENT || url;
    }

    /**
     * Determines whether navigation to a new URL is necessary.
     *
     * @param {string | undefined} targetUrl - The target URL for navigation.
     * @returns {boolean} - True if the current URL differs from the target URL and navigation is required.
     */
    private shouldNavigate(targetUrl: string | undefined): boolean {
        const currentUrl = this.uveStore.pageParams().url;

        // Navigate if the target URL is defined and different from the current URL
        return targetUrl !== undefined && !compareUrlPaths(targetUrl, currentUrl);
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
                contentType: page.contentType
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
     * Asks the user for confirmation to create a new translation for a given language.
     *
     * @param {DotLanguage} language - The language to create a new translation for.
     * @private
     *
     * @return {void}
     */
    private createNewTranslation(language: DotLanguage, page: DotPage): void {
        this.#confirmationService.confirm({
            header: this.#dotMessageService.get(
                'editpage.language-change-missing-lang-populate.confirm.header'
            ),
            message: this.#dotMessageService.get(
                'editpage.language-change-missing-lang-populate.confirm.message',
                language.language
            ),
            rejectIcon: 'hidden',
            acceptIcon: 'hidden',
            key: 'shell-confirm-dialog',
            accept: () => {
                this.dialog.translatePage({
                    page,
                    newLanguage: language.id
                });
            },
            reject: () => this.#goBackToCurrentLanguage()
        });
    }

    /**
     * Get the query params from the Router
     *
     * @return {*}  {DotPageApiParams}
     * @memberof DotEmaShellComponent
     */
    #getPageParams(): DotPageApiParams {
        const { queryParams, data } = this.#activatedRoute.snapshot;
        const uveConfig = data?.uveConfig;
        const allowedDevURLs = uveConfig?.options?.allowedDevURLs;

        // Clone queryParams to avoid mutation errors
        const params = getAllowedPageParams(queryParams);
        const validHost = checkClientHostAccess(params.clientHost, allowedDevURLs);

        if (!validHost) {
            delete params.clientHost;
        }

        if (uveConfig?.url && !validHost) {
            params.clientHost = uveConfig.url;
        }

        return params;
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
     * Use the Page Language to navigate back to the current language
     *
     * @memberof DotEmaShellComponent
     */
    #goBackToCurrentLanguage(): void {
        this.uveStore.loadPageAsset({ language_id: '1' });
    }
}
