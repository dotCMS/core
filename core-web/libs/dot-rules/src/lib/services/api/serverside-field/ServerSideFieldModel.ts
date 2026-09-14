import { UntypedFormControl, Validators, ValidatorFn } from '@angular/forms';

import { LoggerService } from '@dotcms/dotcms-js';

import { BaseModel } from '../../models/base.model';
import { DropdownInputModel, ParameterDefinition } from '../../models/input.model';
import { ParameterModel } from '../rule/Rule';

export class ServerSideFieldModel extends BaseModel {
    parameterDefs: { [key: string]: ParameterDefinition };
    parameters: { [key: string]: ParameterModel };

    /** Set through the `type` accessor below, which every constructor path drives. */
    private _type!: ServerSideTypeModel;

    static createNgControl(model: ServerSideFieldModel, paramName: string): UntypedFormControl {
        const param = model.parameters[paramName];
        const paramDef = model.parameterDefs[paramName];
        // `?? []` rather than an assertion: only `SpacerInputDefinition` has a null `dataType`,
        // and it never reaches here, but "no data type" honestly means "no validators".
        const vFn: ValidatorFn[] = paramDef.inputType.dataType?.validators() ?? [];

        const control = new UntypedFormControl(
            model.getParameterValue(param.key),
            Validators.compose(vFn)
        );

        return control;
    }

    constructor(
        key: string | null,
        _type: ServerSideTypeModel,
        _priority = 1,
        public loggerService?: LoggerService
    ) {
        super(key);
        this.parameters = {};
        this.parameterDefs = {};
    }

    get type(): ServerSideTypeModel {
        return this._type;
    }

    set type(type: ServerSideTypeModel) {
        if (type && this._type !== type) {
            this._type = type;
            this.parameterDefs = {};
            this.parameters = {};

            Object.keys(type.parameters).forEach((key) => {
                const x = type.parameters[key];
                const paramDef = ParameterDefinition.fromJson(
                    x as unknown as Record<string, unknown>
                );
                const defaultValue =
                    paramDef.defaultValue ?? paramDef.inputType.dataType?.defaultValue ?? '';
                this.parameterDefs[key] = paramDef;
                this.parameters[key] = {
                    key: key,
                    priority: paramDef.priority,
                    value: defaultValue
                };
            });
        }
    }

    setParameter(key: string, value: string, priority = 1): void {
        if (this.parameterDefs[key] === undefined) {
            this.loggerService?.info(
                'Unsupported parameter: ',
                key,
                'Valid parameters: ',
                Object.keys(this.parameterDefs)
            );

            return;
        }

        this.parameters[key] = { key: key, priority: priority, value: value };
    }

    /** `null` for an unknown key — the three call sites all test the result before using it. */
    getParameter(key: string): ParameterModel | null {
        return this.parameters[key] ?? null;
    }

    /** `null` when the parameter is unset; callers read it as `value || fallback`. */
    getParameterValue(key: string): string | null {
        return this.parameters[key]?.value ?? null;
    }

    /** `null` for an unknown key. */
    getParameterDef(key: string): ParameterDefinition | null {
        return this.parameterDefs[key] ?? null;
    }

    override isValid(): boolean {
        let valid = true;
        Object.keys(this.parameterDefs).some((key) => {
            const paramDef = this.parameterDefs[key];
            const value = this.parameters[key].value;
            try {
                valid = valid && paramDef.inputType.verify(value) == null;
            } catch (e) {
                this.loggerService?.error(e);
            }

            // `instanceof` instead of `inputType['options']`: `options` lives on
            // `DropdownInputModel`, not on the base, so the old bracket access read `undefined`
            // and then indexed it for any non-dropdown comparison input — a throw, caught by
            // nobody. Comparison inputs are dropdowns in practice, which is why it never fired.
            if (paramDef.inputType.name === 'comparison') {
                const options =
                    paramDef.inputType instanceof DropdownInputModel
                        ? paramDef.inputType.options
                        : undefined;
                const option = options?.[value] as { rightHandArgCount?: number } | undefined;

                // Stop at a comparison that takes no right-hand argument: the parameters after
                // it are the arguments, and they are legitimately unset.
                return option?.rightHandArgCount === 0;
            }

            return false;
        });

        // `!!`: this chain is `string | boolean` — `this._type.key` is a string — so the old
        // `boolean` return type was a lie for every valid field.
        return !!(valid && this._type && this._type.key && this._type.key !== 'NoSelection');
    }
}

export interface ServerSideTypeOption {
    value: string;
    label: string;
}

export interface ServerSideTypeJson {
    key: string;
    i18nKey: string;
    parameterDefinitions?: Record<string, unknown>;
}

export class ServerSideTypeModel {
    key: string;
    /** `null` when the server sends no i18n key; `isValid()` below treats that as invalid. */
    i18nKey: string | null;
    parameters: { [key: string]: ParameterDefinition };

    /**
     * The select option built for this type. Filled by `RuleService`'s type loader as soon as
     * the i18n label resolves, before the type is published to any component.
     */
    _opt!: ServerSideTypeOption;

    static fromJson(json: ServerSideTypeJson): ServerSideTypeModel {
        return new ServerSideTypeModel(json.key, json.i18nKey, json.parameterDefinitions);
    }

    constructor(
        key = 'NoSelection',
        i18nKey: string | null = null,
        parameters: Record<string, unknown> = {}
    ) {
        this.key = key ? key : 'NoSelection';
        this.i18nKey = i18nKey;
        this.parameters = parameters as { [key: string]: ParameterDefinition };
    }

    isValid(): boolean {
        return !!this.i18nKey && !!this.parameters;
    }
}
