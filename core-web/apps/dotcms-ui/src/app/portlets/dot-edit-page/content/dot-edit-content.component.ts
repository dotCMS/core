import { fromEvent, merge, Observable, of, Subject } from 'rxjs';

import { CommonModule } from '@angular/common';
import { Component, ElementRef, inject, NgZone, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { ActivatedRoute, NavigationEnd, Router, RouterModule } from '@angular/router';

import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { DialogModule } from 'primeng/dialog';
import { DialogService } from 'primeng/dynamicdialog';
import { TooltipModule } from 'primeng/tooltip';

import { filter, map, pluck, switchMap, take, takeUntil, tap } from 'rxjs/operators';

import {
    DotAlertConfirmService,
    DotCurrentUserService,
    DotESContentService,
    DotEventsService,
    DotFavoritePageService,
    DotGlobalMessageService,
    DotLicenseService,
    DotMessageService,
    DotPageStateService,
    DotPropertiesService,
    DotRouterService,
    DotSessionStorageService,
    DotUiColorsService
} from '@dotcms/data-access';
import { SiteService } from '@dotcms/dotcms-js';
import {
    DEFAULT_VARIANT_NAME,
    DotCMSContentlet,
    DotCMSContentType,
    DotConfigurationVariables,
    DotContainerStructure,
    DotContentletEventAddContentType,
    DotExperiment,
    DotIframeEditEvent,
    DotPageContainer,
    DotPageContent,
    DotPageMode,
    DotPageRender,
    DotPageRenderState,
    DotVariantData,
    ESContent,
    FeaturedFlags,
    PageModelChangeEvent,
    PageModelChangeEventType,
    SeoMetaTags
} from '@dotcms/dotcms-models';
import {
    DotFavoritePageComponent,
    DotResultsSeoToolComponent,
    DotSelectSeoToolComponent
} from '@dotcms/portlets/dot-ema/ui';
import { DotIconComponent } from '@dotcms/ui';
import { DotLoadingIndicatorService, generateDotFavoritePageUrl } from '@dotcms/utils';

import { DotEditPageToolbarComponent } from './components/dot-edit-page-toolbar/dot-edit-page-toolbar.component';
import { DotFormSelectorComponent } from './components/dot-form-selector/dot-form-selector.component';
import { DotWhatsChangedComponent } from './components/dot-whats-changed/dot-whats-changed.component';
import { DotEditContentHtmlService } from './services/dot-edit-content-html/dot-edit-content-html.service';

import { DotCustomEventHandlerService } from '../../../api/services/dot-custom-event-handler/dot-custom-event-handler.service';
import { DotShowHideFeatureDirective } from '../../../shared/directives/dot-show-hide-feature/dot-show-hide-feature.directive';
import { DotOverlayMaskComponent } from '../../../view/components/_common/dot-overlay-mask/dot-overlay-mask.component';
import { DotLoadingIndicatorComponent } from '../../../view/components/_common/iframe/dot-loading-indicator/dot-loading-indicator.component';
import { IframeOverlayService } from '../../../view/components/_common/iframe/service/iframe-overlay.service';
import { DotEditContentletComponent } from '../../../view/components/dot-contentlet-editor/components/dot-edit-contentlet/dot-edit-contentlet.component';
import { DotReorderMenuComponent } from '../../../view/components/dot-contentlet-editor/components/dot-reorder-menu/dot-reorder-menu.component';
import { DotContentletEditorService } from '../../../view/components/dot-contentlet-editor/services/dot-contentlet-editor.service';
import { DotPaletteComponent } from '../components/dot-palette/dot-palette.component';
import { DotEditPageToolbarSeoComponent } from '../seo/components/dot-edit-page-toolbar-seo/dot-edit-page-toolbar-seo.component';

export const EDIT_BLOCK_EDITOR_CUSTOM_EVENT = 'edit-block-editor';

/**
 * Edit content page component, render the html of a page and bind all events to make it ediable.
 *
 * @export
 * @class DotEditContentComponent
 * @implements {OnInit}
 * @implements {OnDestroy}
 */
@Component({
    selector: 'dot-edit-content',
    templateUrl: './dot-edit-content.component.html',
    styleUrls: ['./dot-edit-content.component.scss'],
    imports: [
        CommonModule,
        ButtonModule,
        DialogModule,
        CheckboxModule,
        RouterModule,
        DotEditContentletComponent,
        DotWhatsChangedComponent,
        DotFormSelectorComponent,
        DotReorderMenuComponent,
        TooltipModule,
        DotLoadingIndicatorComponent,
        DotOverlayMaskComponent,
        DotPaletteComponent,
        DotIconComponent,
        DotEditPageToolbarComponent,
        DotEditPageToolbarSeoComponent,
        DotShowHideFeatureDirective,
        DotResultsSeoToolComponent,
        DotSelectSeoToolComponent
    ]
})
export class DotEditContentComponent implements OnInit, OnDestroy {
    private dialogService = inject(DialogService);
    private dotContentletEditorService = inject(DotContentletEditorService);
    private dotDialogService = inject(DotAlertConfirmService);
    private dotGlobalMessageService = inject(DotGlobalMessageService);
    private dotMessageService = inject(DotMessageService);
    private dotPageStateService = inject(DotPageStateService);
    private dotRouterService = inject(DotRouterService);
    private dotUiColorsService = inject(DotUiColorsService);
    private ngZone = inject(NgZone);
    private route = inject(ActivatedRoute);
    private router = inject(Router);
    private siteService = inject(SiteService);
    private dotCustomEventHandlerService = inject(DotCustomEventHandlerService);
    dotEditContentHtmlService = inject(DotEditContentHtmlService);
    dotLoadingIndicatorService = inject(DotLoadingIndicatorService);
    sanitizer = inject(DomSanitizer);
    iframeOverlayService = inject(IframeOverlayService);
    private dotConfigurationService = inject(DotPropertiesService);
    private dotLicenseService = inject(DotLicenseService);
    private dotEventsService = inject(DotEventsService);
    private dotESContentService = inject(DotESContentService);
    private dotSessionStorageService = inject(DotSessionStorageService);
    private dotCurrentUser = inject(DotCurrentUserService);
    private dotFavoritePageService = inject(DotFavoritePageService);

    @ViewChild('iframe') iframe: ElementRef;

    contentletActionsUrl: SafeResourceUrl;
    pageState$: Observable<DotPageRenderState>;
    showWhatsChanged = false;
    editForm = false;
    showIframe = true;
    reorderMenuUrl = '';
    showOverlay = false;
    dotPageMode = DotPageMode;
    allowedContent: string[] = null;
    isEditMode = false;
    paletteCollapsed = false;
    isEnterpriseLicense$ = of(false);
    variantData: Observable<DotVariantData>;
    featureFlagSeo = FeaturedFlags.FEATURE_FLAG_SEO_IMPROVEMENTS;
    seoOGTags: SeoMetaTags;
    seoOGTagsResults = null;
    pageLanguageId: string;

    private readonly customEventsHandler;
    private destroy$: Subject<boolean> = new Subject<boolean>();
    private pageStateInternal: DotPageRenderState;
    private pageSaved$: Subject<void> = new Subject<void>();

    constructor() {
        if (!this.customEventsHandler) {
            this.customEventsHandler = {
                'remote-render-edit': ({ pathname }) => {
                    this.dotRouterService.goToEditPage({ url: pathname.slice(1) });
                },
                'load-edit-mode-page': (pageRendered: DotPageRender) => {
                    /*
This is the events that gets emitted from the backend when the user
browse from the page internal links
*/

                    const dotRenderedPageState = new DotPageRenderState(
                        this.pageStateInternal.user,
                        pageRendered
                    );

                    this.variantData = null; // internal navigation, reset variant data - leave experiments.

                    if (this.isInternallyNavigatingToSamePage(pageRendered.page.pageURI)) {
                        this.dotPageStateService.setLocalState(dotRenderedPageState);
                    } else {
                        this.dotPageStateService.setInternalNavigationState(dotRenderedPageState);
                        this.dotRouterService.goToEditPage({
                            url: pageRendered.page.pageURI
                        });
                    }
                },
                'in-iframe': () => {
                    this.reload(null);
                },
                'reorder-menu': (reorderMenuUrl: string) => {
                    this.reorderMenuUrl = reorderMenuUrl;
                },
                'save-menu-order': () => {
                    this.reorderMenuUrl = '';
                    this.reload(null);
                },
                'error-saving-menu-order': () => {
                    this.reorderMenuUrl = '';
                    this.dotGlobalMessageService.error(
                        this.dotMessageService.get('an-unexpected-system-error-occurred')
                    );
                },
                'cancel-save-menu-order': () => {
                    this.reorderMenuUrl = '';
                    this.reload(null);
                },
                'edit-block-editor': (element) => {
                    this.dotEventsService.notify(EDIT_BLOCK_EDITOR_CUSTOM_EVENT, element);
                }
            };
        }
    }

    ngOnInit() {
        this.isEnterpriseLicense$ = this.dotLicenseService.isEnterprise().pipe(take(1));
        this.dotLoadingIndicatorService.show();
        this.setInitalData();
        this.subscribeSwitchSite();
        this.subscribeToNgEvents();
        this.subscribeIframeActions();
        this.subscribePageModelChange();
        this.subscribeOverlayService();
        this.subscribeDraggedContentType();
        this.getExperimentResolverData();
        this.subscribeToLanguageChange();

        /*This is needed when the user is in the edit mode in an experiment variant
        and navigate to another page with the page menu and want to go back with the
         browser back button */
        this.router.events
            .pipe(
                takeUntil(this.destroy$),
                filter((event) => event instanceof NavigationEnd)
            )
            .subscribe(() => {
                this.getExperimentResolverData();
            });

        this.pageSaved$.pipe(takeUntil(this.destroy$)).subscribe(() => {
            // If this changes and the dialog closes we trigger a reload
            this.dotContentletEditorService.close$.pipe(take(1)).subscribe(() => {
                this.reload(null);
            });
        });
    }

    ngOnDestroy(): void {
        if (!this.router.routerState.snapshot.url.startsWith('/edit-page/layout')) {
            this.dotSessionStorageService.removeVariantId();
        }

        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Go to the experiment
     * @memberof DotEditContentComponent
     */
    backToExperiment() {
        const { experimentId } = this.route.snapshot.queryParams;

        this.router.navigate(
            [
                '/edit-page/experiments/',
                this.pageStateInternal.page.identifier,
                experimentId,
                'configuration'
            ],
            {
                queryParams: {
                    mode: null,
                    variantName: null,
                    experimentId: null
                },
                queryParamsHandling: 'merge'
            }
        );
    }

    /**
     * Close Reorder Menu Dialog
     * @memberof DotEditContentComponent
     */
    onCloseReorderDialog(): void {
        this.reorderMenuUrl = '';
    }

    /**
     * Handle the iframe page load
     * @param any $event
     * @memberof DotEditContentComponent
     */
    onLoad($event): void {
        this.dotLoadingIndicatorService.hide();
        const doc = $event.target.contentWindow.document;
        this.dotUiColorsService.setColors(doc.querySelector('html'));
    }

    /**
     * Reload the edit page. If content comes reload with the provided contentlet.
     ** @param DotCMSContentlet contentlet
     * @memberof DotEditContentComponent
     */
    reload(contentlet: DotCMSContentlet): void {
        contentlet
            ? this.dotRouterService.goToEditPage({
                  url: contentlet.url,
                  host_id: contentlet.host,
                  language_id: contentlet.languageId
              })
            : this.dotPageStateService.reload();
    }

    /**
     * Handle form selected
     *
     * @param ContentType item
     * @memberof DotEditContentComponent
     */
    onFormSelected(item: DotCMSContentType): void {
        this.dotEditContentHtmlService.renderAddedForm(item.id);
        this.editForm = false;
    }

    /**
     * Handle cancel button click in the toolbar
     *
     * @memberof DotEditContentComponent
     */
    onCancelToolbar() {
        this.dotRouterService.goToSiteBrowser();
    }

    /**
     * Handle the custom events emmited by the Edit Contentlet
     *
     * @param CustomEvent $event
     * @memberof DotEditContentComponent
     */
    onCustomEvent($event: CustomEvent): void {
        // If we save we trigger a change
        if ($event.detail?.name === 'save-page') return this.pageSaved$.next();

        this.dotCustomEventHandlerService.handle($event);
    }

    /**
     * Execute actions needed when closing the create dialog.
     *
     * @memberof DotEditContentComponent
     */
    handleCloseAction(): void {
        this.dotEditContentHtmlService.removeContentletPlaceholder();
    }

    /**
     * Handle add Form ContentType from Content Palette.
     *
     * @memberof DotEditContentComponent
     */
    addFormContentType(): void {
        this.editForm = true;
        this.dotEditContentHtmlService.removeContentletPlaceholder();
    }

    /**
     * Fires a dynamic dialog instance with DotFavoritePage component
     *
     * @param boolean openDialog
     * @memberof DotEditContentComponent
     */
    showFavoritePageDialog(openDialog: boolean): void {
        if (openDialog) {
            const favoritePageUrl = generateDotFavoritePageUrl({
                deviceInode: this.pageStateInternal.viewAs.device?.inode,
                languageId: this.pageStateInternal.viewAs.language.id,
                pageURI: this.pageStateInternal.page.pageURI,
                siteId: this.pageStateInternal.site?.identifier
            });

            this.dialogService.open(DotFavoritePageComponent, {
                header: this.dotMessageService.get('favoritePage.dialog.header'),
                width: '80rem',
                data: {
                    page: {
                        favoritePageUrl: favoritePageUrl,
                        favoritePage: this.pageStateInternal.state.favoritePage
                    },
                    onSave: (favoritePageUrl: string) => {
                        this.updateFavoritePageIconStatus(favoritePageUrl);
                    },
                    onDelete: (favoritePageUrl: string) => {
                        this.updateFavoritePageIconStatus(favoritePageUrl);
                    }
                }
            });
        }
    }

    private updateFavoritePageIconStatus(pageUrl: string) {
        this.dotCurrentUser
            .getCurrentUser()
            .pipe(
                switchMap(({ userId }) =>
                    this.dotFavoritePageService
                        .get({ limit: 10, userId, url: pageUrl })
                        .pipe(take(1))
                )
            )
            .subscribe((response: ESContent) => {
                const favoritePage = response.jsonObjectView?.contentlets[0];
                this.dotPageStateService.setFavoritePageHighlight(favoritePage);
            });
    }

    private setAllowedContent(pageState: DotPageRenderState): void {
        this.dotConfigurationService
            .getKeyAsList(DotConfigurationVariables.CONTENT_PALETTE_HIDDEN_CONTENT_TYPES)
            .pipe(take(1))
            .subscribe((results) => {
                this.allowedContent = this.filterAllowedContentTypes(results, pageState) || [];
            });
    }

    private isInternallyNavigatingToSamePage(url: string): boolean {
        return this.route.snapshot.queryParams.url === url;
    }

    private shouldReload(type: PageModelChangeEventType): boolean {
        return (
            (type !== PageModelChangeEventType.REMOVE_CONTENT &&
                this.pageStateInternal.page.remoteRendered) ||
            type === PageModelChangeEventType.SAVE_ERROR
        );
    }

    private addContentType($event: DotContentletEventAddContentType): void {
        const container: DotPageContainer = {
            identifier: $event.data.container.dotIdentifier,
            uuid: $event.data.container.dotUuid
        };
        this.dotEditContentHtmlService.setContainterToAppendContentlet(container);

        if ($event.data.contentType.variable !== 'forms') {
            this.dotContentletEditorService
                .getActionUrl($event.data.contentType.variable)
                .pipe(take(1))
                .subscribe((url) => {
                    url = this.setCurrentContentLang(url);
                    this.dotContentletEditorService.create({
                        data: { url },
                        events: {
                            load: (event) => {
                                (event.target as HTMLIFrameElement).contentWindow[
                                    'ngEditContentletEvents'
                                ] = this.dotEditContentHtmlService.contentletEvents$;
                            }
                        }
                    });
                });
        } else {
            this.addFormContentType();
        }
    }

    private searchContentlet($event: DotIframeEditEvent): void {
        const container: DotPageContainer = {
            identifier: $event.dataset.dotIdentifier,
            uuid: $event.dataset.dotUuid
        };
        this.dotEditContentHtmlService.setContainterToAppendContentlet(container);

        if ($event.dataset.dotAdd === 'form') {
            this.editForm = true;
        } else {
            this.dotContentletEditorService.add({
                header: this.dotMessageService.get('dot.common.content.search'),
                data: {
                    container: $event.dataset.dotIdentifier,
                    baseTypes: $event.dataset.dotAdd
                },
                events: {
                    load: (event) => {
                        (event.target as HTMLIFrameElement).contentWindow[
                            'ngEditContentletEvents'
                        ] = this.dotEditContentHtmlService.contentletEvents$;
                    }
                }
            });
        }
    }

    private editContentlet(inode: string): void {
        this.dotContentletEditorService.edit({
            data: {
                inode
            },
            events: {
                load: (event) => {
                    (event.target as HTMLIFrameElement).contentWindow['ngEditContentletEvents'] =
                        this.dotEditContentHtmlService.contentletEvents$;
                }
            }
        });
    }

    private iframeActionsHandler(event: string): (contentlet: DotIframeEditEvent) => void {
        const eventsHandlerMap = {
            edit: this.editHandlder.bind(this),
            code: ({ dataset }) => this.editContentlet(dataset.dotInode),
            add: this.searchContentlet.bind(this),
            remove: this.removeContentlet.bind(this),
            'add-content': this.addContentType.bind(this),
            select: () => this.dotContentletEditorService.clear(),
            save: () => this.reload(null)
        };

        return eventsHandlerMap[event];
    }

    private editHandlder($event: DotIframeEditEvent): void {
        const { dotInode: inode } = $event.dataset;
        this.editContentlet(inode);
    }

    private subscribeToNgEvents(): void {
        fromEvent(window.document, 'ng-event')
            .pipe(pluck('detail'), takeUntil(this.destroy$))
            .subscribe((customEvent: { name: string; data: unknown }) => {
                if (this.customEventsHandler[customEvent.name]) {
                    this.customEventsHandler[customEvent.name](customEvent.data);
                }
            });
    }

    private removeContentlet($event: DotIframeEditEvent): void {
        this.dotDialogService.confirm({
            accept: () => {
                const pageContainer: DotPageContainer = {
                    identifier: $event.container.dotIdentifier,
                    uuid: $event.container.dotUuid
                };

                const pageContent: DotPageContent = {
                    inode: $event.dataset.dotInode,
                    identifier: $event.dataset.dotIdentifier
                };

                this.dotEditContentHtmlService.removeContentlet(pageContainer, pageContent);
            },
            header: this.dotMessageService.get(
                'editpage.content.contentlet.remove.confirmation_message.header'
            ),
            message: this.dotMessageService.get(
                'editpage.content.contentlet.remove.confirmation_message.message'
            )
        });
    }

    private renderPage(pageState: DotPageRenderState): void {
        this.dotEditContentHtmlService.setCurrentPage(pageState.page);
        this.dotEditContentHtmlService.setCurrentPersona(pageState.viewAs.persona);
        if (this.shouldEditMode(pageState)) {
            this.isEnterpriseLicense$.subscribe((isEnterpriseLicense) => {
                if (isEnterpriseLicense) {
                    this.setAllowedContent(pageState);
                }

                this.dotEditContentHtmlService.initEditMode(pageState, this.iframe);
                this.isEditMode = true;
            });
        } else {
            this.dotEditContentHtmlService.renderPage(pageState, this.iframe)?.then(() => {
                this.seoOGTags = this.dotEditContentHtmlService.getMetaTags();
                this.seoOGTagsResults = this.dotEditContentHtmlService.getMetaTagsResults();
            });
            this.isEditMode = false;
        }
    }

    private subscribeIframeActions(): void {
        this.dotEditContentHtmlService.iframeActions$
            .pipe(takeUntil(this.destroy$))
            .subscribe((contentletEvent: DotIframeEditEvent) => {
                this.ngZone.run(() => {
                    this.iframeActionsHandler(contentletEvent.name)(contentletEvent);
                });
            });
    }

    private setInitalData(): void {
        const content$ = merge(
            this.route.parent.parent.data.pipe(pluck('content')),
            this.dotPageStateService.state$
        ).pipe(takeUntil(this.destroy$));

        this.pageState$ = content$.pipe(
            takeUntil(this.destroy$),
            tap((pageState: DotPageRenderState) => {
                this.pageStateInternal = pageState;
                this.showIframe = false;
                // In order to get the iframe clean up we need to remove it and then re-add it to the DOM
                setTimeout(() => {
                    this.showIframe = true;
                    const intervalId = setInterval(() => {
                        if (this.iframe) {
                            this.renderPage(pageState);
                            clearInterval(intervalId);
                        }
                    }, 1);
                }, 0);
            })
        );
    }

    private shouldEditMode(pageState: DotPageRenderState): boolean {
        return pageState.state.mode === DotPageMode.EDIT && !pageState.state.lockedByAnotherUser;
    }

    private subscribePageModelChange(): void {
        this.dotEditContentHtmlService.pageModel$
            .pipe(
                filter((event: PageModelChangeEvent) => {
                    return !!event.model.length;
                }),
                takeUntil(this.destroy$)
            )
            .subscribe((event: PageModelChangeEvent) => {
                this.ngZone.run(() => {
                    this.dotPageStateService.updatePageStateHaveContent(event);
                    if (this.shouldReload(event.type)) {
                        this.reload(null);
                    }
                });
            });
    }

    private subscribeSwitchSite(): void {
        this.siteService.switchSite$.pipe(takeUntil(this.destroy$)).subscribe(() => {
            this.reload(null);
        });
    }

    private subscribeOverlayService(): void {
        this.iframeOverlayService.overlay
            .pipe(takeUntil(this.destroy$))
            .subscribe((val: boolean) => (this.showOverlay = val));
    }

    private subscribeDraggedContentType(): void {
        this.dotContentletEditorService.draggedContentType$
            .pipe(takeUntil(this.destroy$))
            .subscribe((contentType: DotCMSContentType | DotCMSContentlet) => {
                const iframeWindow: WindowProxy = (this.iframe.nativeElement as HTMLIFrameElement)
                    .contentWindow;
                iframeWindow['draggedContent'] = contentType;
            });
    }

    private filterAllowedContentTypes(
        blackList: string[] = [],
        pageState: DotPageRenderState
    ): string[] {
        const allowedContent = new Set();
        Object.values(pageState.containers).forEach((container) => {
            Object.values(container.containerStructures).forEach(
                (containerStructure: DotContainerStructure) => {
                    allowedContent.add(containerStructure.contentTypeVar.toLocaleLowerCase());
                }
            );
        });
        blackList.forEach((content) => allowedContent.delete(content.toLocaleLowerCase()));

        return [...allowedContent] as string[];
    }

    private getExperimentResolverData(): void {
        const { variantName, mode } = this.route.snapshot.queryParams;
        this.variantData = this.route.parent.parent.data.pipe(
            take(1),
            pluck('experiment'),
            filter((experiment) => !!experiment),
            map((experiment: DotExperiment) => {
                const variant = experiment.trafficProportion.variants.find(
                    (variant) => variant.id === variantName
                );

                return {
                    variant: {
                        id: variant.id,
                        url: variant.url,
                        title: variant.name,
                        isOriginal: variant.name === DEFAULT_VARIANT_NAME
                    },
                    pageId: experiment.pageId,
                    experimentId: experiment.id,
                    experimentStatus: experiment.status,
                    experimentName: experiment.name,
                    mode: mode
                } as DotVariantData;
            })
        );
    }

    /**
     * Subscribe to language change, because pageState.page.languageId is not being updated
     * as should be between dev environments.
     */
    private subscribeToLanguageChange(): void {
        this.route.queryParams.pipe(takeUntil(this.destroy$)).subscribe((params) => {
            this.pageLanguageId = params['language_id'];
        });
    }

    /**
     * Sets the language parameter in the given URL and returns the concatenated pathname and search.
     * the input URL doesn't include the host, origin is used as the base URL.
     *
     * @param {string} url - The input URL ( include pathname and search parameters).
     * @returns {string} - The concatenated pathname and search parameters with the language parameter set.
     */
    private setCurrentContentLang(url: string): string {
        const newUrl = new URL(url, window.location.origin);
        newUrl.searchParams.set('_content_lang', this.pageLanguageId);

        return newUrl.pathname + newUrl.search;
    }
}
