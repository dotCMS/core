import { Spectator, createComponentFactory } from '@ngneat/spectator';

import { CommonModule } from '@angular/common';
import {
    ControlContainer,
    FormControl,
    FormGroup,
    FormGroupDirective,
    ReactiveFormsModule
} from '@angular/forms';

import { RadioButtonModule } from 'primeng/radiobutton';

import { DotFieldRequiredDirective } from '@dotcms/ui';

import { DotEditContentRadioFieldComponent } from './dot-edit-content-radio-field.component';

export const FIELD_RADIO_MOCK = {
    clazz: 'com.dotcms.contenttype.model.field.ImmutableRadioField',
    contentTypeId: '40e0cb1b57b3b1b7ec34191e942316d5',
    dataType: 'TEXT',
    fieldType: 'Radio',
    fieldTypeLabel: 'Radio',
    fieldVariables: [],
    fixed: false,
    forceIncludeInApi: false,
    iDate: 1697598313000,
    id: '824b4e9907fe4f450ced438598cc0ce8',
    indexed: false,
    listed: false,
    modDate: 1697662296000,
    name: 'radio',
    readOnly: false,
    required: false,
    searchable: false,
    sortOrder: 8,
    unique: false,
    values: 'Uno|uno\r\nDos|dos',
    variable: 'radio'
};

const FORM_GROUP_MOCK = new FormGroup({
    radio: new FormControl('')
});
const FORM_GROUP_DIRECTIVE_MOCK: FormGroupDirective = new FormGroupDirective([], []);
FORM_GROUP_DIRECTIVE_MOCK.form = FORM_GROUP_MOCK;

describe('DotEditContentRadioFieldComponent', () => {
    let spectator: Spectator<DotEditContentRadioFieldComponent>;

    const createComponent = createComponentFactory({
        component: DotEditContentRadioFieldComponent,
        imports: [CommonModule, RadioButtonModule, ReactiveFormsModule, DotFieldRequiredDirective],
        componentViewProviders: [
            { provide: ControlContainer, useValue: FORM_GROUP_DIRECTIVE_MOCK }
        ],
        providers: [FormGroupDirective]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                field: FIELD_RADIO_MOCK
            }
        });
    });
    it('should have a options array', () => {
        const expectedList = [
            {
                label: 'Uno',
                value: 'uno'
            },
            {
                label: 'Dos',
                value: 'dos'
            }
        ];
        spectator.detectChanges();
        expect(spectator.component.options).toEqual(expectedList);
    });
});
