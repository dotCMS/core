import { Component, Input, OnInit, Output, EventEmitter } from '@angular/core';
import { DotContentTypeField } from '../models';
import { DotMessageService } from '@services/dot-messages-service';
import { DotContentTypeLayoutDivider } from '@portlets/content-types/fields/models/dot-content-type-layout-divider.model';
import { DotAlertConfirmService } from '@services/dot-alert-confirm/dot-alert-confirm.service';
import { take } from 'rxjs/operators';

/**
 * Display Tab Field
 *
 * @export
 * @class ContentTypeFieldsTabComponent
 */
@Component({
    selector: 'dot-content-type-fields-tab',
    styleUrls: ['./content-type-fields-tab.component.scss'],
    templateUrl: './content-type-fields-tab.component.html'
})
export class ContentTypeFieldsTabComponent implements OnInit {
    @Input()
    fieldTab: DotContentTypeLayoutDivider;

    @Output()
    editTab: EventEmitter<DotContentTypeField> = new EventEmitter();

    @Output()
    removeTab: EventEmitter<DotContentTypeLayoutDivider> = new EventEmitter();

    i18nMessages: any = {};
    label: string;

    constructor(
        private dotMessageService: DotMessageService,
        private dotDialogService: DotAlertConfirmService
    ) {}

    ngOnInit() {
        this.dotMessageService
            .getMessages([
                'contenttypes.action.delete',
                'contenttypes.confirm.message.delete.field',
                'contenttypes.content.field',
                'contenttypes.action.cancel'
            ])
            .pipe(take(1))
            .subscribe((res) => {
                this.i18nMessages = res;
            });
        this.label = this.fieldTab.divider.name;
    }

    /**
     * Trigger the editTab event to change tab label
     * @memberof ContentTypeFieldsTabComponent
     */
    changeLabel($event: any): void {
        $event.stopPropagation();
        $event.preventDefault();
        const label = $event.target.textContent;
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
            header: `${this.i18nMessages['contenttypes.action.delete']} ${
                this.i18nMessages['contenttypes.content.field']
            }`,
            message: this.dotMessageService.get(
                'contenttypes.confirm.message.delete.field',
                this.fieldTab.divider.name
            ),
            footerLabel: {
                accept: this.i18nMessages['contenttypes.action.delete'],
                reject: this.i18nMessages['contenttypes.action.cancel']
            }
        });
    }
}
