import { Component, OnInit, inject } from '@angular/core';
import { UntypedFormGroup } from '@angular/forms';

import { DotMessageService } from '@dotcms/data-access';

import { FieldProperty } from '../field-properties.model';

@Component({
    selector: 'dot-default-value-property',
    templateUrl: './default-value-property.component.html',
    standalone: false
})
export class DefaultValuePropertyComponent implements OnInit {
    private dotMessageService = inject(DotMessageService);

    property: FieldProperty;
    group: UntypedFormGroup;
    errorLabel: string;
    private errorLabelsMap = new Map<string, string>();

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
