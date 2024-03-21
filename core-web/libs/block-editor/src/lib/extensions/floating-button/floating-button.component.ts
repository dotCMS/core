import { Component, EventEmitter, Input, Output } from '@angular/core';

import { FileStatus } from '@dotcms/data-access';

@Component({
    selector: 'dot-floating-button',
    templateUrl: './floating-button.component.html',
    styleUrls: ['./floating-button.component.scss']
})
export class FloatingButtonComponent {
    @Input() label = '';
    @Input() isLoading = false;
    @Output() byClick: EventEmitter<void> = new EventEmitter();

    public status = FileStatus;

    get title() {
        if (!this.label) return '';

        return this.label[0].toUpperCase() + this.label?.substring(1).toLowerCase();
    }

    get isCompleted() {
        return this.label === this.status.COMPLETED;
    }
}
