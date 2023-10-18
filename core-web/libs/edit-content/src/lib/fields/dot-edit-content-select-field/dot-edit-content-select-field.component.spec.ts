import { Spectator, createComponentFactory } from '@ngneat/spectator';

import { CommonModule } from '@angular/common';
import {
    ControlContainer,
    FormControl,
    FormGroup,
    FormGroupDirective,
    ReactiveFormsModule
} from '@angular/forms';

import { DropdownModule } from 'primeng/dropdown';

import { DotFieldRequiredDirective } from '@dotcms/ui';

import { DotEditContentSelectFieldComponent } from './dot-edit-content-select-field.component';

const SELECT_FIELD_MOCK = {
    clazz: 'com.dotcms.contenttype.model.field.ImmutableSelectField',
    contentTypeId: '40e0cb1b57b3b1b7ec34191e942316d5',
    dataType: 'TEXT',
    defaultValue: '123-ad',
    fieldType: 'Select',
    fieldTypeLabel: 'Select',
    fieldVariables: [],
    fixed: false,
    forceIncludeInApi: false,
    iDate: 1697579843000,
    id: 'a6f33b8941b6c06c8ab36e44c4bf6500',
    indexed: false,
    listed: false,
    modDate: 1697661626000,
    name: 'selectNormal',
    readOnly: false,
    required: false,
    searchable: false,
    sortOrder: 3,
    unique: false,
    values: 'Option 1|Test,1\r\nOption 2|2\r\nOption 3|3\r\n123-ad\r\nrules and weird code',
    variable: 'selectNormal'
};
const FORM_GROUP_MOCK = new FormGroup({
    selectNormal: new FormControl('')
});
const FORM_GROUP_DIRECTIVE_MOCK: FormGroupDirective = new FormGroupDirective([], []);
FORM_GROUP_DIRECTIVE_MOCK.form = FORM_GROUP_MOCK;

describe('DotEditContentSelectFieldComponent', () => {
    let spectator: Spectator<DotEditContentSelectFieldComponent>;

    const createComponent = createComponentFactory({
        component: DotEditContentSelectFieldComponent,
        imports: [CommonModule, DropdownModule, ReactiveFormsModule, DotFieldRequiredDirective],
        componentViewProviders: [
            { provide: ControlContainer, useValue: FORM_GROUP_DIRECTIVE_MOCK }
        ],
        providers: [FormGroupDirective]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                field: SELECT_FIELD_MOCK
            }
        });
    });

    it('should have a options array', () => {
        const expectedList = [
            {
                label: 'Option 1',
                value: 'Test,1'
            },
            {
                label: 'Option 2',
                value: '2'
            },
            {
                label: 'Option 3',
                value: '3'
            },
            {
                label: '123-ad',
                value: '123-ad'
            },
            {
                label: 'rules and weird code',
                value: 'rules and weird code'
            }
        ];
        spectator.detectChanges();
        expect(spectator.component.options).toEqual(expectedList);
    });
});
