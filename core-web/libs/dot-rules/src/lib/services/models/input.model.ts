import { ValidationErrors, Validators, ValidatorFn } from '@angular/forms';

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
    /** Built on first call to `validators()`, so `null` means "not yet computed". */
    private _vFns: ValidatorFn[] | null = null;

    constructor(
        public id: string,
        public errorMessageKey: string,
        private _constraints: Record<string, TypeConstraint>,
        // A JSON data type need not declare a default. `ServerSideFieldModel` reads this as
        // `paramDef.defaultValue || paramDef.inputType.dataType.defaultValue`, so absence is
        // the expected case rather than an error.
        public defaultValue: string | null = null
    ) {}

    validators(): ValidatorFn[] {
        if (this._vFns == null) {
            // Accumulated in a local: assigning `this._vFns` first and pushing inside the
            // callback loses the narrowing, because a closure could reassign the property.
            const fns: ValidatorFn[] = [];
            Object.keys(VALIDATIONS).forEach((vDefKey) => {
                const vDef = VALIDATIONS[vDefKey];
                const constraint = this._constraints[vDef.key];
                if (constraint) {
                    fns.push(vDef.providerFn(constraint));
                }
            });
            this._vFns = fns;
        }

        return this._vFns;
    }

    /** `null` when this data type declares no constraints — what `Validators.compose` returns. */
    validator(): ValidatorFn | null {
        return Validators.compose(this.validators());
    }
}

export class InputDefinition {
    private _vFns: ValidatorFn[] | null = null;
    private _validator: ValidatorFn | null = null;

    static fromJson(json: Record<string, unknown>, name: string): InputDefinition {
        const typeId = (json['id'] || json['type']) as string;
        let type = Registry[typeId];

        if (!type) {
            const msg = "No input definition registered for '" + typeId + "'. Using default.";
            console.error(msg, json);
            type = InputDefinition;
        }

        let dataType: DataTypeModel | null = null;
        const dataTypeJson = json['dataType'] as Record<string, unknown>;
        if (dataTypeJson) {
            dataType = new DataTypeModel(
                dataTypeJson['id'] as string,
                dataTypeJson['errorMessageKey'] as string,
                dataTypeJson['constraints'] as Record<string, TypeConstraint>,
                dataTypeJson['defaultValue'] as string
            );
        }

        return new type(json, typeId, name, json['placeholder'] as string, dataType);
    }

    constructor(
        public json: Record<string, unknown>,
        public type: string,
        public name: string | null,
        public placeholder: string | null,
        // `null` for `SpacerInputDefinition`, which has nothing to validate. Every definition
        // built from JSON has one, which is why `validators()` could get away with assuming it.
        public dataType: DataTypeModel | null,
        private _validators: ValidatorFn[] = []
    ) {}

    validators(): ValidatorFn[] {
        if (this._vFns == null) {
            this._vFns = (this.dataType?.validators() ?? []).concat(this._validators);
        }

        return this._vFns;
    }

    validator(): ValidatorFn {
        if (this._validator == null) {
            const fns = this.validators();
            this._validator = (fns.length ? Validators.compose(fns) : null) ?? (() => null);
        }

        return this._validator;
    }

    /**
     * `null` when the value passes — that is `ValidationErrors`, and it is what the one caller
     * expects: `ServerSideFieldModel` tests `paramDef.inputType.verify(value) == null`. The old
     * `{ [key: string]: boolean }` return type could not express the passing case at all.
     */
    verify(value: unknown): ValidationErrors | null {
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

    static createValidators(json: Record<string, unknown>): ValidatorFn[] {
        return [
            CustomValidators.minSelections((json['minSelections'] as number) || 0),
            CustomValidators.maxSelections((json['maxSelections'] as number) || 1)
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
        this.options = json['options'] as { [key: string]: unknown };
        this.allowAdditions = json['allowAdditions'] as boolean;
        this.minSelections = json['minSelections'] as number;
        this.maxSelections = json['maxSelections'] as number;
        const dataTypeJson = json['dataType'] as Record<string, unknown>;
        const defV = dataTypeJson?.['defaultValue'];
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

    constructor(
        json: Record<string, unknown>,
        type: string,
        name: string,
        placeholder: string,
        dataType: DataTypeModel
    ) {
        super(json, type, name, placeholder, dataType, DropdownInputModel.createValidators(json));
        this.optionUrl = json['optionUrl'] as string;
        this.optionValueField = json['jsonValueField'] as string;
        this.optionLabelField = json['jsonLabelField'] as string;
        this.allowAdditions = json['allowAdditions'] as boolean;
        this.minSelections = json['minSelections'] as number;
        this.maxSelections = json['maxSelections'] as number;
        const dataTypeJson = json['dataType'] as Record<string, unknown>;
        const defV = dataTypeJson?.['defaultValue'];
        this.selected = defV == null || defV === '' ? [] : [defV];
    }
}

/** @deprecated Use RestDropdownInputModel instead */
export const CwRestDropdownInputModel = RestDropdownInputModel;

export class ParameterDefinition {
    /**
     * Constructed only through `fromJson`, which is why these were declared without
     * initialisers. Made constructor parameters so the class cannot exist half-filled.
     */
    constructor(
        // Empty string and absent both mean "no default", collapsed to `null` by `fromJson`.
        public defaultValue: string | null,
        public priority: number,
        public key: string,
        public inputType: InputDefinition,
        public i18nBaseKey: string
    ) {}

    static fromJson(json: Record<string, unknown>): ParameterDefinition {
        const defV = json['defaultValue'] as string;
        const key = json['key'] as string;

        return new ParameterDefinition(
            defV == null || defV === '' ? null : defV,
            json['priority'] as number,
            key,
            InputDefinition.fromJson(json['inputType'] as Record<string, unknown>, key),
            json['i18nBaseKey'] as string
        );
    }
}

const Registry: Record<string, typeof InputDefinition> = {
    text: InputDefinition,
    datetime: InputDefinition,
    number: InputDefinition,
    dropdown: DropdownInputModel,
    restDropdown: RestDropdownInputModel
};
