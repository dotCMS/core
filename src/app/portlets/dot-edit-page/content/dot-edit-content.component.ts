import { ActivatedRoute } from '@angular/router';
import { Component, OnInit, ViewChild, ElementRef, NgZone, OnDestroy } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

import { Subject } from 'rxjs/Subject';
import { Subscription } from 'rxjs/Subscription';
import { Observable } from 'rxjs/Observable';

import * as _ from 'lodash';
import { SiteService, ResponseView } from 'dotcms-js/dotcms-js';

import { DotDialogService } from '../../../api/services/dot-dialog';
import { DotEditContentHtmlService } from './services/dot-edit-content-html/dot-edit-content-html.service';
import { DotEditPageService } from '../../../api/services/dot-edit-page/dot-edit-page.service';
import { DotEditPageToolbarComponent } from './components/dot-edit-page-toolbar/dot-edit-page-toolbar.component';
import { DotEditPageViewAs } from '../../../shared/models/dot-edit-page-view-as/dot-edit-page-view-as.model';
import { DotGlobalMessageService } from '../../../view/components/_common/dot-global-message/dot-global-message.service';
import { DotHttpErrorManagerService, DotHttpErrorHandled } from '../../../api/services/dot-http-error-manager/dot-http-error-manager.service';
import { DotLoadingIndicatorService } from '../../../view/components/_common/iframe/dot-loading-indicator/dot-loading-indicator.service';
import { DotMenuService } from '../../../api/services/dot-menu.service';
import { DotMessageService } from '../../../api/services/dot-messages-service';
import { DotPageContainer } from '../shared/models/dot-page-container.model';
import { DotPageContent } from '../shared/models/dot-page-content.model';
import { DotPageState, DotRenderedPageState } from '../shared/models/dot-rendered-page-state.model';
import { DotPageStateService } from './services/dot-page-state/dot-page-state.service';
import { DotRenderedPage } from './../shared/models/dot-rendered-page.model';
import { DotRouterService } from '../../../api/services/dot-router/dot-router.service';
import { PageMode } from '../shared/models/page-mode.enum';


@Component({
    selector: 'dot-edit-content',
    templateUrl: './dot-edit-content.component.html',
    styleUrls: ['./dot-edit-content.component.scss']
})
export class DotEditContentComponent implements OnInit, OnDestroy {
    @ViewChild('contentletActionsIframe') contentletActionsIframe: ElementRef;
    @ViewChild('iframe') iframe: ElementRef;
    @ViewChild('toolbar') toolbar: DotEditPageToolbarComponent;

    contentletActionsUrl: SafeResourceUrl;
    dialogSize = {
        height: null,
        width: null
    };
    dialogTitle: string;
    isModelUpdated = false;
    pageState: DotRenderedPageState;
    swithSiteSub: Subscription;

    private originalValue: any;

    constructor(
        private dotDialogService: DotDialogService,
        private dotEditPageService: DotEditPageService,
        private dotGlobalMessageService: DotGlobalMessageService,
        private dotHttpErrorManagerService: DotHttpErrorManagerService,
        private dotMenuService: DotMenuService,
        private dotMessageService: DotMessageService,
        private dotPageStateService: DotPageStateService,
        private dotRouterService: DotRouterService,
        private ngZone: NgZone,
        private route: ActivatedRoute,
        private sanitizer: DomSanitizer,
        private siteService: SiteService,
        public dotEditContentHtmlService: DotEditContentHtmlService,
        public dotLoadingIndicatorService: DotLoadingIndicatorService
    ) {}

    ngOnInit() {
        this.dotLoadingIndicatorService.show();

        this.dotMessageService
            .getMessages([
                'editpage.content.contentlet.remove.confirmation_message.message',
                'editpage.content.contentlet.remove.confirmation_message.header',
                'editpage.content.contentlet.add.content',
                'dot.common.message.saving',
                'dot.common.message.saved'
            ])
            .subscribe();

        this.dotEditContentHtmlService.iframeActions.subscribe((contentletEvent: any) => {
            this.ngZone.run(() => {
                this.iframeActionsHandler(contentletEvent.name)(contentletEvent);
            });
        });

        this.dotEditContentHtmlService.pageModelChange.filter((model) => model.length).subscribe((model) => {
            if (this.originalValue) {
                this.ngZone.run(() => {
                    this.isModelUpdated = !_.isEqual(model, this.originalValue);
                });
            } else {
                this.setOriginalValue(model);
            }
        });

        this.route.parent.parent.data.pluck('content').subscribe((pageState: DotRenderedPageState) => {
            this.setPageState(pageState);
        });

        this.swithSiteSub = this.switchSiteSubscription();

        this.setDialogSize();
    }

