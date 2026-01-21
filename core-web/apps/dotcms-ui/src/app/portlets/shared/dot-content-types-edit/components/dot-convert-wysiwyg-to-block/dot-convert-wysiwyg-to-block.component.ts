import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
    selector: 'dot-convert-wysiwyg-to-block',
    templateUrl: './dot-convert-wysiwyg-to-block.component.html',
    standalone: false,
    host: {
        class: 'mt-6 block border border-gray-300 p-4 rounded-sm'
    }
})
export class DotConvertWysiwygToBlockComponent {
    @Input() currentFieldType;

    @Output() convert = new EventEmitter<MouseEvent>();

    accept = false;
}
