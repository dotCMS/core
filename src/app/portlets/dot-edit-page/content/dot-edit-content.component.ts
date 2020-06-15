import { Observable, Subject, fromEvent, merge } from 'rxjs';

import { filter, takeUntil, pluck, take, tap } from 'rxjs/operators';
import { ActivatedRoute } from '@angular/router';
import { Component, OnInit, ViewChild, ElementRef, NgZone, OnDestroy } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

import { SiteService } from 'dotcms-js';

import { DotAlertConfirmService } from '@services/dot-alert-confirm';
import { DotEditContentHtmlService } from './services/dot-edit-content-html/dot-edit-content-html.service';
import { DotEditPageService } from '@services/dot-edit-page/dot-edit-page.service';
import { DotGlobalMessageService } from '@components/_common/dot-global-message/dot-global-message.service';
import { DotLoadingIndicatorService } from '@components/_common/iframe/dot-loading-indicator/dot-loading-indicator.service';
import { DotMessageService } from '@services/dot-messages-service';
import {
    DotPageContainer,
    DotPageContainerPersonalized
} from '../shared/models/dot-page-container.model';
import { DotPageContent } from '../shared/models/dot-page-content.model';
import { DotPageRenderState } from '../shared/models/dot-rendered-page-state.model';
import { DotPageStateService } from './services/dot-page-state/dot-page-state.service';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { DotPageMode } from '../shared/models/dot-page-mode.enum';
import { DotPageRender } from '../shared/models/dot-rendered-page.model';
import { DotContentletEditorService } from '@components/dot-contentlet-editor/services/dot-contentlet-editor.service';
import { DotUiColorsService } from '@services/dot-ui-colors/dot-ui-colors.service';
import { DotCMSContentType } from 'dotcms-models';
import {
    PageModelChangeEvent,
    PageModelChangeEventType
} from './services/dot-edit-content-html/models';
import { IframeOverlayService } from '@components/_common/iframe/service/iframe-overlay.service';
import { DotCustomEventHandlerService } from '@services/dot-custom-event-handler/dot-custom-event-handler.service';

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

    private readonly customEventsHandler;
    private destroy$: Subject<boolean> = new Subject<boolean>();
    private pageStateInternal: DotPageRenderState;

    constructor(
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
        public iframeOverlayService: IframeOverlayService
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
                    this.reload();
                },
                'reorder-menu': (reorderMenuUrl: string) => {
                    this.reorderMenuUrl = reorderMenuUrl;
                },
                'save-menu-order': () => {
                    this.reorderMenuUrl = '';
                    this.reload();
                },
                'error-saving-menu-order': () => {
                    this.reorderMenuUrl = '';
                    this.dotGlobalMessageService.error(
                        this.dotMessageService.get('an-unexpected-system-error-occurred')
                    );
                },
                'cancel-save-menu-order': () => {
                    this.reorderMenuUrl = '';
                }
            };
        }
    }

    ngOnInit() {
        this.dotLoadingIndicatorService.show();

        this.setInitalData();
        this.subscribeSwitchSite();
        this.subscribeIframeCustomEvents();
        this.subscribeIframeActions();
        this.subscribePageModelChange();
        this.subscribeOverlayService();
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
        if (
            this.shouldSetContainersHeight() &&
            $event.currentTarget.contentDocument.body.innerHTML
        ) {
            this.dotEditContentHtmlService.setContaintersChangeHeightListener(
                this.pageStateInternal.layout
            );
        }

        const doc = $event.target.contentWindow.document;
        this.dotUiColorsService.setColors(doc.querySelector('html'));
    }

    /**
     * Reload the edit page
     *
     * @memberof DotEditContentComponent
     */
    reload(): void {
        this.dotPageStateService.reload();
    }

    /**
     * Handle form selected
     *
     * @param ContentType item
     * @memberof DotEditContentComponent
     */
    onFormSelected(item: DotCMSContentType): void {
        this.dotEditContentHtmlService
            .renderAddedForm(item)
            .subscribe((model: DotPageContainer[]) => {
                if (model) {
                    this.saveToPage(model)
                        .pipe(take(1))
                        .subscribe(() => {
                            this.reload();
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

    private isInternallyNavigatingToSamePage(url: string): boolean {
        return this.route.snapshot.queryParams.url === url;
    }

    private shouldSetContainersHeight() {
        return (
            this.pageStateInternal &&
            this.pageStateInternal.layout &&
            this.pageStateInternal.state.mode === DotPageMode.EDIT
        );
    }

    private saveContent(event: PageModelChangeEvent): void {
        this.saveToPage(event.model)
            .pipe(
                tap(() => {
                    if (this.shouldSetContainersHeight()) {
                        this.dotEditContentHtmlService.setContaintersSameHeight(
                            this.pageStateInternal.layout
                        );
                    }
                }),
                filter(() => this.shouldReload(event.type))
            )
            .subscribe(() => {
                this.reload();
            });
    }

    private shouldReload(type: PageModelChangeEventType): boolean {
        return (
            type !== PageModelChangeEventType.MOVE_CONTENT &&
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
                    this.dotGlobalMessageService.success(
                        this.dotMessageService.get('dot.common.message.saved')
                    );
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

    private addContentlet($event: any): void {
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
                        event.target.contentWindow.ngEditContentletEvents = this.dotEditContentHtmlService.contentletEvents$;
                    }
                }
            });
        }
    }

    private editContentlet($event: any): void {
        this.dotContentletEditorService.edit({
            data: {
                inode: $event.dataset.dotInode
            },
            events: {
                load: (event) => {
                    event.target.contentWindow.ngEditContentletEvents = this.dotEditContentHtmlService.contentletEvents$;
                }
            }
        });
    }

    private iframeActionsHandler(event: any): Function {
        const eventsHandlerMap = {
            edit: this.editContentlet.bind(this),
            code: this.editContentlet.bind(this),
            add: this.addContentlet.bind(this),
            remove: this.removeContentlet.bind(this),
            select: () => {
                this.dotContentletEditorService.clear();
            },
            save: () => {
                this.reload();
            }
        };

        return eventsHandlerMap[event];
    }

    private subscribeIframeCustomEvents(): void {
        fromEvent(window.document, 'ng-event')
            .pipe(pluck('detail'), takeUntil(this.destroy$))
            .subscribe((customEvent: any) => {
                if (this.customEventsHandler[customEvent.name]) {
                    this.customEventsHandler[customEvent.name](customEvent.data);
                }
            });
    }

    private removeContentlet($event: any): void {
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
        if (this.shouldEditMode(pageState)) {
            this.dotEditContentHtmlService.initEditMode(pageState, this.iframe);
        } else {
            this.dotEditContentHtmlService.renderPage(pageState, this.iframe);
        }
    }

    private subscribeIframeActions(): void {
        this.dotEditContentHtmlService.iframeActions$
            .pipe(takeUntil(this.destroy$))
            .subscribe((contentletEvent: any) => {
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
                filter((event: PageModelChangeEvent) => !!event.model.length),
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
        this.siteService.switchSite$.pipe(takeUntil(this.destroy$)).subscribe(() => {
            this.reload();
        });
    }

    private subscribeOverlayService(): void {
        this.iframeOverlayService.overlay
            .pipe(takeUntil(this.destroy$))
            .subscribe((val: boolean) => (this.showOverlay = val));
    }
}
