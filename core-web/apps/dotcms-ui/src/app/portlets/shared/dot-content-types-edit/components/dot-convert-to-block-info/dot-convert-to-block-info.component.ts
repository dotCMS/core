import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
    selector: 'dot-convert-to-block-info',
    templateUrl: './dot-convert-to-block-info.component.html',
    standalone: false,
    host: {
        class: 'flex justify-center items-center gap-1 px-6 py-2 bg-primary-100/50 text-primary-900 rounded-sm'
    }
})
export class DotConvertToBlockInfoComponent {
    @Input() currentFieldType;
    @Output() action = new EventEmitter<MouseEvent>();
    @Input() currentField;
}
