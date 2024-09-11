import { Subject } from 'rxjs';

import { CommonModule } from '@angular/common';
import { Component, effect, inject, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogService } from 'primeng/dynamicdialog';
import { ToastModule } from 'primeng/toast';

import { skip, takeUntil } from 'rxjs/operators';

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
import { isEqual } from '@dotcms/utils/lib/shared/lodash/functions';

import { EditEmaNavigationBarComponent } from './components/edit-ema-navigation-bar/edit-ema-navigation-bar.component';

import { DotEmaDialogComponent } from '../components/dot-ema-dialog/dot-ema-dialog.component';
import { DotActionUrlService } from '../services/dot-action-url/dot-action-url.service';
import { DotPageApiParams, DotPageApiService } from '../services/dot-page-api.service';
import { WINDOW } from '../shared/consts';
import { FormStatus, NG_CUSTOM_EVENTS } from '../shared/enums';
import { DialogAction, DotPage } from '../shared/models';
import { UVEStore } from '../store/dot-uve.store';

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

    protected readonly $shellProps = this.uveStore.$shellProps;

    readonly #destroy$ = new Subject<boolean>();

    readonly translatePageEffect = effect(() => {
        const { page, currentLanguage } = this.uveStore.$translateProps();

        if (currentLanguage && !currentLanguage?.translated) {
            this.createNewTranslation(currentLanguage, page);
        }
    });

    ngOnInit(): void {
        this.#activatedRoute.queryParams
            .pipe(takeUntil(this.#destroy$))
            .subscribe((queryParams) => {
                const { data } = this.#activatedRoute.snapshot.data;

                // If we have a clientHost we need to check if it's in the whitelist
                if (queryParams.clientHost) {
                    const canAccessClientHost = this.checkClientHostAccess(
                        queryParams.clientHost,
                        data?.options?.allowedDevURLs
                    ); // If we don't have a whitelist we can't access the clientHost;

                    // If we can't access the clientHost we need to navigate to the default page
                    if (!canAccessClientHost) {
                        this.navigate({
                            ...queryParams,
                            clientHost: null // Clean the queryParam so the editor behaves as expected
                        });

                        return; // We need to return here, to avoid the editor to load with a non desirable clientHost
                    }
                }

                const currentParams = {
                    ...(queryParams as DotPageApiParams),
                    clientHost: queryParams.clientHost ?? data?.url
                };

                // We don't need to load if the params are the same
                if (isEqual(this.uveStore.params(), currentParams)) {
                    return;
                }

                this.uveStore.init(currentParams);
            });

        // We need to skip one because it's the initial value
        this.#siteService.switchSite$.pipe(skip(1)).subscribe(() => {
            this.#router.navigate(['/pages']);
        });
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
                // Maybe this can be out of the switch but we should evaluate it if it's needed
                // This can be undefined
                const url = event.detail.payload?.htmlPageReferer?.split('?')[0].replace('/', '');

                if (url && this.uveStore.params().url !== url) {
                    this.navigate({
                        url
                    });

                    return;
                }

                this.uveStore.reload();

                break;
            }
        }
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
