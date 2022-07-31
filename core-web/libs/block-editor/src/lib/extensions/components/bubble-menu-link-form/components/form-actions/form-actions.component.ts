import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
    selector: 'dotcms-form-actions',
    templateUrl: './form-actions.component.html',
    styleUrls: ['./form-actions.component.scss']
})
export class FormActionsComponent {
    @Output() remove: EventEmitter<boolean> = new EventEmitter(false);
    @Output() hide: EventEmitter<boolean> = new EventEmitter(false);

    @Input() link = '';

    copy() {
        navigator.clipboard
            .writeText(this.link)
            .then(() => this.hide.emit(true))
            .catch(() => alert('Could not copy link'));
    }
}
