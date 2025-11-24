import { Validators } from '@angular/forms';

import { validateDateDefaultValue } from './validators';

import {
    CategoriesPropertyComponent,
    CheckboxPropertyComponent,
    DataTypePropertyComponent,
    DefaultValuePropertyComponent,
    HintPropertyComponent,
    NamePropertyComponent,
    RegexCheckPropertyComponent,
    ValuesPropertyComponent,
    NewRenderModePropertyComponent
} from '../content-type-fields-properties-form/field-properties';
import { DotRelationshipsPropertyComponent } from '../content-type-fields-properties-form/field-properties/dot-relationships-property/dot-relationships-property.component';
import { validateRelationship } from '../content-type-fields-properties-form/field-properties/dot-relationships-property/services/validators/dot-relationship-validator';
import { noWhitespaceValidator } from '../content-type-fields-properties-form/field-properties/dot-relationships-property/services/validators/no-whitespace-validator';

export const PROPERTY_INFO = {
    categories: {
        component: CategoriesPropertyComponent,
        defaultValue: '',
        order: 2,
        validations: [Validators.required]
    },
    dataType: {
        component: DataTypePropertyComponent,
        defaultValue: '',
        order: 1,
        disabledInEdit: true
    },
    defaultValue: {
        component: DefaultValuePropertyComponent,
        defaultValue: '',
        order: 4,
        validations: [validateDateDefaultValue]
    },
    hint: {
        component: HintPropertyComponent,
        defaultValue: '',
        order: 5
    },
    indexed: {
        component: CheckboxPropertyComponent,
        defaultValue: false,
        order: 9
    },
    listed: {
        component: CheckboxPropertyComponent,
        defaultValue: false,
        order: 10
    },
    name: {
        component: NamePropertyComponent,
        defaultValue: '',
        order: 0,
        validations: [Validators.required, noWhitespaceValidator]
    },
    regexCheck: {
        component: RegexCheckPropertyComponent,
        defaultValue: '',
        order: 6
    },
    required: {
        component: CheckboxPropertyComponent,
        defaultValue: false,
        order: 7
    },
    searchable: {
        component: CheckboxPropertyComponent,
        defaultValue: false,
        order: 8
    },
    unique: {
        component: CheckboxPropertyComponent,
        defaultValue: false,
        order: 11
    },
    values: {
        component: ValuesPropertyComponent,
        defaultValue: '',
        order: 3,
        validations: [Validators.required]
    },
    relationships: {
        component: DotRelationshipsPropertyComponent,
        defaultValue: {
            cardinality: 0
        },
        order: 6,
        validations: [validateRelationship]
    },
    newRenderMode: {
        component: NewRenderModePropertyComponent,
        defaultValue: 'false',
        order: 1
    }
};