    ngOnDestroy(): void {
        this.swithSiteSub.unsubscribe();
    }

    /**
     * Callback when dialog hide
     *
     * @memberof DotEditContentComponent
     */
    onHideDialog(): void {
        this.dialogTitle = null;
        this.contentletActionsUrl = null;
    }

    /**
     * Handle the iframe page load
     * @param {any} $event
     * @memberof DotEditContentComponent
     */
    onLoad(_event): void {
        this.dotLoadingIndicatorService.hide();
    }

    /**
     * Handle the changes of the state gf the page
     *
     * @param {DotPageState} newState
     * @memberof DotEditContentComponent
     */
    statePageHandler(newState: DotPageState): void {
        this.dotGlobalMessageService.display(this.dotMessageService.get('dot.common.message.saving'));

        this.dotPageStateService.set(this.pageState.page, newState).subscribe(
            (pageState: DotRenderedPageState) => {
                this.setPageState(pageState);

                this.dotGlobalMessageService.display(this.dotMessageService.get('dot.common.message.saved'));
            },
            (err: ResponseView) => {
                this.handleSetPageStateFailed(err);
            }
        );
    }

    /**
     * Save the page's content
     *
     * @memberof DotEditContentComponent
     */
    saveContent(): void {
        this.dotGlobalMessageService.loading(this.dotMessageService.get('dot.common.message.saving'));
        this.dotEditPageService.save(this.pageState.page.identifier, this.dotEditContentHtmlService.getContentModel()).subscribe(() => {
            this.dotGlobalMessageService.display(this.dotMessageService.get('dot.common.message.saved'));
            this.setOriginalValue();
        });
    }

    /**
     * Hanlde changes in the configuration of "View As" toolbar
     *
     * @param {DotEditPageViewAs} viewAsConfig
     * @memberof DotEditContentComponent
     */
    changeViewAsHandler(viewAsConfig: DotEditPageViewAs): void {
        // TODO: Refactor to send just the pageState.
        this.dotPageStateService.set(this.pageState.page, this.pageState.state, viewAsConfig).subscribe(
            (pageState: DotRenderedPageState) => {
                this.setPageState(pageState);
            },
            (err: ResponseView) => {
                this.handleSetPageStateFailed(err);
            }
        );
    }

    private addContentlet($event: any): void {
        const container: DotPageContainer = {
            identifier: $event.dataset.dotIdentifier,
            uuid: $event.dataset.dotUuid
        };
        this.dotEditContentHtmlService.setContainterToAppendContentlet(container);
        this.dialogTitle = this.dotMessageService.get('editpage.content.contentlet.add.content');

        this.loadDialogEditor(
            `/html/ng-contentlet-selector.jsp?ng=true&container_id=${$event.dataset.dotIdentifier}&add=${$event.dataset.dotAdd}`,
            $event.contentletEvents
        );
    }

    private closeDialog(): void {
        this.dialogTitle = null;
        this.contentletActionsUrl = null;
    }

