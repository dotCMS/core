import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
    selector: 'dotcms-input-fields',
    templateUrl: './input-field.component.html',
    styleUrls: ['./input-field.component.scss']
})
export class InputFieldComponent {
    @Input() label = 'Name';
    @Input() placeholder = 'Enter Name';
    @Input() type = 'text';
    @Input() value = '';
    @Output() valueChange = new EventEmitter<string>();
}
