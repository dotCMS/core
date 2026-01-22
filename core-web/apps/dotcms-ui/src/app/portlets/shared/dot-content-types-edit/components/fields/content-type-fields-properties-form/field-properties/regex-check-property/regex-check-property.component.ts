import { Component, OnInit, inject } from '@angular/core';
import { UntypedFormGroup } from '@angular/forms';

import { DotMessageService } from '@dotcms/data-access';

import { FieldProperty } from '../field-properties.model';

export interface RegexTemplate {
    label: string;
    value: string;
}

@Component({
    selector: 'dot-regex-check-property',
    templateUrl: './regex-check-property.component.html',
    standalone: false
})
export class RegexCheckPropertyComponent implements OnInit {
    private dotMessageService = inject(DotMessageService);

    regexCheckTemplates: RegexTemplate[] = [];

    property: FieldProperty;
    group: UntypedFormGroup;

    ngOnInit() {
        this.regexCheckTemplates = [
            {
                label: this.dotMessageService.get(
                    'contenttypes.field.properties.validation_regex.values.select'
                ),
                value: ''
            },
            {
                label: this.dotMessageService.get(
                    'contenttypes.field.properties.validation_regex.values.email'
                ),
                value: '^([a-zA-Z0-9]+[a-zA-Z0-9._%+-]*@(?:[a-zA-Z0-9-]+.)+[a-zA-Z]{2,4})$'
            },
            {
                label: this.dotMessageService.get(
                    'contenttypes.field.properties.validation_regex.values.numbers_only'
                ),
                value: '[0-9]*'
            },
            {
                label: this.dotMessageService.get(
                    'contenttypes.field.properties.validation_regex.values.letters_only'
                ),
                value: '[a-zA-Zs]*'
            },
            {
                label: this.dotMessageService.get(
                    'contenttypes.field.properties.validation_regex.values.alphanumeric'
                ),
                value: '[0-9a-zA-Zs]*'
            },
            {
                label: this.dotMessageService.get(
                    'contenttypes.field.properties.validation_regex.values.us_zip_code'
                ),
                value: '(^d{5}$)|(^d{5}-d{4}$)'
            },
            {
                label: this.dotMessageService.get(
                    'contenttypes.field.properties.validation_regex.values.us_phone'
                ),
                value: '^(?[1-9]d{2}[)-]s?d{3}-d{4}$'
            },
            {
                label: this.dotMessageService.get(
                    'contenttypes.field.properties.validation_regex.values.url_pattern'
                ),
                value:
                    // tslint:disable-next-line:max-line-length
                    '^((http|ftp|https):\\/\\/w{3}[d]*.|(http|ftp|https):\\/\\/|w{3}[d]*.)([wd._\\-#\\(\\)\\[\\),;:]+@[wd._\\-#\\(\\)\\[\\),;:])?([a-z0-9]+.)*[a-z-0-9]+.([a-z]{2,3})?[a-z]{2,6}(:[0-9]+)?(\\/[\\/a-zA-Z0-9._\\-,%s]+)*(\\/|\\?[a-z0-9=%&.\\-,#]+)?$'
            },
            {
                label: this.dotMessageService.get(
                    'contenttypes.field.properties.validation_regex.values.no_html'
                ),
                value: '^[^<><|>]+$'
            }
        ];
    }

    templateSelect(event): void {
        this.group.controls[this.property.name].setValue(event.value);
    }
}
