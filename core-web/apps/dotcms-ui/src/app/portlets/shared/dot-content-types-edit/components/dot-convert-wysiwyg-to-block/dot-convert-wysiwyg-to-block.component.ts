import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
    selector: 'dot-convert-wysiwyg-to-block',
    templateUrl: './dot-convert-wysiwyg-to-block.component.html',
    styleUrls: ['./dot-convert-wysiwyg-to-block.component.scss'],
    standalone: false
})
export class DotConvertWysiwygToBlockComponent {
    @Input() currentFieldType;

    @Output() convert = new EventEmitter<MouseEvent>();

    accept = false;
}
