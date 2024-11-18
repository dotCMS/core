import { Subject } from 'rxjs';

import { CommonModule, Location } from '@angular/common';
import { Component, effect, inject, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';

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
import { DotInfoPageComponent, DotNotLicenseComponent, SafeUrlPipe } from '@dotcms/ui';

import { EditEmaNavigationBarComponent } from './components/edit-ema-navigation-bar/edit-ema-navigation-bar.component';

import { DotEmaDialogComponent } from '../components/dot-ema-dialog/dot-ema-dialog.component';
import { DotActionUrlService } from '../services/dot-action-url/dot-action-url.service';
import { DotPageApiParams, DotPageApiService } from '../services/dot-page-api.service';
import { WINDOW } from '../shared/consts';
import { FormStatus, NG_CUSTOM_EVENTS } from '../shared/enums';
import { DialogAction, DotPage } from '../shared/models';
import { UVEStore } from '../store/dot-uve.store';
import { compareUrlPaths } from '../utils';

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
        SafeUrlPipe,
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

    readonly translatePageEffect = effect(() => {
        const { page, currentLanguage } = this.uveStore.$translateProps();

        if (currentLanguage && !currentLanguage?.translated) {
            this.createNewTranslation(currentLanguage, page);
        }
    });

    ngOnInit(): void {
        const { queryParams, data } = this.#activatedRoute.snapshot;
        const { data: dotData } = data;
        const allowedDevURLs = dotData?.options?.allowedDevURLs;

        // This can be a fuction that returns the queryParams to use
        const queryParamsClone = { ...queryParams } as DotPageApiParams;
        const validHost = this.checkClientHostAccess(queryParamsClone?.clientHost, allowedDevURLs);
        if (!validHost) {
            delete queryParamsClone?.clientHost;
            this.#location.replaceState(
                this.#router.createUrlTree([], { queryParams: queryParamsClone }).toString()
            );
        }

        this.uveStore.init(queryParamsClone);

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
                    // At this point we are in the language of the translation, if the user didn't save we need to navigate to the default language
                    this.navigate({
                        language_id: 1
                    });
                }

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
        const url = this.extractPageRefererUrl(event);
        const targetUrl = this.getTargetUrl(url);

        if (this.shouldNavigate(targetUrl)) {
            // Navigate to the new URL if it's different from the current one
            this.navigate({ url: targetUrl });

            return;
        }

        this.uveStore.reload();
    }
    /**
     * Extracts the htmlPageReferer url from the event payload.
     *
     * @param {CustomEvent} event - The event object containing the payload with the URL.
     * @return {string | undefined} - The extracted URL or undefined if not found.
     */
    private extractPageRefererUrl(event: CustomEvent): string | undefined {
        return event.detail.payload?.htmlPageReferer;
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
        const currentUrl = this.uveStore.params().url;

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
        this.uveStore.reload();
    }

    private navigate(queryParams) {
        this.#router.navigate([], {
            queryParams,
            queryParamsHandling: 'merge'
        });
    }

    /**
     * Check if the clientHost is in the whitelist provided by the app
     *
     * @private
     * @param {string} clientHost
     * @param {*} [allowedDevURLs=[]]
     * @return {*}
     * @memberof DotEmaShellComponent
     */
    private checkClientHostAccess(clientHost: string, allowedDevURLs: string[] = []): boolean {
        // If we don't have a whitelist or a clientHost we can't access it
        if (!clientHost || !Array.isArray(allowedDevURLs) || !allowedDevURLs.length) {
            return false;
        }

        // Most IDEs and terminals add a / at the end of the URL, so we need to sanitize it
        const sanitizedClientHost = clientHost.endsWith('/') ? clientHost.slice(0, -1) : clientHost;

        // We need to sanitize the whitelist as well
        const sanitizedAllowedDevURLs = allowedDevURLs.map((url) =>
            url.endsWith('/') ? url.slice(0, -1) : url
        );

        // If the clientHost is in the whitelist we can access it
        return sanitizedAllowedDevURLs.includes(sanitizedClientHost);
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
            reject: () => {
                this.navigate({
                    language_id: 1
                });
            }
        });
    }
}
