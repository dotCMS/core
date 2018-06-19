import { Component, OnInit, Input, EventEmitter, Output } from '@angular/core';
import { DotContentletEditorService } from '../../services/dot-contentlet-editor.service';
import { DotMessageService } from '../../../../../api/services/dot-messages-service';
import { DotAlertConfirmService } from '../../../../../api/services/dot-alert-confirm';

@Component({
    selector: 'dot-contentlet-wrapper',
    templateUrl: './dot-contentlet-wrapper.component.html',
    styleUrls: ['./dot-contentlet-wrapper.component.scss']
})
export class DotContentletWrapperComponent implements OnInit {
    @Input() header = '';
    @Input() url: string;
    @Output() close: EventEmitter<any> = new EventEmitter();

    private isContentletModified = false;
    private readonly customEventsHandler;

    constructor(
        private dotContentletEditorService: DotContentletEditorService,
        private dotAlertConfirmService: DotAlertConfirmService,
        private dotMessageService: DotMessageService
    ) {
        if (!this.customEventsHandler) {
            this.customEventsHandler = {
                'close': (_e: CustomEvent) => {
                    this.onClose();
                },
                'edit-contentlet-data-updated': (e: CustomEvent) => {
                    this.isContentletModified = e.detail.payload;
                },
                'edit-contentlet-loaded': (e: CustomEvent) => {
                    this.header = e.detail.data.contentType;
                }
            };
        }
    }

    ngOnInit() {
        this.dotMessageService.getMessages([
            'editcontentlet.lose.dialog.header',
            'editcontentlet.lose.dialog.message',
            'editcontentlet.lose.dialog.accept'
        ]).subscribe();
    }

    /**
     * Habdle the before close dialog event
     *
     * @param {*} $event
     * @memberof DotEditContentletComponent
     */
    onBeforeClose($event?: { originalEvent: MouseEvent | KeyboardEvent; close: () => void }): void {
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
        this.header = null;
        this.dotContentletEditorService.clear();
        this.isContentletModified = false;
        this.close.emit();
    }

    /**
     * Handle the custome events from the DotDialogIframe component
     *
     * @param {any} $event
     * @memberof DotAddContentletComponent
     */
    onCustomEvent($event) {
        this.customEventsHandler[$event.detail.name]($event);
    }

    /**
     * Call the keyDown method from the service if exist
     *
     * @param {any} $event
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
     * @param {any} $event
     * @memberof DotAddContentletComponent
     */
    onLoad($event): void {
        if (this.dotContentletEditorService.load) {
            this.dotContentletEditorService.load($event);
        }
    }
}
