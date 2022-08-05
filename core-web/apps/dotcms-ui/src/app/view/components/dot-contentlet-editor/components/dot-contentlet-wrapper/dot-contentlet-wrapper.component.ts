import { Component, Input, EventEmitter, Output } from '@angular/core';
import { DotContentletEditorService } from '../../services/dot-contentlet-editor.service';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DotAlertConfirmService } from '@services/dot-alert-confirm';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { DotIframeService } from '@components/_common/iframe/service/dot-iframe/dot-iframe.service';
import { Title } from '@angular/platform-browser';

export interface DotCMSEditPageEvent {
    name: string;
    data: {
        url: string;
        languageId: string;
        hostId: string;
    };
}

interface DotCSMSavePageEvent {
    detail: {
        payload: {
            contentletInode: string;
            isMoveAction: boolean;
        };
    };
}

@Component({
    selector: 'dot-contentlet-wrapper',
    templateUrl: './dot-contentlet-wrapper.component.html',
    styleUrls: ['./dot-contentlet-wrapper.component.scss']
})
export class DotContentletWrapperComponent {
    @Input()
    header = '';

    @Input()
    url: string;

    @Output()
    shutdown: EventEmitter<unknown> = new EventEmitter();

    @Output()
    custom: EventEmitter<unknown> = new EventEmitter();

    private isContentletModified = false;
    private _appMainTitle = '';
    private readonly customEventsHandler;

    constructor(
        private dotContentletEditorService: DotContentletEditorService,
        private dotAlertConfirmService: DotAlertConfirmService,
        private dotMessageService: DotMessageService,
        private dotRouterService: DotRouterService,
        private dotIframeService: DotIframeService,
        private titleService: Title
    ) {
        if (!this.customEventsHandler) {
            this.customEventsHandler = {
                close: ({ detail: { data } }: CustomEvent) => {
                    this.onClose();
                    if (data?.redirectUrl) {
                        this.dotRouterService.goToEditPage({
                            url: data.redirectUrl,
                            language_id: data.languageId
                        });
                    }
                },
                'edit-page': ({ detail: { data } }: CustomEvent<DotCMSEditPageEvent>) => {
                    this.dotRouterService.goToEditPage({
                        url: data.url,
                        language_id: data.languageId,
                        host_id: data.hostId
                    });
                },
                'deleted-page': () => {
                    this.onClose();
                },
                'edit-contentlet-data-updated': (e: CustomEvent) => {
                    this.isContentletModified = e.detail.payload;
                },
                'save-page': (data: DotCSMSavePageEvent) => {
                    if (this.shouldRefresh(data)) {
                        this.dotIframeService.reload();
                    }

                    this.isContentletModified = false;
                },
                'edit-contentlet-loaded': (e: CustomEvent) => {
                    this._appMainTitle = this.titleService.getTitle();
                    this.header = e.detail.data.contentType;
                    this.titleService.setTitle(
                        `${
                            e.detail.data.pageTitle
                                ? e.detail.data.pageTitle + ' -'
                                : `${this.dotMessageService.get('New')} ${this.header} -`
                        } ${this.titleService.getTitle().split(' - ')[1]}`
                    );
                }
            };
        }
    }

    /**
     * Habdle the before close dialog event
     *
     * @param * $event
     * @memberof DotContentletWrapperComponent
     */
    onBeforeClose($event?: { close: () => void }): void {
        if (this.isContentletModified) {
            this.dotAlertConfirmService.confirm({
                accept: () => {
                    $event.close();
                },
                reject: () => {
                    //
                },
                header: this.dotMessageService.get('editcontentlet.lose.dialog.header'),
                message: this.dotMessageService.get('editcontentlet.lose.dialog.message'),
                footerLabel: {
                    accept: this.dotMessageService.get('editcontentlet.lose.dialog.accept')
                }
            });
        } else {
            $event.close();
        }
    }

    /**
     * Handle close event form the iframe
     *
     * @memberof DotContentletWrapperComponent
     */
    onClose(): void {
        this.titleService.setTitle(this._appMainTitle || this.titleService.getTitle());
        this.dotContentletEditorService.clear();
        this.isContentletModified = false;
        this.header = '';
        this.shutdown.emit();
    }

    /**
     * Handle the custome events from the DotDialogIframe component
     *
     * @param CustomEvent $event
     * @memberof DotContentletWrapperComponent
     */
    onCustomEvent($event: CustomEvent): void {
        if (this.customEventsHandler[$event.detail.name]) {
            this.customEventsHandler[$event.detail.name]($event);
        }

        this.custom.emit($event);
    }

    /**
     * Call the keyDown method from the service if exist
     *
     * @param any $event
     * @memberof DotContentletWrapperComponent
     */
    onKeyDown($event): void {
        if (this.dotContentletEditorService.keyDown) {
            this.dotContentletEditorService.keyDown($event);
        }
    }

    /**
     * Call the load method from the service if exist
     *
     * @param any $event
     * @memberof DotContentletWrapperComponent
     */
    onLoad($event): void {
        if (this.dotContentletEditorService.load) {
            this.dotContentletEditorService.load($event);
        }
    }

    private shouldRefresh(data: DotCSMSavePageEvent): boolean {
        // is not new content
        return (
            this.dotRouterService.currentPortlet.url.includes(
                data?.detail?.payload?.contentletInode
            ) && data?.detail?.payload?.isMoveAction
        );
    }
}
