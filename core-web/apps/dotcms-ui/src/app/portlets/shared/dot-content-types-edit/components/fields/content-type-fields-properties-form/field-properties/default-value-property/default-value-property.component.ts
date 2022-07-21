import { Component, OnInit } from '@angular/core';
import { FieldProperty } from '../field-properties.model';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { UntypedFormGroup } from '@angular/forms';

@Component({
    selector: 'dot-default-value-property',
    templateUrl: './default-value-property.component.html'
})
export class DefaultValuePropertyComponent implements OnInit {
    property: FieldProperty;
    group: UntypedFormGroup;
    errorLabel: string;
    private errorLabelsMap = new Map<string, string>();

    constructor(private dotMessageService: DotMessageService) {}

    ngOnInit(): void {
        this.setErrorLabelMap();
        this.errorLabel = this.getErrorLabel(this.property.field.clazz);
    }

    private getErrorLabel(clazz: string): string {
        return this.errorLabelsMap.get(clazz)
            ? this.errorLabelsMap.get(clazz)
            : this.errorLabelsMap.get('default');
    }

    private setErrorLabelMap(): void {
        this.errorLabelsMap.set(
            'com.dotcms.contenttype.model.field.ImmutableDateField',
            this.dotMessageService.get(
                'contenttypes.field.properties.default_value.immutable_date.error.format'
            )
        );
        this.errorLabelsMap.set(
            'com.dotcms.contenttype.model.field.ImmutableDateTimeField',
            this.dotMessageService.get(
                'contenttypes.field.properties.default_value.immutable_date_time.error.format'
            )
        );
        this.errorLabelsMap.set(
            'default',
            this.dotMessageService.get('contenttypes.field.properties.default_value.error.format')
        );
    }
}
