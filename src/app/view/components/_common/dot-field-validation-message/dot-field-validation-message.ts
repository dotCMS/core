/*
- TODO: add options for more than one message, like: required and email valid
- TODO: maybe crawl the html to find the form parent and save one @Input
*/

import { Component, Input } from '@angular/core';
import { FormControl } from '@angular/forms';

@Component({
    selector: 'dot-field-validation-message',
    styleUrls: ['./dot-field-validation-message.scss'],
    templateUrl: './dot-field-validation-message.html'
})
export class DotFieldValidationMessageComponent {
    @Input()
    field: FormControl;

    @Input()
    message: string;

    constructor() {}
}
