import { Component, EventEmitter, Input, OnInit, Output, inject } from '@angular/core';

import { DotAlertConfirmService, DotMessageService } from '@dotcms/data-access';
import { DotCMSContentTypeField, DotCMSContentTypeLayoutRow } from '@dotcms/dotcms-models';

/**
 * Display Tab Field
 *
 * @export
 * @class ContentTypeFieldsTabComponent
 */
@Component({
    selector: 'dot-content-type-fields-tab',
    styleUrls: ['./content-type-fields-tab.component.scss'],
    templateUrl: './content-type-fields-tab.component.html',
    standalone: false
})
export class ContentTypeFieldsTabComponent implements OnInit {
    private dotMessageService = inject(DotMessageService);
    private dotDialogService = inject(DotAlertConfirmService);

    @Input()
    fieldTab: DotCMSContentTypeLayoutRow;

    @Output()
    editTab: EventEmitter<DotCMSContentTypeField> = new EventEmitter();

    @Output()
    removeTab: EventEmitter<DotCMSContentTypeLayoutRow> = new EventEmitter();

    label: string;

    ngOnInit() {
        this.label = this.fieldTab.divider.name;
    }

    /**
     * Trigger the editTab event to change tab label
     * @memberof ContentTypeFieldsTabComponent
     */
    changeLabel($event: FocusEvent): void {
        $event.stopPropagation();
        $event.preventDefault();
        const label = ($event.target as HTMLElement).textContent;
        if (label && label !== this.fieldTab.divider.name) {
            this.editTab.emit({
                ...this.fieldTab.divider,
                name: label
            });
        } else {
            this.label = this.fieldTab.divider.name;
        }
    }

    /**
     * Trigger confirmation dialog to remove Tab Divider
     * @param {MouseEvent} $event
     * @memberof ContentTypeFieldsTabComponent
     */
    removeItem($event: MouseEvent): void {
        $event.stopPropagation();

        this.dotDialogService.confirm({
            accept: () => {
                this.removeTab.emit(this.fieldTab);
            },
            header: `${this.dotMessageService.get(
                'contenttypes.action.delete'
            )} ${this.dotMessageService.get('contenttypes.content.field')}`,
            message: this.dotMessageService.get(
                'contenttypes.confirm.message.delete.field',
                this.fieldTab.divider.name
            ),
            footerLabel: {
                accept: this.dotMessageService.get('contenttypes.action.delete'),
                reject: this.dotMessageService.get('contenttypes.action.cancel')
            }
        });
    }
}
