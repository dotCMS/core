import { Validators, ValidatorFn } from '@angular/forms';

import { CustomValidators } from '../validation/CustomValidators';

export class CwValidationResults {
    valid: boolean;

    constructor(valid: boolean) {
        this.valid = valid;
    }
}

interface TypeConstraint {
    id: string;
    args: { [key: string]: any };
}

interface ValidatorDefinition {
    key: string;
    providerFn: Function;
}
const VALIDATIONS = {
    maxLength: {
        key: 'maxLength',
        providerFn: (constraint: TypeConstraint) =>
            CustomValidators.maxLength(constraint.args['value'])
    },
    maxValue: {
        key: 'maxValue',
        providerFn: (constraint: TypeConstraint) => CustomValidators.max(constraint.args['value'])
    },
    minLength: {
        key: 'minLength',
        providerFn: (constraint: TypeConstraint) =>
            CustomValidators.minLength(constraint.args['value'])
    },
    minValue: {
        key: 'minValue',
        providerFn: (constraint: TypeConstraint) => CustomValidators.min(constraint.args['value'])
    },
    required: {
        key: 'required',
        providerFn: (_constraint: TypeConstraint) => CustomValidators.required()
    }
};
export class DataTypeModel {
    private _vFns: Function[];

    constructor(
        public id: string,
        public errorMessageKey: string,
        private _constraints: any,
        public defaultValue: string = null
    ) {}

    validators(): Array<Function> {
        if (this._vFns == null) {
            this._vFns = [];
            Object.keys(VALIDATIONS).forEach((vDefKey) => {
                const vDef: ValidatorDefinition = VALIDATIONS[vDefKey];
                const constraint: TypeConstraint = this._constraints[vDef.key];
                if (constraint) {
                    const fn = vDef.providerFn(constraint);
                    this._vFns.push(fn);
                }
            });
        }

        return this._vFns;
    }

    validator(): ValidatorFn {
        return Validators.compose(<ValidatorFn[]>this.validators());
    }
}

export class CwInputDefinition {
    private _vFns: Function[];
    private _validator: Function;

    static fromJson(json: any, name: string): CwInputDefinition {
        const typeId = json.id || json.type;
        let type = Registry[typeId];

        if (!type) {
            const msg =
                "No input definition registered for '" +
                (json.id || json.type) +
                "'. Using default.";
            // tslint:disable-next-line:no-console
            console.error(msg, json);
            type = 'text';
        }

        let dataType = null;
        if (json.dataType) {
            dataType = new DataTypeModel(
                json.dataType.id,
                json.dataType.errorMessageKey,
                json.dataType.constraints,
                json.dataType.defaultValue
            );
        }

        return new type(json, typeId, name, json.placeholder, dataType);
    }

    constructor(
        public json: any,
        public type: string,
        public name: string,
        public placeholder: string,
        public dataType: DataTypeModel,
        private _validators: Function[] = []
    ) {}

    validators(): Array<Function> {
        if (this._vFns == null) {
            this._vFns = this.dataType.validators().concat(this._validators);
        }

        return this._vFns;
    }

    validator(): Function {
        if (this._validator == null) {
            this._vFns = this.validators();
            if (this._vFns && this._vFns.length) {
                this._validator = Validators.compose(<ValidatorFn[]>this._vFns);
            } else {
                this._validator = () => {
                    return null;
                };
            }
        }

        return this._validator;
    }

    verify(value: any): { [key: string]: boolean } {
        return this.validator()({ value: value });
    }
}

export class CwSpacerInputDefinition extends CwInputDefinition {
    protected flex;

    constructor(flex: number) {
        super({}, 'spacer', null, null, null);
        this.flex = flex;
    }
}

export class CwDropdownInputModel extends CwInputDefinition {
    options: { [key: string]: any };
    allowAdditions: boolean;
    minSelections = 0;
    maxSelections = 1;
    selected: Array<any> = [];
    i18nBaseKey: string;

    static createValidators(json: any): any[] {
        const ary = [];
        ary.push(CustomValidators.minSelections(json.minSelections || 0));
        ary.push(CustomValidators.maxSelections(json.maxSelections || 1));

        return ary;
    }

    constructor(json, type, name, placeholder, dataType) {
        super(json, type, name, placeholder, dataType, CwDropdownInputModel.createValidators(json));
        this.options = json.options;
        this.allowAdditions = json.allowAdditions;
        this.minSelections = json.minSelections;
        this.maxSelections = json.maxSelections;
        const defV = json.dataType.defaultValue;
        this.selected = defV == null || defV === '' ? [] : [defV];
    }
}

export class CwRestDropdownInputModel extends CwInputDefinition {
    optionUrl: string;
    optionValueField: string;
    optionLabelField: string;
    allowAdditions: boolean;
    minSelections = 0;
    maxSelections = 1;
    selected: Array<any> = [];
    i18nBaseKey: string;

    constructor(json, type, name, placeholder, dataType) {
        super(json, type, name, placeholder, dataType, CwDropdownInputModel.createValidators(json));
        this.optionUrl = json.optionUrl;
        this.optionValueField = json.jsonValueField;
        this.optionLabelField = json.jsonLabelField;
        this.allowAdditions = json.allowAdditions;
        this.minSelections = json.minSelections;
        this.maxSelections = json.maxSelections;
        const defV = json.dataType.defaultValue;
        this.selected = defV == null || defV === '' ? [] : [defV];
    }
}

export class ParameterDefinition {
    defaultValue: string;
    priority: number;
    key: string;
    inputType: CwInputDefinition;
    i18nBaseKey: string;

    static fromJson(json: any): ParameterDefinition {
        const m = new ParameterDefinition();
        const defV = json.defaultValue;
        m.defaultValue = defV == null || defV === '' ? null : defV;
        m.priority = json.priority;
        m.key = json.key;
        m.inputType = CwInputDefinition.fromJson(json.inputType, m.key);
        m.i18nBaseKey = json.i18nBaseKey;

        return m;
    }
}

const Registry = {
    text: CwInputDefinition,
    datetime: CwInputDefinition,
    number: CwInputDefinition,
    dropdown: CwDropdownInputModel,
    restDropdown: CwRestDropdownInputModel
};
