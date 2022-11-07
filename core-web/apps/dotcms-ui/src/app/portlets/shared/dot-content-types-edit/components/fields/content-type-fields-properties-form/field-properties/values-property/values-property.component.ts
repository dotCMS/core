import { Component, ViewChild } from '@angular/core';
import { FieldProperty } from '../field-properties.model';
import { UntypedFormGroup } from '@angular/forms';
import { DotTextareaContentComponent } from '@components/_common/dot-textarea-content/dot-textarea-content.component';

@Component({
    selector: 'dot-values-property',
    templateUrl: './values-property.component.html',
    styleUrls: ['./values-property.component.scss']
})
export class ValuesPropertyComponent {
    @ViewChild('value') value: DotTextareaContentComponent;
    property: FieldProperty;
    group: UntypedFormGroup;
    helpText: string;

    private validTextHelperClazz = [
        'com.dotcms.contenttype.model.field.ImmutableRadioField',
        'com.dotcms.contenttype.model.field.ImmutableCheckboxField',
        'com.dotcms.contenttype.model.field.ImmutableMultiSelectField',
        'com.dotcms.contenttype.model.field.ImmutableSelectField'
    ];

    /**
     * Checks if helper should show, based on the clazz property.
     *
     * @returns {Boolean}
     * @memberof ValuesPropertyComponent
     */
    isValidHelperClass(): boolean {
        return this.validTextHelperClazz.includes(this.property.field.clazz);
    }
}
