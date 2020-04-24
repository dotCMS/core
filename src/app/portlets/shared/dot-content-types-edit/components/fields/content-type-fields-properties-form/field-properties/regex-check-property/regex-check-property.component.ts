import { Component, OnInit } from '@angular/core';
import { DotMessageService } from '@services/dot-messages-service';
import { FieldProperty } from '../field-properties.model';
import { FormGroup } from '@angular/forms';
import { take } from 'rxjs/operators';

export interface RegexTemplate {
    label: string;
     value: string;
}
@Component({
    selector: 'dot-regex-check-property',
    templateUrl: './regex-check-property.component.html',
    styleUrls: ['./regex-check-property.component.scss']
})
export class RegexCheckPropertyComponent implements OnInit {
    regexCheckTempletes: RegexTemplate[] = [];

    property: FieldProperty;
    group: FormGroup;

    i18nMessages: {
        [key: string]: string;
    } = {};

    constructor(public dotMessageService: DotMessageService) {}

    ngOnInit() {
        this.dotMessageService
            .getMessages([
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
            ])
            .pipe(take(1))
            .subscribe((res) => {
                this.i18nMessages = res;

                this.regexCheckTempletes = [
                    {
                        label: this.i18nMessages[
                            'contenttypes.field.properties.validation_regex.values.select'
                        ],
                        value: ''
                    },
                    {
                        label: this.i18nMessages[
                            'contenttypes.field.properties.validation_regex.values.email'
                        ],
                        value: '^([a-zA-Z0-9]+[a-zA-Z0-9._%+-]*@(?:[a-zA-Z0-9-]+.)+[a-zA-Z]{2,4})$'
                    },
                    {
                        label: this.i18nMessages[
                            'contenttypes.field.properties.validation_regex.values.numbers_only'
                        ],
                        value: '[0-9]*'
                    },
                    {
                        label: this.i18nMessages[
                            'contenttypes.field.properties.validation_regex.values.letters_only'
                        ],
                        value: '[a-zA-Zs]*'
                    },
                    {
                        label: this.i18nMessages[
                            'contenttypes.field.properties.validation_regex.values.alphanumeric'
                        ],
                        value: '[0-9a-zA-Zs]*'
                    },
                    {
                        label: this.i18nMessages[
                            'contenttypes.field.properties.validation_regex.values.us_zip_code'
                        ],
                        value: '(^d{5}$)|(^d{5}-d{4}$)'
                    },
                    {
                        label: this.i18nMessages[
                            'contenttypes.field.properties.validation_regex.values.us_phone'
                        ],
                        value: '^(?[1-9]d{2}[)-]s?d{3}-d{4}$'
                    },
                    {
                        label: this.i18nMessages[
                            'contenttypes.field.properties.validation_regex.values.url_pattern'
                        ],
                        value:
                            // tslint:disable-next-line:max-line-length
                            '^((http|ftp|https):\\/\\/w{3}[d]*.|(http|ftp|https):\\/\\/|w{3}[d]*.)([wd._\\-#\\(\\)\\[\\],;:]+@[wd._\\-#\\(\\)\\[\\],;:])?([a-z0-9]+.)*[a-z-0-9]+.([a-z]{2,3})?[a-z]{2,6}(:[0-9]+)?(\\/[\\/a-zA-Z0-9._\\-,%s]+)*(\\/|\\?[a-z0-9=%&.\\-,#]+)?$'
                    },
                    {
                        label: this.i18nMessages[
                            'contenttypes.field.properties.validation_regex.values.no_html'
                        ],
                        value: '[^(<[.\n]+>)]*'
                    }
                ];
            });
    }

    templateSelect(event): void {
        this.group.controls[this.property.name].setValue(event.value);
    }
}