    private editContentlet($event: any): void {
        const container: DotPageContainer = {
            identifier: $event.container.dotIdentifier,
            uuid: $event.container.dotUuid
        };
        this.dotEditContentHtmlService.setContainterToEditContentlet(container);

        this.dotMenuService.getDotMenuId('content').subscribe((portletId: string) => {
            // tslint:disable-next-line:max-line-length
            const url = `/c/portal/layout?p_l_id=${portletId}&p_p_id=content&p_p_action=1&p_p_state=maximized&p_p_mode=view&_content_struts_action=%2Fext%2Fcontentlet%2Fedit_contentlet&_content_cmd=edit&inode=${$event.dataset.dotInode}&referer=%2Fc%2Fportal%2Flayout%3Fp_l_id%3D${portletId}%26p_p_id%3Dcontent%26p_p_action%3D1%26p_p_state%3Dmaximized%26_content_struts_action%3D%2Fext%2Fcontentlet%2Fview_contentlets`;

            // TODO: this will get the title of the contentlet but will need and update to the endpoint to do it
            this.dialogTitle = 'Edit Contentlet';
            this.loadDialogEditor(url, $event.contentletEvents);
        });
    }

    private errorHandler(err: ResponseView): Observable<DotRenderedPageState> {
        this.dotHttpErrorManagerService.handle(err).subscribe((res: DotHttpErrorHandled) => {
            if (!res.redirected) {
                this.dotRouterService.gotoPortlet('/c/site-browser');
            }
        });
        return Observable.empty();
    }

    private iframeActionsHandler(event: any): Function {
        const eventsHandlerMap = {
            edit: this.editContentlet.bind(this),
            add: this.addContentlet.bind(this),
            remove: this.removeContentlet.bind(this),
            cancel: this.closeDialog.bind(this),
            close: this.closeDialog.bind(this),
            save: () => {}
        };

        return eventsHandlerMap[event];
    }

    private handleSetPageStateFailed(err: ResponseView): void {
        this.dotHttpErrorManagerService.handle(err).subscribe((res: any) => {
            if (res.forbidden) {
                this.dotRouterService.gotoPortlet('/c/site-browser');
            } else {
                this.route.queryParams
                    .pluck('url')
                    .concatMap((url: string) => this.dotPageStateService.get(url))
                    .subscribe((pageState: DotRenderedPageState) => {
                        this.setPageState(pageState);
                    });
            }
        });
    }

    private loadDialogEditor(url: string, contentletEvents: Subject<any>): void {
        this.setDialogSize();
        this.contentletActionsUrl = this.sanitizer.bypassSecurityTrustResourceUrl(url);

        /*
            We have an ngIf in the <iframe> to prevent the jsp to load before the dialog shows, so we need to wait that
            element it's available in the DOM
        */
        setTimeout(() => {
            const editContentletIframeEl = this.contentletActionsIframe.nativeElement;

            /*
                TODO: should I remove this listener when when the dialog closes?
            */
            editContentletIframeEl.addEventListener('load', () => {
                editContentletIframeEl.contentWindow.ngEditContentletEvents = contentletEvents;
            });
        }, 0);
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
            header: this.dotMessageService.get('editpage.content.contentlet.remove.confirmation_message.header'),
            message: this.dotMessageService.get('editpage.content.contentlet.remove.confirmation_message.message')
        });
    }

    private renderPage(pageState: DotRenderedPageState): void {
        if (this.shouldEditMode(pageState)) {
            this.dotEditContentHtmlService.initEditMode(pageState.html, this.iframe);
        } else {
            this.dotEditContentHtmlService.renderPage(pageState.html, this.iframe);
        }
    }

    private setDialogSize(): void {
        this.dialogSize = {
            width: window.innerWidth - 200,
            height: window.innerHeight - 100
        };
    }

    private setPageState(pageState: DotRenderedPageState): void {
        this.pageState = pageState;
        this.renderPage(pageState);
    }

    private setOriginalValue(model?: any): void {
        this.originalValue = model || this.dotEditContentHtmlService.getContentModel();
        this.isModelUpdated = false;
    }

    private shouldEditMode(pageState: DotRenderedPageState): boolean {
        return pageState.state.mode === PageMode.EDIT && !pageState.state.lockedByAnotherUser;
    }

    private switchSiteSubscription(): Subscription {
        return this.siteService.switchSite$.subscribe(() => {
            this.dotPageStateService
                .get(this.route.snapshot.queryParams.url)
                .catch((err: ResponseView) => {
                    return this.errorHandler(err);
                })
                .subscribe((pageState: DotRenderedPageState) => {
                    this.setPageState(pageState);
                    this.originalValue = null;
                });
        });
    }
}
