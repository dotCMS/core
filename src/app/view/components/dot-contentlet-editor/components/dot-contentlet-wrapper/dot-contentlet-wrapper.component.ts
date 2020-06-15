import { Component, Input, EventEmitter, Output } from '@angular/core';
import { DotContentletEditorService } from '../../services/dot-contentlet-editor.service';
import { DotMessageService } from '@services/dot-messages-service';
import { DotAlertConfirmService } from '@services/dot-alert-confirm';
import { DotRouterService } from '@services/dot-router/dot-router.service';

export interface DotCMSEditPageEvent {
    name: string;
    data: {
        url: string;
        languageId: string;
        hostId: string;
    };
}

@Component({
    selector: 'dot-contentlet-wrapper',
    templateUrl: './dot-contentlet-wrapper.component.html',
    styleUrls: ['./dot-contentlet-wrapper.component.scss']
})
export class DotContentletWrapperComponent{
    @Input()
    header = '';

    @Input()
    url: string;

    @Output()
    close: EventEmitter<any> = new EventEmitter();

    @Output()
    custom: EventEmitter<any> = new EventEmitter();

    private isContentletModified = false;
    private readonly customEventsHandler;

    constructor(
        private dotContentletEditorService: DotContentletEditorService,
        private dotAlertConfirmService: DotAlertConfirmService,
        private dotMessageService: DotMessageService,
        private dotRouterService: DotRouterService
    ) {
        if (!this.customEventsHandler) {
            this.customEventsHandler = {
                close: () => {
                    this.onClose();
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
                'save-page': () => {
                    this.isContentletModified = false;
                },
                'edit-contentlet-loaded': (e: CustomEvent) => {
                    this.header = e.detail.data.contentType;
                }
            };
        }
    }

    /**
     * Habdle the before close dialog event
     *
     * @param * $event
     * @memberof DotEditContentletComponent
     */
    onBeforeClose($event?: { close: () => void }): void {
        if (this.isContentletModified) {
            this.dotAlertConfirmService.confirm({
                accept: () => {
                    $event.close();
                },
                reject: () => {},
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
        this.dotContentletEditorService.clear();
        this.isContentletModified = false;
        this.header = '';
        this.close.emit();
    }

    /**
     * Handle the custome events from the DotDialogIframe component
     *
     * @param CustomEvent $event
     * @memberof DotAddContentletComponent
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
     * @memberof DotAddContentletComponent
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
     * @memberof DotAddContentletComponent
     */
    onLoad($event): void {
        if (this.dotContentletEditorService.load) {
            this.dotContentletEditorService.load($event);
        }
    }
}
