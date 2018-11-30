import { Component, OnInit } from '@angular/core';
import { FieldProperty } from '../field-properties.model';
import { DotMessageService } from '@services/dot-messages-service';
import { FormGroup } from '@angular/forms';
import { take } from 'rxjs/operators';

@Component({
    selector: 'dot-checkbox-property',
    templateUrl: './checkbox-property.component.html'
})
export class CheckboxPropertyComponent implements OnInit {
    property: FieldProperty;
    group: FormGroup;

    i18nMessages: {
        [key: string]: string;
    } = {};

    private readonly map = {
        indexed: 'contenttypes.field.properties.system_indexed.label',
        listed: 'contenttypes.field.properties.listed.label',
        required: 'contenttypes.field.properties.required.label',
        searchable: 'contenttypes.field.properties.user_searchable.label',
        unique: 'contenttypes.field.properties.unique.label'
    };

    constructor(public dotMessageService: DotMessageService) {}

    ngOnInit() {
        this.dotMessageService
            .getMessages([
                'contenttypes.field.properties.required.label',
                'contenttypes.field.properties.user_searchable.label',
                'contenttypes.field.properties.system_indexed.label',
                'contenttypes.field.properties.listed.label',
                'contenttypes.field.properties.unique.label'
            ])
            .pipe(take(1))
            .subscribe((res) => {
                this.i18nMessages = res;
            });
    }

    setCheckboxLabel(field): string {
        return this.map[field] || field;
    }
}
