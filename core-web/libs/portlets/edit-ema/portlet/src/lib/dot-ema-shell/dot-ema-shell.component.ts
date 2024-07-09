import { combineLatest, Observable, Subject } from 'rxjs';

import { CommonModule } from '@angular/common';
import { Component, inject, OnDestroy, OnInit, signal, ViewChild } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogService } from 'primeng/dynamicdialog';
import { ToastModule } from 'primeng/toast';

import { map, skip, take, takeUntil } from 'rxjs/operators';

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
import { DotLanguage, DotPageToolUrlParams } from '@dotcms/dotcms-models';
import { DotPageToolsSeoComponent } from '@dotcms/portlets/dot-ema/ui';
import { DotInfoPageComponent, DotNotLicenseComponent, InfoPage, SafeUrlPipe } from '@dotcms/ui';

import { EditEmaNavigationBarComponent } from './components/edit-ema-navigation-bar/edit-ema-navigation-bar.component';
import { EditEmaStore } from './store/dot-ema.store';

import { DotEmaDialogComponent } from '../components/dot-ema-dialog/dot-ema-dialog.component';
import { EditEmaEditorComponent } from '../edit-ema-editor/edit-ema-editor.component';
import { DotActionUrlService } from '../services/dot-action-url/dot-action-url.service';
import { DotPageApiParams, DotPageApiService } from '../services/dot-page-api.service';
import { WINDOW } from '../shared/consts';
import { NG_CUSTOM_EVENTS } from '../shared/enums';
import { DotPage, NavigationBarItem } from '../shared/models';

