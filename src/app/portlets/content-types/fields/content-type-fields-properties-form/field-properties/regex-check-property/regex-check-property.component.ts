import { Component, Input, ViewChild } from '@angular/core';
import { BaseComponent } from '../../../../../../view/components/_common/_base/base-component';
import { MessageService } from '../../../../../../api/services/messages-service';
import { FieldProperty } from '../field-properties.model';
import { FormGroup, FormControl } from '@angular/forms';

@Component({
    selector: 'regex-check-property',
    templateUrl: './regex-check-property.component.html',
})
export class RegexCheckPropertyComponent extends BaseComponent {

    regexCheckTempletes = [];

    property: FieldProperty;
    group: FormGroup;

    constructor(public messageService: MessageService) {
        super(
            [
                'contenttypes.field.properties.validation_regex.label',
                'contenttypes.field.properties.validation_regex.values.select',
                'contenttypes.field.properties.validation_regex.values.email',
                'contenttypes.field.properties.validation_regex.values.numbers_only',
                'contenttypes.field.properties.validation_regex.values.letters_only',
                'contenttypes.field.properties.validation_regex.values.alphanumeric',
                'contenttypes.field.properties.validation_regex.values.us_zip_code',
                'contenttypes.field.properties.validation_regex.values.us_phone',
                'contenttypes.field.properties.validation_regex.values.url_pattern',
                'contenttypes.field.properties.validation_regex.values.no_html'
            ], messageService);
    }

    templateSelect(event): void {
        this.group.controls[this.property.name].setValue(event.value);
    }

    /**
     * Callback call from BaseComponent when the messages are received.
     * @memberOf ContentTypesPortletComponent
     */
    onMessage(): void {
        this.regexCheckTempletes = [
            {
                label: this.i18nMessages['contenttypes.field.properties.validation_regex.values.select'],
                value: ''
            },
            {
                label: this.i18nMessages['contenttypes.field.properties.validation_regex.values.email'],
                value: '^([a-zA-Z0-9]+[a-zA-Z0-9._%+-]*@(?:[a-zA-Z0-9-]+\.)+[a-zA-Z]{2,4})$'
            },
            {
                label: this.i18nMessages['contenttypes.field.properties.validation_regex.values.numbers_only'],
                value: '[0-9]*'
            },
            {
                label: this.i18nMessages['contenttypes.field.properties.validation_regex.values.letters_only'],
                value: '[a-zA-Z\s]*'
            },
            {
                label: this.i18nMessages['contenttypes.field.properties.validation_regex.values.alphanumeric'],
                value: '[0-9a-zA-Z\s]*'
            },
            {
                label: this.i18nMessages['contenttypes.field.properties.validation_regex.values.us_zip_code'],
                value: '(^\d{5}$)|(^\d{5}-\d{4}$)'
            },
            {
                label: this.i18nMessages['contenttypes.field.properties.validation_regex.values.us_phone'],
                value: '^\(?[1-9]\d{2}[\)\-]\s?\d{3}\-\d{4}$'
            },
            {
                label: this.i18nMessages['contenttypes.field.properties.validation_regex.values.url_pattern'],
                // tslint:disable-next-line:max-line-length
                value: '^((http|ftp|https):\/\/w{3}[\d]*.|(http|ftp|https):\/\/|w{3}[\d]*.)([\w\d\._\-#\(\)\[\]\,;:]+@[\w\d\._\-#\(\)\[\]\,;:])?([a-z0-9]+.)*[a-z\-0-9]+.([a-z]{2,3})?[a-z]{2,6}(:[0-9]+)?(\/[\/a-zA-Z0-9\._\-,\%\s]+)*(\/|\?[a-z0-9=%&\.\-,#]+)?$'
            },
            {
                label: this.i18nMessages['contenttypes.field.properties.validation_regex.values.no_html'],
                value: '[^(<[.\n]+>)]*'
            }
        ];
    }
}
