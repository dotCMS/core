import { Component, OnInit } from '@angular/core';
import { FieldProperty } from '../field-properties.model';
import { DotMessageService } from '@services/dot-messages-service';
import { FormGroup } from '@angular/forms';

@Component({
    selector: 'dot-default-value-property',
    templateUrl: './default-value-property.component.html'
})
export class DefaultValuePropertyComponent implements OnInit {
    property: FieldProperty;
    group: FormGroup;
    errorLabel: string;
    private errorLabelsMap = new Map<string, string>();

    constructor(public dotMessageService: DotMessageService) {}

    ngOnInit(): void {
        this.dotMessageService
            .getMessages([
                'contenttypes.field.properties.default_value.label',
                'contenttypes.field.properties.default_value.error.format',
                'contenttypes.field.properties.default_value.immutable_date.error.format',
                'contenttypes.field.properties.default_value.immutable_date_time.error.format'
            ])
            .subscribe((messages: any) => {
                this.setErrorLabelMap(messages);
                this.errorLabel = this.getErrorLabel(this.property.field.clazz);
            });
    }

    private getErrorLabel(clazz: string): string {
        return this.errorLabelsMap.get(clazz) ? this.errorLabelsMap.get(clazz) : this.errorLabelsMap.get('default');
    }

    private setErrorLabelMap(messages: string[]): void {
        this.errorLabelsMap.set(
            'com.dotcms.contenttype.model.field.ImmutableDateField',
            messages['contenttypes.field.properties.default_value.immutable_date.error.format']
        );
        this.errorLabelsMap.set(
            'com.dotcms.contenttype.model.field.ImmutableDateTimeField',
            messages['contenttypes.field.properties.default_value.immutable_date_time.error.format']
        );
        this.errorLabelsMap.set('default', messages['contenttypes.field.properties.default_value.error.format']);
    }
}
