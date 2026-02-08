import { Validators, ValidatorFn } from '@angular/forms';

import { CustomValidators } from '../validators/custom-validators';

export class ValidationResults {
    valid: boolean;

    constructor(valid: boolean) {
        this.valid = valid;
    }
}

/** @deprecated Use ValidationResults instead */
export const CwValidationResults = ValidationResults;

interface TypeConstraint {
    id: string;
    args: { [key: string]: unknown };
}

interface ValidatorDefinition {
    key: string;
    providerFn: (constraint: TypeConstraint) => ValidatorFn;
}

const VALIDATIONS: Record<string, ValidatorDefinition> = {
    maxLength: {
        key: 'maxLength',
        providerFn: (constraint: TypeConstraint) =>
            CustomValidators.maxLength(constraint.args['value'] as number)
    },
    maxValue: {
        key: 'maxValue',
        providerFn: (constraint: TypeConstraint) =>
            CustomValidators.max(constraint.args['value'] as number)
    },
    minLength: {
        key: 'minLength',
        providerFn: (constraint: TypeConstraint) =>
            CustomValidators.minLength(constraint.args['value'] as number)
    },
    minValue: {
        key: 'minValue',
        providerFn: (constraint: TypeConstraint) =>
            CustomValidators.min(constraint.args['value'] as number)
    },
    required: {
        key: 'required',
        providerFn: () => CustomValidators.required()
    }
};

export class DataTypeModel {
    private _vFns: ValidatorFn[];

    constructor(
        public id: string,
        public errorMessageKey: string,
        private _constraints: Record<string, TypeConstraint>,
        public defaultValue: string = null
    ) {}

    validators(): ValidatorFn[] {
        if (this._vFns == null) {
            this._vFns = [];
            Object.keys(VALIDATIONS).forEach((vDefKey) => {
                const vDef = VALIDATIONS[vDefKey];
                const constraint = this._constraints[vDef.key];
                if (constraint) {
                    const fn = vDef.providerFn(constraint);
                    this._vFns.push(fn);
                }
            });
        }

        return this._vFns;
    }

    validator(): ValidatorFn {
        return Validators.compose(this.validators());
    }
}

export class InputDefinition {
    private _vFns: ValidatorFn[];
    private _validator: ValidatorFn;

    static fromJson(json: Record<string, unknown>, name: string): InputDefinition {
        const typeId = (json.id || json.type) as string;
        let type = Registry[typeId];

        if (!type) {
            const msg = "No input definition registered for '" + typeId + "'. Using default.";
            console.error(msg, json);
            type = InputDefinition;
        }

        let dataType: DataTypeModel = null;
        const dataTypeJson = json.dataType as Record<string, unknown>;
        if (dataTypeJson) {
            dataType = new DataTypeModel(
                dataTypeJson.id as string,
                dataTypeJson.errorMessageKey as string,
                dataTypeJson.constraints as Record<string, TypeConstraint>,
                dataTypeJson.defaultValue as string
            );
        }

        return new type(json, typeId, name, json.placeholder as string, dataType);
    }

    constructor(
        public json: Record<string, unknown>,
        public type: string,
        public name: string,
        public placeholder: string,
        public dataType: DataTypeModel,
        private _validators: ValidatorFn[] = []
    ) {}

    validators(): ValidatorFn[] {
        if (this._vFns == null) {
            this._vFns = this.dataType.validators().concat(this._validators);
        }

        return this._vFns;
    }

    validator(): ValidatorFn {
        if (this._validator == null) {
            this._vFns = this.validators();
            if (this._vFns && this._vFns.length) {
                this._validator = Validators.compose(this._vFns);
            } else {
                this._validator = () => null;
            }
        }

        return this._validator;
    }

    verify(value: unknown): { [key: string]: boolean } {
        return this.validator()({ value } as never);
    }
}

/** @deprecated Use InputDefinition instead */
export const CwInputDefinition = InputDefinition;

export class SpacerInputDefinition extends InputDefinition {
    protected flex: number;

    constructor(flex: number) {
        super({}, 'spacer', null, null, null);
        this.flex = flex;
    }
}

/** @deprecated Use SpacerInputDefinition instead */
export const CwSpacerInputDefinition = SpacerInputDefinition;

export class DropdownInputModel extends InputDefinition {
    options: { [key: string]: unknown };
    allowAdditions: boolean;
    minSelections = 0;
    maxSelections = 1;
    selected: unknown[] = [];
    i18nBaseKey: string;

    static createValidators(json: Record<string, unknown>): ValidatorFn[] {
        return [
            CustomValidators.minSelections((json.minSelections as number) || 0),
            CustomValidators.maxSelections((json.maxSelections as number) || 1)
        ];
    }

    constructor(
        json: Record<string, unknown>,
        type: string,
        name: string,
        placeholder: string,
        dataType: DataTypeModel
    ) {
        super(json, type, name, placeholder, dataType, DropdownInputModel.createValidators(json));
        this.options = json.options as { [key: string]: unknown };
        this.allowAdditions = json.allowAdditions as boolean;
        this.minSelections = json.minSelections as number;
        this.maxSelections = json.maxSelections as number;
        const dataTypeJson = json.dataType as Record<string, unknown>;
        const defV = dataTypeJson?.defaultValue;
        this.selected = defV == null || defV === '' ? [] : [defV];
    }
}

/** @deprecated Use DropdownInputModel instead */
export const CwDropdownInputModel = DropdownInputModel;

export class RestDropdownInputModel extends InputDefinition {
    optionUrl: string;
    optionValueField: string;
    optionLabelField: string;
    allowAdditions: boolean;
    minSelections = 0;
    maxSelections = 1;
    selected: unknown[] = [];
    i18nBaseKey: string;

    constructor(
        json: Record<string, unknown>,
        type: string,
        name: string,
        placeholder: string,
        dataType: DataTypeModel
    ) {
        super(json, type, name, placeholder, dataType, DropdownInputModel.createValidators(json));
        this.optionUrl = json.optionUrl as string;
        this.optionValueField = json.jsonValueField as string;
        this.optionLabelField = json.jsonLabelField as string;
        this.allowAdditions = json.allowAdditions as boolean;
        this.minSelections = json.minSelections as number;
        this.maxSelections = json.maxSelections as number;
        const dataTypeJson = json.dataType as Record<string, unknown>;
        const defV = dataTypeJson?.defaultValue;
        this.selected = defV == null || defV === '' ? [] : [defV];
    }
}

/** @deprecated Use RestDropdownInputModel instead */
export const CwRestDropdownInputModel = RestDropdownInputModel;

export class ParameterDefinition {
    defaultValue: string;
    priority: number;
    key: string;
    inputType: InputDefinition;
    i18nBaseKey: string;

    static fromJson(json: Record<string, unknown>): ParameterDefinition {
        const m = new ParameterDefinition();
        const defV = json.defaultValue as string;
        m.defaultValue = defV == null || defV === '' ? null : defV;
        m.priority = json.priority as number;
        m.key = json.key as string;
        m.inputType = InputDefinition.fromJson(json.inputType as Record<string, unknown>, m.key);
        m.i18nBaseKey = json.i18nBaseKey as string;

        return m;
    }
}

const Registry: Record<string, typeof InputDefinition> = {
    text: InputDefinition,
    datetime: InputDefinition,
    number: InputDefinition,
    dropdown: DropdownInputModel,
    restDropdown: RestDropdownInputModel
};
