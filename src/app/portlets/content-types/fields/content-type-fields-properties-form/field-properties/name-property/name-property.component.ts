import { Component, OnInit } from '@angular/core';
import { FormGroup } from '@angular/forms/forms';
import { FieldProperty } from '../field-properties.model';
import { DotMessageService } from '@services/dot-messages-service';

@Component({
    selector: 'dot-name-property',
    templateUrl: './name-property.component.html'
})
export class NamePropertyComponent implements OnInit {
    property: FieldProperty;
    group: FormGroup;

    i18nMessages: {
        [key: string]: string;
    } = {};

    constructor(public dotMessageService: DotMessageService) {}

    ngOnInit(): void {
        this.dotMessageService
            .getMessages([
                'contenttypes.field.properties.name.label',
                'contenttypes.field.properties.name.error.required',
                'contenttypes.field.properties.name.variable'
            ])
            .subscribe((res) => {
                this.i18nMessages = res;
            });
    }
}
