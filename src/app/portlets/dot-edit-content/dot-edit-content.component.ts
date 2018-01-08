import { Component, OnInit, ViewChild, ElementRef, ChangeDetectorRef, NgZone } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { Subject } from 'rxjs/Subject';
import { ActivatedRoute } from '@angular/router';
import { DotEditContentHtmlService } from './services/dot-edit-content-html.service';
import { DotConfirmationService } from '../../api/services/dot-confirmation';
import { DotLoadingIndicatorService } from '../../view/components/_common/iframe/dot-loading-indicator/dot-loading-indicator.service';
import { DotMessageService } from '../../api/services/dot-messages-service';
@Component({
    selector: 'dot-edit-content',
    templateUrl: './dot-edit-content.component.html',
    styleUrls: ['./dot-edit-content.component.scss'],
})
export class DotEditContentComponent implements OnInit {
    @ViewChild('contentletActionsIframe') contentletActionsIframe: ElementRef;
    @ViewChild('iframe') iframe: ElementRef;

    contentletActionsUrl: SafeResourceUrl;
    dialogTitle: string;
    source: any;

    constructor(
        private dotConfirmationService: DotConfirmationService,
        private ref: ChangeDetectorRef,
        private route: ActivatedRoute,
        private sanitizer: DomSanitizer,
        private ngZone: NgZone,
        public dotEditContentHtmlService: DotEditContentHtmlService,
        public dotLoadingIndicatorService: DotLoadingIndicatorService,
        private dotMessageService: DotMessageService
    ) {}

    ngOnInit() {
        this.dotLoadingIndicatorService.show();
        this.route.data.pluck('editPageHTML').subscribe((editPageHTML: string) => {
            this.dotEditContentHtmlService.initEditMode(editPageHTML, this.iframe);

            this.dotEditContentHtmlService.contentletEvents.subscribe((res) => {
                switch (res.event) {
                    case 'edit':
                        this.editContentlet(res);
                        break;
                    case 'add':
                        this.addContentlet(res);
                        break;
                    case 'remove':
                        this.ngZone.run(() => {
                            this.removeContentlet(res);
                        });
                        break;
                    case 'cancel':
                        this.closeDialog();
                        break;
                    case 'save':
                        this.closeDialog();
                        break;
                    case 'select':
                        this.closeDialog();
                        break;
                    default:
                        break;
                }
            });
        });

        this.dotMessageService
            .getMessages([
                'editpage.content.contentlet.remove.confirmation_message.header',
                'editpage.content.contentlet.remove.confirmation_message.message',
                'editpage.content.contentlet.remove.confirmation_message.accept',
                'editpage.content.contentlet.remove.confirmation_message.reject'
            ])
            .subscribe();
    }

    onHide(): void {
        this.dialogTitle = null;
        this.contentletActionsUrl = null;
    }

    /**
     * it hides the loading indicator when the component loads
     * @param {any} $event
     * @memberof DotEditContentComponent
     */
    onLoad($event): void {
        this.dotLoadingIndicatorService.hide();
    }

    private addContentlet($event: any): void {
        this.dotEditContentHtmlService.setContainterToAppendContentlet($event.dataset.dotIdentifier);
        this.loadDialogEditor(
            $event.dataset.dotIdentifier,
            '/html/ng-contentlet-selector.html?ng=true',
            $event.contentletEvents,
        );
    }

    private closeDialog(): void {
        this.dialogTitle = null;
        this.contentletActionsUrl = null;

        /*
            I think because we are triggering the .next() from the jsp the Angular detect changes it's not
            happenning automatically, so I have to triggered manually so the changes propagates to the template
        */
        this.ref.detectChanges();
    }

    private editContentlet($event: any): void {
        // tslint:disable-next-line:max-line-length
        const url =
            '/c/portal/layout?p_l_id=71b8a1ca-37b6-4b6e-a43b-c7482f28db6c&p_p_id=content&p_p_action=1&p_p_state=maximized&p_p_mode=view&_content_struts_action=%2Fext%2Fcontentlet%2Fedit_contentlet&_content_cmd=edit&inode=aaee9776-8fb7-4501-8048-844912a20405&referer=%2Fc%2Fportal%2Flayout%3Fp_l_id%3D71b8a1ca-37b6-4b6e-a43b-c7482f28db6c%26p_p_id%3Dcontent%26p_p_action%3D1%26p_p_state%3Dmaximized%26_content_struts_action%3D%2Fext%2Fcontentlet%2Fview_contentlets';

        this.loadDialogEditor($event.dataset.dotIdentifier, url, $event.contentletEvents);
    }

    private loadDialogEditor(containerId: string, url: string, contentletEvents: Subject<any>): void {
        this.dialogTitle = containerId;
        this.contentletActionsUrl = this.sanitizer.bypassSecurityTrustResourceUrl(url);

        /*
            Again, because the click event it's comming form the iframe, we need to trigger the detect changes manually.
        */
        this.ref.detectChanges();

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
                rejectLabel: this.dotMessageService.get('editpage.content.contentlet.remove.confirmation_message.reject'),
            },
        });
    }
}
