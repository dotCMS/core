import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
    selector: 'dot-convert-to-block-info',
    templateUrl: './dot-convert-to-block-info.component.html',
    styleUrls: ['./dot-convert-to-block-info.component.scss'],
    standalone: false
})
export class DotConvertToBlockInfoComponent {
    @Input() currentFieldType;
    @Output() action = new EventEmitter<MouseEvent>();
    @Input() currentField;
}
