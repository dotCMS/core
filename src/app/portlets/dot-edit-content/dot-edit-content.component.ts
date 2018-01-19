
import * as _ from 'lodash';
import { Component, OnInit, ViewChild, ElementRef, ChangeDetectorRef, NgZone } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { Subject } from 'rxjs/Subject';
import { ActivatedRoute } from '@angular/router';
import { DotEditContentHtmlService } from './services/dot-edit-content-html.service';
import { DotConfirmationService } from '../../api/services/dot-confirmation';
import { DotContainerContentletService } from './services/dot-container-contentlet.service';
import { DotLoadingIndicatorService } from '../../view/components/_common/iframe/dot-loading-indicator/dot-loading-indicator.service';
import { DotMessageService } from '../../api/services/dot-messages-service';
import { DotRenderedPage } from '../dot-edit-page/shared/models/dot-rendered-page.model';
import { DotGlobalMessageService } from '../../view/components/_common/dot-global-message/dot-global-message.service';
import { DotMenuService } from '../../api/services/dot-menu.service';

@Component({
    selector: 'dot-edit-content',
    templateUrl: './dot-edit-content.component.html',
    styleUrls: ['./dot-edit-content.component.scss']
})
export class DotEditContentComponent implements OnInit {
    @ViewChild('contentletActionsIframe') contentletActionsIframe: ElementRef;
    @ViewChild('iframe') iframe: ElementRef;

    contentletActionsUrl: SafeResourceUrl;
    dialogSize = {
        height: null,
        width: null
    };
    dialogTitle: string;
    isModelUpdated = false;
    pageIdentifier: string;
    source: any;

    private originalValue: any;

    constructor(
        private dotConfirmationService: DotConfirmationService,
        private dotMenuService: DotMenuService,
        private dotContainerContentletService: DotContainerContentletService,
        private dotGlobalMessageService: DotGlobalMessageService,
        private dotMessageService: DotMessageService,
        private ngZone: NgZone,
        private route: ActivatedRoute,
        private sanitizer: DomSanitizer,
        public dotEditContentHtmlService: DotEditContentHtmlService,
        public dotLoadingIndicatorService: DotLoadingIndicatorService
    ) {}

    ngOnInit() {
        this.dotLoadingIndicatorService.show();

        this.route.data.pluck('editPageHTML').subscribe((editPageHTML: DotRenderedPage) => {
            this.pageIdentifier = editPageHTML.identifier;
            this.dotEditContentHtmlService.initEditMode(editPageHTML.render, this.iframe);

            this.dotEditContentHtmlService.contentletEvents.subscribe((contentletEvent: any) => {
                this.ngZone.run(() => {
                    switch (contentletEvent.name) {
                        case 'edit':
                            this.editContentlet(contentletEvent);
                            break;
                        case 'add':
                            this.addContentlet(contentletEvent);
                            break;
                        case 'remove':
                            this.removeContentlet(contentletEvent);
                            break;
                        case 'cancel':
                        case 'save':
                        case 'select':
                            this.closeDialog();
                            break;
                        default:
                            break;
                    }
                });
            });

            this.dotEditContentHtmlService.pageModelChange
                .subscribe(model =>  {
                    if (this.originalValue) {
                        this.ngZone.run(() => {
                            this.isModelUpdated = !_.isEqual(model, this.originalValue);
                        });
                    } else {
                        this.setOriginalValue(model);
                    }
                });
        });

        this.dotMessageService
            .getMessages([
                'editpage.content.contentlet.remove.confirmation_message.header',
                'editpage.content.contentlet.remove.confirmation_message.message',
                'editpage.content.contentlet.remove.confirmation_message.accept',
                'editpage.content.contentlet.remove.confirmation_message.reject',
                'editpage.content.contentlet.add.content',
                'dot.common.message.saving',
                'dot.common.message.saved'
            ])
            .subscribe();

        this.setDialogSize();
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
     * Save the page's content
     */
    saveContent(): void {
        this.dotGlobalMessageService.loading(this.dotMessageService.get('dot.common.message.saving'));
        this.dotContainerContentletService.saveContentlet(this.pageIdentifier, this.dotEditContentHtmlService.getContentModel())
            .subscribe(() => {
                this.dotGlobalMessageService.display(this.dotMessageService.get('dot.common.message.saved'));
                this.setOriginalValue();
            });
    }

    /**
     * it hides the loading indicator when the component loads
     * @param {any} $event
     * @memberof DotEditContentComponent
     */
    onLoad($event): void {
        this.dotLoadingIndicatorService.hide();
    }

    private setOriginalValue(model?: any): void {
        this.originalValue = model || this.dotEditContentHtmlService.getContentModel();
        this.isModelUpdated = false;
    }

    private addContentlet($event: any): void {
        this.dotEditContentHtmlService.setContainterToAppendContentlet($event.dataset.dotIdentifier);
        this.dialogTitle = this.dotMessageService.get('editpage.content.contentlet.add.content');

        this.loadDialogEditor(
            $event.dataset.dotIdentifier,
            `/html/ng-contentlet-selector.jsp?ng=true&container_id=${$event.dataset.dotIdentifier}&add=${$event.dataset.dotAdd}`,
            $event.contentletEvents
        );
    }

    private closeDialog(): void {
        this.dialogTitle = null;
        this.contentletActionsUrl = null;
    }

    private editContentlet($event: any): void {
        this.dotMenuService.getDotMenuId('content').subscribe((portletId: string) => {
            // tslint:disable-next-line:max-line-length
            const url =
            `/c/portal/layout?p_l_id=${portletId}&p_p_id=content&p_p_action=1&p_p_state=maximized&p_p_mode=view&_content_struts_action=%2Fext%2Fcontentlet%2Fedit_contentlet&_content_cmd=edit&inode=${$event.dataset.dotInode}&referer=%2Fc%2Fportal%2Flayout%3Fp_l_id%3D${portletId}%26p_p_id%3Dcontent%26p_p_action%3D1%26p_p_state%3Dmaximized%26_content_struts_action%3D%2Fext%2Fcontentlet%2Fview_contentlets`;

            // TODO: this will get the title of the contentlet but will need and update to the endpoint to do it
            this.dialogTitle = 'Edit Contentlet';
            this.loadDialogEditor($event.dataset.dotIdentifier, url, $event.contentletEvents);
        });
    }

    private loadDialogEditor(containerId: string, url: string, contentletEvents: Subject<any>): void {
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
        this.dotConfirmationService.confirm({
            accept: () => {
                this.dotEditContentHtmlService.removeContentlet($event.dataset.dotInode);
            },
            header: this.dotMessageService.get('editpage.content.contentlet.remove.confirmation_message.header'),
            message: this.dotMessageService.get('editpage.content.contentlet.remove.confirmation_message.message'),
            footerLabel: {
                acceptLabel: this.dotMessageService.get('editpage.content.contentlet.remove.confirmation_message.accept'),
                rejectLabel: this.dotMessageService.get('editpage.content.contentlet.remove.confirmation_message.reject')
            }
        });
    }

    private setDialogSize(): void {
        this.dialogSize = {
            width: (window.innerWidth) - 200,
            height: (window.innerHeight) - 100
        };

    }
}