@Component({
    selector: 'dot-ema-shell',
    standalone: true,
    providers: [
        EditEmaStore,
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

    readonly $didTranslate = signal(false);

    readonly store = inject(EditEmaStore);
    EMA_INFO_PAGES: Record<'NOT_FOUND' | 'ACCESS_DENIED', InfoPage> = {
        NOT_FOUND: {
            icon: 'compass',
            title: 'editema.infopage.notfound.title',
            description: 'editema.infopage.notfound.description',
            buttonPath: '/pages',
            buttonText: 'editema.infopage.button.gotopages'
        },
        ACCESS_DENIED: {
            icon: 'ban',
            title: 'editema.infopage.accessdenied.title',
            description: 'editema.infopage.accessdenied.description',
            buttonPath: '/pages',
            buttonText: 'editema.infopage.button.gotopages'
        }
    };
    // We need to move the logic to a function, we still need to add enterprise logic
    shellProperties$: Observable<{
        items: NavigationBarItem[];
        canRead: boolean;
        seoProperties: DotPageToolUrlParams;
        error?: number;
    }> = this.store.shellProps$.pipe(
        map(({ currentUrl, page, host, languageId, siteId, templateDrawed, error }) => {
            const isLayoutDisabled = !page.canEdit || !templateDrawed;

            if (
                isLayoutDisabled &&
                this.#activatedRoute.firstChild.snapshot.url[0].path === 'layout'
            ) {
                this.#router.navigate(['./content'], { relativeTo: this.#activatedRoute });
            }

            return {
                items: [
                    {
                        icon: 'pi-file',
                        label: 'editema.editor.navbar.content',
                        href: 'content'
                    },
                    {
                        icon: 'pi-table',
                        label: 'editema.editor.navbar.layout',
                        href: 'layout',
                        isDisabled: isLayoutDisabled,
                        tooltip: templateDrawed
                            ? null
                            : 'editema.editor.navbar.layout.tooltip.cannot.edit.advanced.template'
                    },
                    {
                        icon: 'pi-sliders-h',
                        label: 'editema.editor.navbar.rules',
                        href: `rules/${page.identifier}`,
                        isDisabled: !page.canEdit
                    },
                    {
                        iconURL: 'experiments',
                        label: 'editema.editor.navbar.experiments',
                        href: `experiments/${page.identifier}`,
                        isDisabled: !page.canEdit
                    },
                    {
                        icon: 'pi-th-large',
                        label: 'editema.editor.navbar.page-tools',
                        action: () => {
                            this.pageTools.toggleDialog();
                        }
                    },
                    {
                        icon: 'pi-ellipsis-v',
                        label: 'editema.editor.navbar.properties',
                        action: () => {
                            this.dialog.editContentlet({
                                inode: page.inode,
                                title: page.title,
                                identifier: page.identifier,
                                contentType: page.contentType
                            });
                        }
                    }
                ],
                canRead: page.canRead,
                seoProperties: {
                    currentUrl,
                    languageId,
                    siteId,
                    requestHostName: host
                },
                error
            };
        })
    );
    readonly #activatedRoute = inject(ActivatedRoute);
    readonly #router = inject(Router);
    readonly #siteService = inject(SiteService);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #confirmationService = inject(ConfirmationService);

    readonly #destroy$ = new Subject<boolean>();
    #currentComponent: unknown;

    // We can internally navigate, so the PageID can change

    get queryParams(): DotPageApiParams {
        const queryParams = this.#activatedRoute.snapshot.queryParams;

        return {
            language_id: queryParams['language_id'],
            url: queryParams['url'],
            'com.dotmarketing.persona.id': queryParams['com.dotmarketing.persona.id'],
            variantName: queryParams['variantName'],
            clientHost: queryParams['clientHost']
        };
    }

    ngOnInit(): void {
        combineLatest([this.#activatedRoute.data, this.#activatedRoute.queryParams])
            .pipe(takeUntil(this.#destroy$))
            .subscribe(([{ data }, queryParams]) => {
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

                this.store.load({
                    ...(queryParams as DotPageApiParams),
                    clientHost: queryParams.clientHost ?? data?.url
                });
            });

        // We need to skip one because it's the initial value
        this.#siteService.switchSite$.pipe(skip(1)).subscribe(() => {
            this.#router.navigate(['/pages']);
        });

        // We need to check if the language is translated
        this.store.translateProps$
            .pipe(takeUntil(this.#destroy$))
            .subscribe(({ languages, page, pageLanguageId }) => {
                const currentLanguage = languages.find((lang) => lang.id === pageLanguageId);

                if (!currentLanguage.translated) {
                    this.createNewTranslation(currentLanguage, page);
                }
            });
    }

    ngOnDestroy(): void {
        this.#destroy$.next(true);
        this.#destroy$.complete();
    }

    onActivateRoute(event) {
        this.#currentComponent = event;
    }

    handleNgEvent({ event }: { event: CustomEvent }) {
        switch (event.detail.name) {
            case NG_CUSTOM_EVENTS.DIALOG_CLOSED: {
                if (!this.$didTranslate()) {
                    this.navigate({
                        language_id: 1 // We navigate to the default language if the user didn't translate
                    });
                } else {
                    this.$didTranslate.set(false);
                    this.reloadFromDialog();
                }

                break;
            }

            case NG_CUSTOM_EVENTS.EDIT_CONTENTLET_UPDATED: {
                // We need to check when the contentlet is updated, to know if we need to reload the page
                this.$didTranslate.set(true);
                break;
            }

            case NG_CUSTOM_EVENTS.SAVE_PAGE: {
                this.$didTranslate.set(true);
                const url = event.detail.payload.htmlPageReferer.split('?')[0].replace('/', '');

                if (this.queryParams.url !== url) {
                    this.navigate({
                        url
                    });

                    return;
                }

                if (this.#currentComponent instanceof EditEmaEditorComponent) {
                    this.#currentComponent.reloadIframeContent();
                }

                this.#activatedRoute.data.pipe(take(1)).subscribe(({ data }) => {
                    this.store.load({
                        ...this.queryParams,
                        clientHost: this.queryParams.clientHost ?? data?.url
                    });
                });
                break;
            }
        }
    }

    /**
     * Reloads the component from the dialog.
     */
    reloadFromDialog() {
        this.store.reload({ params: this.queryParams });
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
