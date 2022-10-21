import { Observable, Subject, fromEvent, merge, of } from 'rxjs';

import { filter, takeUntil, pluck, take, tap, skip, catchError } from 'rxjs/operators';
import { ActivatedRoute } from '@angular/router';
import { Component, OnInit, ViewChild, ElementRef, NgZone, OnDestroy } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

import { SiteService } from '@dotcms/dotcms-js';
import { DotCMSContentlet, DotCMSContentType } from '@dotcms/dotcms-models';

import { DotAlertConfirmService } from '@services/dot-alert-confirm';
import { DotEditContentHtmlService } from './services/dot-edit-content-html/dot-edit-content-html.service';
import { DotEditPageService } from '@services/dot-edit-page/dot-edit-page.service';
import { DotGlobalMessageService } from '@components/_common/dot-global-message/dot-global-message.service';
import { DotLoadingIndicatorService } from '@components/_common/iframe/dot-loading-indicator/dot-loading-indicator.service';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import {
    DotPageContainer,
    DotPageContainerPersonalized
} from '@models/dot-page-container/dot-page-container.model';
import { DotPageContent } from '../shared/models/dot-page-content.model';
import { DotPageRenderState } from '../shared/models/dot-rendered-page-state.model';
import { DotPageStateService } from './services/dot-page-state/dot-page-state.service';
import { DotRouterService } from '@services/dot-router/dot-router.service';

import { DotPageMode } from '@models/dot-page/dot-page-mode.enum';
import { DotPageRender } from '@models/dot-page/dot-rendered-page.model';
import { DotContentletEditorService } from '@components/dot-contentlet-editor/services/dot-contentlet-editor.service';
import { DotUiColorsService } from '@services/dot-ui-colors/dot-ui-colors.service';
import {
    PageModelChangeEvent,
    PageModelChangeEventType
} from './services/dot-edit-content-html/models';
import { IframeOverlayService } from '@components/_common/iframe/service/iframe-overlay.service';
import { DotCustomEventHandlerService } from '@services/dot-custom-event-handler/dot-custom-event-handler.service';
import { DotContainerStructure } from '@models/container/dot-container.model';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { HttpErrorResponse } from '@angular/common/http';
import { DotPropertiesService } from '@services/dot-properties/dot-properties.service';
import { DotLicenseService } from '@services/dot-license/dot-license.service';
import { DotContentletEventAddContentType } from './services/dot-edit-content-html/models/dot-contentlets-events.model';
import { DotIframeEditEvent } from '@dotcms/dotcms-models';
import { DotEventsService } from '@services/dot-events/dot-events.service';
import { DialogService } from 'primeng/dynamicdialog';
import { DotFavoritePageComponent } from '../components/dot-favorite-page/dot-favorite-page.component';

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
    styleUrls: ['./dot-edit-content.component.scss']
})
export class DotEditContentComponent implements OnInit, OnDestroy {
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
    isEnterpriseLicense = false;

    private readonly customEventsHandler;
    private destroy$: Subject<boolean> = new Subject<boolean>();
    private pageStateInternal: DotPageRenderState;

