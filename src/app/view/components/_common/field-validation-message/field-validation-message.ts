/*
- TODO: add options for more than one message, like: required and email valid
- TODO: maybe crawl the html to find the form parent and save one @Input
*/

import { Component, Input } from '@angular/core';
import { NgControl } from '@angular/forms';

@Component({
  selector: 'field-validation-message',
  styleUrls: ['./field-validation-message.scss'],
  templateUrl: './field-validation-message.html'
})
export class FieldValidationMessageComponent {
  @Input() field: NgControl;
  @Input() message: string;

  constructor() { }
}
