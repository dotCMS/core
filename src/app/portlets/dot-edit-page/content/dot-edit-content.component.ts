import { empty as observableEmpty, Observable, Subject, fromEvent } from 'rxjs';

import { concatMap, catchError, filter, takeUntil, pluck, take } from 'rxjs/operators';
import { ActivatedRoute } from '@angular/router';
import { Component, OnInit, ViewChild, ElementRef, NgZone, OnDestroy } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

import { SiteService, ResponseView } from 'dotcms-js/dotcms-js';

import { DotAlertConfirmService } from '@services/dot-alert-confirm';
import { DotEditContentHtmlService } from './services/dot-edit-content-html/dot-edit-content-html.service';
import { DotEditPageService } from '@services/dot-edit-page/dot-edit-page.service';
import { DotEditPageViewAs } from '@models/dot-edit-page-view-as/dot-edit-page-view-as.model';
import { DotGlobalMessageService } from '@components/_common/dot-global-message/dot-global-message.service';
import {
    DotHttpErrorManagerService,
    DotHttpErrorHandled
} from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { DotLoadingIndicatorService } from '@components/_common/iframe/dot-loading-indicator/dot-loading-indicator.service';
import { DotMessageService } from '@services/dot-messages-service';
import { DotPageContainer } from '../shared/models/dot-page-container.model';
import { DotPageContent } from '../shared/models/dot-page-content.model';
import { DotPageState, DotRenderedPageState } from '../shared/models/dot-rendered-page-state.model';
import { DotPageStateService } from './services/dot-page-state/dot-page-state.service';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { PageMode } from '../shared/models/page-mode.enum';
import { DotRenderedPage } from '../shared/models/dot-rendered-page.model';
import { DotEditPageDataService } from '../shared/services/dot-edit-page-resolver/dot-edit-page-data.service';
import { DotContentletEditorService } from '@components/dot-contentlet-editor/services/dot-contentlet-editor.service';
import { DotUiColorsService } from '@services/dot-ui-colors/dot-ui-colors.service';
import { ContentType } from '../../content-types/shared/content-type.model';

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
    @ViewChild('iframe')
    iframe: ElementRef;

    contentletActionsUrl: SafeResourceUrl;
    pageState: DotRenderedPageState;
    showWhatsChanged = false;
    editForm = false;
    showIframe = true;
    reorderMenuUrl = '';

    private readonly customEventsHandler;
    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private dotContentletEditorService: DotContentletEditorService,
        private dotDialogService: DotAlertConfirmService,
        private dotEditPageDataService: DotEditPageDataService,
        private dotEditPageService: DotEditPageService,
        private dotGlobalMessageService: DotGlobalMessageService,
        private dotHttpErrorManagerService: DotHttpErrorManagerService,
        private dotMessageService: DotMessageService,
        private dotPageStateService: DotPageStateService,
        private dotRouterService: DotRouterService,
        private dotUiColorsService: DotUiColorsService,
        private ngZone: NgZone,
        private route: ActivatedRoute,
        private siteService: SiteService,
        public dotEditContentHtmlService: DotEditContentHtmlService,
        public dotLoadingIndicatorService: DotLoadingIndicatorService,
        public sanitizer: DomSanitizer
    ) {
        if (!this.customEventsHandler) {
            this.customEventsHandler = {
                'load-edit-mode-page': (pageRendered: DotRenderedPage) => {
                    const dotRenderedPageState = new DotRenderedPageState(
                        this.pageState.user,
                        pageRendered
                    );

                    if (this.route.snapshot.queryParams.url === pageRendered.page.pageURI) {
                        this.setPageState(dotRenderedPageState);
                    } else {
                        this.dotEditPageDataService.set(dotRenderedPageState);
                        this.dotRouterService.goToEditPage(pageRendered.page.pageURI);
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
                    this.dotGlobalMessageService.display(
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

        this.getMessages();
        this.setInitalData();
        this.subscribeSwitchSite();
        this.subscribeIframeCustomEvents();
        this.subscribeIframeActions();
        this.subscribePageModelChange();
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
                this.pageState.layout
            );
        }

        const doc = $event.target.contentWindow.document;
        this.dotUiColorsService.setColors(doc.querySelector('html'));
    }

    /**
     * Handle the changes of the state gf the page
     *
     * @param DotPageState newState
     * @memberof DotEditContentComponent
     */
    statePageHandler(newState: DotPageState): void {
        if (this.shouldHideWhatsChanged(newState.mode)) {
            this.showWhatsChanged = false;
        }

        this.dotPageStateService
            .set(this.pageState.page, newState)
            .pipe(takeUntil(this.destroy$))
            .subscribe(
                (pageState: DotRenderedPageState) => {
                    this.setPageState(pageState);
                },
                (err: ResponseView) => {
                    this.handleSetPageStateFailed(err);
                }
            );
    }

    /**
     * Handle changes in the configuration of "View As" toolbar
     *
     * @param DotEditPageViewAs viewAsConfig
     * @memberof DotEditContentComponent
     */
    changeViewAsHandler(viewAsConfig: DotEditPageViewAs): void {
            this.dotPageStateService.reload(
                {
                    url: this.route.snapshot.queryParams.url,
                    mode: this.pageState.state.mode,
                    viewAs: {
                        persona_id: viewAsConfig.persona ? viewAsConfig.persona.identifier : null,
                        language_id: viewAsConfig.language.id,
                        device_inode: viewAsConfig.device ? viewAsConfig.device.inode : null
                    }
                }
            );
    }

    /**
     * Reload the edit page
     *
     * @memberof DotEditContentComponent
     */
    reload(): void {
        this.dotPageStateService
            .get(this.route.snapshot.queryParams.url)
            .pipe(catchError((err: ResponseView) => this.errorHandler(err)))
            .pipe(takeUntil(this.destroy$))
            .subscribe((pageState: DotRenderedPageState) => {
                this.setPageState(pageState);
            });
    }

    /**
     * Handle form selected
     *
     * @param ContentType item
     * @memberof DotEditContentComponent
     */
    onFormSelected(item: ContentType): void {
        this.dotEditContentHtmlService.renderAddedForm(item).subscribe((model) => {
            if (model) {
                this.dotEditPageService
                    .save(this.pageState.page.identifier, model)
                    .pipe(take(1))
                    .subscribe(() => {
                        this.dotGlobalMessageService.display(
                            this.dotMessageService.get('dot.common.message.saved')
                        );
                    });

                this.reload();
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

    private shouldSetContainersHeight() {
        return (
            this.pageState && this.pageState.layout && this.pageState.state.mode === PageMode.EDIT
        );
    }

    private shouldHideWhatsChanged(mode: PageMode): boolean {
        return (this.showWhatsChanged && mode === PageMode.EDIT) || mode === PageMode.LIVE;
    }

    private saveContent(model: DotPageContainer[]): void {
        this.dotGlobalMessageService.loading(
            this.dotMessageService.get('dot.common.message.saving')
        );

        this.dotEditPageService
            .save(this.pageState.page.identifier, model)
            .pipe(take(1))
            .subscribe(() => {
                this.dotGlobalMessageService.display(
                    this.dotMessageService.get('dot.common.message.saved')
                );
            });
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

    private errorHandler(err: ResponseView): Observable<DotRenderedPageState> {
        this.dotHttpErrorManagerService
            .handle(err)
            .pipe(takeUntil(this.destroy$))
            .subscribe((res: DotHttpErrorHandled) => {
                if (!res.redirected) {
                    this.dotRouterService.goToSiteBrowser();
                }
            });
        return observableEmpty();
    }

    private getMessages(): void {
        this.dotMessageService
            .getMessages([
                'editpage.content.contentlet.remove.confirmation_message.message',
                'editpage.content.contentlet.remove.confirmation_message.header',
                'dot.common.message.saving',
                'dot.common.message.saved',
                'dot.common.content.search',
                'editpage.content.save.changes.confirmation.header',
                'editpage.content.save.changes.confirmation.message',
                'an-unexpected-system-error-occurred'
            ])
            .pipe(take(1))
            .subscribe();
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
            save: () => {}
        };

        return eventsHandlerMap[event];
    }

    // TODO: this whole method need testing.
    private handleSetPageStateFailed(err: ResponseView): void {
        this.dotHttpErrorManagerService
            .handle(err)
            .pipe(takeUntil(this.destroy$))
            .subscribe((res: any) => {
                if (res.forbidden) {
                    this.dotRouterService.goToSiteBrowser();
                } else {
                    this.route.queryParams
                        .pipe(
                            pluck('url'),
                            concatMap((url: string) => this.dotPageStateService.get(url)),
                            takeUntil(this.destroy$)
                        )
                        .subscribe((pageState: DotRenderedPageState) => {
                            this.setPageState(pageState);
                        });
                }
            });
    }

    private subscribeIframeCustomEvents(): void {
        fromEvent(window.document, 'ng-event')
            .pipe(
                pluck('detail'),
                takeUntil(this.destroy$)
            )
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

    private renderPage(pageState: DotRenderedPageState): void {
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
        this.route.parent.parent.data
            .pipe(pluck('content'), takeUntil(this.destroy$))
            .subscribe((pageState: DotRenderedPageState) => {
                this.setPageState(pageState);
            });

        this.dotPageStateService.reload$
            .pipe(takeUntil(this.destroy$))
            .subscribe((pageState: DotRenderedPageState) => {
                this.setPageState(pageState);
            });
    }

    private setPageState(pageState: DotRenderedPageState): void {
        this.pageState = pageState;
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
    }

    private shouldEditMode(pageState: DotRenderedPageState): boolean {
        return pageState.state.mode === PageMode.EDIT && !pageState.state.lockedByAnotherUser;
    }

    private subscribePageModelChange(): void {
        this.dotEditContentHtmlService.pageModel$
            .pipe(filter((model: any) => model.length), takeUntil(this.destroy$))
            .subscribe((model: DotPageContainer[]) => {
                this.ngZone.run(() => {
                    this.saveContent(model);
                    if (this.shouldSetContainersHeight()) {
                        this.dotEditContentHtmlService.setContaintersSameHeight(
                            this.pageState.layout
                        );
                    }
                });
            });
    }

    private subscribeSwitchSite(): void {
        this.siteService.switchSite$.pipe(takeUntil(this.destroy$)).subscribe(() => {
            this.reload();
        });
    }
}