    constructor(
        private dialogService: DialogService,
        private dotContentletEditorService: DotContentletEditorService,
        private dotDialogService: DotAlertConfirmService,
        private dotEditPageService: DotEditPageService,
        private dotGlobalMessageService: DotGlobalMessageService,
        private dotMessageService: DotMessageService,
        private dotPageStateService: DotPageStateService,
        private dotRouterService: DotRouterService,
        private dotUiColorsService: DotUiColorsService,
        private ngZone: NgZone,
        private route: ActivatedRoute,
        private siteService: SiteService,
        private dotCustomEventHandlerService: DotCustomEventHandlerService,
        public dotEditContentHtmlService: DotEditContentHtmlService,
        public dotLoadingIndicatorService: DotLoadingIndicatorService,
        public sanitizer: DomSanitizer,
        public iframeOverlayService: IframeOverlayService,
        private httpErrorManagerService: DotHttpErrorManagerService,
        private dotConfigurationService: DotPropertiesService,
        private dotLicenseService: DotLicenseService,
        private dotEventsService: DotEventsService
    ) {
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

                    if (this.isInternallyNavigatingToSamePage(pageRendered.page.pageURI)) {
                        this.dotPageStateService.setLocalState(dotRenderedPageState);
                    } else {
                        this.dotPageStateService.setInternalNavigationState(dotRenderedPageState);
                        this.dotRouterService.goToEditPage({ url: pageRendered.page.pageURI });
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
        this.dotLicenseService
            .isEnterprise()
            .pipe(take(1))
            .subscribe((isEnterprise) => {
                this.isEnterpriseLicense = isEnterprise;
            });
        this.dotLoadingIndicatorService.show();
        this.setInitalData();
        this.subscribeSwitchSite();
        this.subscribeToNgEvents();
        this.subscribeIframeActions();
        this.subscribePageModelChange();
        this.subscribeOverlayService();
        this.subscribeDraggedContentType();
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
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
        this.dotEditContentHtmlService
            .renderAddedForm(item.id)
            .subscribe((model: DotPageContainer[]) => {
                if (model) {
                    this.saveToPage(model)
                        .pipe(take(1))
                        .subscribe(() => {
                            this.reload(null);
                        });
                }
            });

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
        this.pageState$.pipe(take(1)).subscribe((pageState: DotPageRenderState) => {
            if (openDialog) {
                this.dialogService.open(DotFavoritePageComponent, {
                    header: this.dotMessageService.get('favoritePage.dialog.header.add.page'),
                    width: '80rem',
                    data: {
                        page: {
                            pageState: pageState,
                            pageRenderedHtml: pageState.params.page.rendered || null
                        },
                        onSave: () => {
                            this.dotPageStateService.setFavoritePageHighlight(true);
                        }
                    }
                });
            }
        });
    }

    private setAllowedContent(pageState: DotPageRenderState): void {
        const CONTENT_HIDDEN_KEY = 'CONTENT_PALETTE_HIDDEN_CONTENT_TYPES';
        this.dotConfigurationService
            .getKeyAsList(CONTENT_HIDDEN_KEY)
            .pipe(take(1))
            .subscribe((results) => {
                this.allowedContent = this.filterAllowedContentTypes(results, pageState) || [];
            });
    }

    private isInternallyNavigatingToSamePage(url: string): boolean {
        return this.route.snapshot.queryParams.url === url;
    }

    private saveContent(event: PageModelChangeEvent): void {
        this.saveToPage(event.model)
            .pipe(
                filter((message: string) => {
                    return this.shouldReload(event.type) || message === 'error';
                }),
                take(1)
            )
            .subscribe(() => {
                this.reload(null);
            });
    }

    private shouldReload(type: PageModelChangeEventType): boolean {
        return (
            type !== PageModelChangeEventType.REMOVE_CONTENT &&
            this.pageStateInternal.page.remoteRendered
        );
    }

    private saveToPage(model: DotPageContainer[]): Observable<string> {
        this.dotGlobalMessageService.loading(
            this.dotMessageService.get('dot.common.message.saving')
        );

        return this.dotEditPageService
            .save(this.pageStateInternal.page.identifier, this.getPersonalizedModel(model) || model)
            .pipe(
                take(1),
                tap(() => {
                    this.dotGlobalMessageService.success();
                }),
                catchError((error: HttpErrorResponse) => {
                    this.httpErrorManagerService.handle(error);

                    return of('error');
                })
            );
    }

    private getPersonalizedModel(model: DotPageContainer[]): DotPageContainerPersonalized[] {
        const persona = this.pageStateInternal.viewAs.persona;

        if (persona && persona.personalized) {
            return model.map((container: DotPageContainer) => {
                return {
                    ...container,
                    personaTag: persona.keyTag
                };
            });
        }

        return null;
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

    private editContentlet($event: DotIframeEditEvent): void {
        this.dotContentletEditorService.edit({
            data: {
                inode: $event.dataset.dotInode
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
            edit: this.editContentlet.bind(this),
            code: this.editContentlet.bind(this),
            add: this.searchContentlet.bind(this),
            remove: this.removeContentlet.bind(this),
            'add-content': this.addContentType.bind(this),
            select: () => {
                this.dotContentletEditorService.clear();
            },
            save: () => {
                this.reload(null);
            }
        };

        return eventsHandlerMap[event];
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

        if (this.shouldEditMode(pageState)) {
            if (this.isEnterpriseLicense) {
                this.setAllowedContent(pageState);
            }

            this.dotEditContentHtmlService.initEditMode(pageState, this.iframe);
            this.isEditMode = true;
        } else {
            this.dotEditContentHtmlService.renderPage(pageState, this.iframe);
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
                    this.saveContent(event);
                });
            });
    }

    private subscribeSwitchSite(): void {
        this.siteService.switchSite$.pipe(skip(1), takeUntil(this.destroy$)).subscribe(() => {
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
}
