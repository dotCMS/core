import { CwModel } from './util/CwModel';
import { ParameterDefinition } from './util/CwInputModel';
import { ParameterModel } from './Rule';
import { UntypedFormControl, Validators, ValidatorFn } from '@angular/forms';
import { LoggerService } from '@dotcms/dotcms-js';
import { CustomValidators } from './validation/CustomValidators';

export class ServerSideFieldModel extends CwModel {
    parameterDefs: { [key: string]: ParameterDefinition };
    parameters: { [key: string]: ParameterModel };
    priority: number;

    private _type: ServerSideTypeModel;

    static createNgControl(model: ServerSideFieldModel, paramName: string): UntypedFormControl {
        const param = model.parameters[paramName];
        const paramDef = model.parameterDefs[paramName];
        const vFn: Function[] = <ValidatorFn[]>paramDef.inputType.dataType.validators();
        vFn.push(CustomValidators.noDoubleQuotes());
        const control = new UntypedFormControl(
            model.getParameterValue(param.key),
            Validators.compose(<ValidatorFn[]>vFn)
        );
        return control;
    }

    constructor(
        key: string,
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
                const paramDef = ParameterDefinition.fromJson(x);
                const defaultValue =
                    paramDef.defaultValue || paramDef.inputType.dataType.defaultValue;
                this.parameterDefs[key] = paramDef;
                this.parameters[key] = {
                    key: key,
                    priority: paramDef.priority,
                    value: defaultValue
                };
            });
        }
    }

    setParameter(key: string, value: any, priority = 1): void {
        if (this.parameterDefs[key] === undefined) {
            this.loggerService.info(
                'Unsupported parameter: ',
                key,
                'Valid parameters: ',
                Object.keys(this.parameterDefs)
            );
            return;
        }
        this.parameters[key] = { key: key, priority: priority, value: value };
    }

    getParameter(key: string): ParameterModel {
        let v: any = '';
        if (this.parameters[key] !== undefined) {
            v = this.parameters[key];
        }
        return v;
    }

    getParameterValue(key: string): string {
        let v: any = null;
        if (this.parameters[key] !== undefined) {
            v = this.parameters[key].value;
        }
        return v;
    }

    getParameterDef(key: string): ParameterDefinition {
        let v: any = '';
        if (this.parameterDefs[key] !== undefined) {
            v = this.parameterDefs[key];
        }
        return v;
    }

    isValid(): boolean {
        let valid = true;
        if (this.parameterDefs) {
            Object.keys(this.parameterDefs).some((key) => {
                const paramDef = this.getParameterDef(key);
                const param = this.parameters[key];
                const value = param.value;
                try {
                    valid = valid && paramDef.inputType.verify(value) == null;
                } catch (e) {
                    this.loggerService.error(e);
                }
                if (
                    paramDef.inputType.name === 'comparison' &&
                    paramDef.inputType['options'][value].rightHandArgCount === 0
                ) {
                    return true;
                }
            });
        }
        valid = valid && this._type && this._type.key && this._type.key !== 'NoSelection';
        return valid;
    }
}

export class ServerSideTypeModel {
    key: string;
    priority: number;
    i18nKey: string;
    parameters: { [key: string]: ParameterDefinition };
    _opt: any;

    static fromJson(json: any): ServerSideTypeModel {
        return new ServerSideTypeModel(json.key, json.i18nKey, json.parameterDefinitions);
    }

    constructor(key = 'NoSelection', i18nKey: string = null, parameters: any = {}) {
        this.key = key ? key : 'NoSelection';
        this.i18nKey = i18nKey;
        this.parameters = parameters;
    }

    isValid(): boolean {
        return !!this.i18nKey && !!this.parameters;
    }
}
