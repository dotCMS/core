import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator';

import { CommonModule } from '@angular/common';
import {
    ControlContainer,
    FormControl,
    FormGroup,
    FormGroupDirective,
    ReactiveFormsModule
} from '@angular/forms';

import { InputTextModule } from 'primeng/inputtext';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { DotEditContentFieldComponent } from './dot-edit-content-field.component';

export const FIELD_MOCK: DotCMSContentTypeField = {
    clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
    contentTypeId: 'd46d6404125ac27e6ab68fad09266241',
    dataType: 'TEXT',
    fieldType: 'Text',
    fieldTypeLabel: 'Text',
    fieldVariables: [],
    fixed: false,
    iDate: 1696896882000,
    id: 'c3b928bc2b59fc22c67022de4dd4b5c4',
    indexed: false,
    listed: false,
    hint: 'A helper text',
    modDate: 1696896882000,
    name: 'testVariable',
    readOnly: false,
    required: false,
    searchable: false,
    sortOrder: 2,
    unique: false,
    variable: 'testVariable'
};

const FORM_GROUP_MOCK = new FormGroup({
    testVariable: new FormControl('')
});
const FORM_GROUP_DIRECTIVE_MOCK: FormGroupDirective = new FormGroupDirective([], []);
FORM_GROUP_DIRECTIVE_MOCK.form = FORM_GROUP_MOCK;

describe('DotFieldComponent', () => {
    let spectator: Spectator<DotEditContentFieldComponent>;
    const createComponent = createComponentFactory({
        component: DotEditContentFieldComponent,
        imports: [DotEditContentFieldComponent, CommonModule, ReactiveFormsModule, InputTextModule],
        providers: [
            {
                provide: ControlContainer,
                useValue: FORM_GROUP_DIRECTIVE_MOCK
            }
        ]
    });

    beforeEach(async () => {
        spectator = createComponent({
            props: {
                field: FIELD_MOCK
            }
        });
    });

    it('should render the label', () => {
        spectator.detectChanges();
        const label = spectator.query(byTestId(`label-${FIELD_MOCK.variable}`));
        expect(label?.textContent).toContain(FIELD_MOCK.fieldTypeLabel);
    });

    it('should render the hint', () => {
        spectator.detectChanges();
        const hint = spectator.query(byTestId(`hint-${FIELD_MOCK.variable}`));
        expect(hint?.textContent).toContain(FIELD_MOCK.hint);
    });
});
