import {CwModel} from '../util/CwModel';
import {ParameterDefinition} from '../util/CwInputModel';
import {ParameterModel} from './Rule';
import {FormControl, Validators, ValidatorFn} from '@angular/forms';
import {CustomValidators} from '../validation/CustomValidators';

export class ServerSideFieldModel extends CwModel {

  parameterDefs: {[key: string]: ParameterDefinition};
  parameters: {[key: string]: ParameterModel};
  priority: number;

  private _type: ServerSideTypeModel;

  static createNgControl(model: ServerSideFieldModel, paramName: string): FormControl {
    let param = model.parameters[paramName];
    let paramDef = model.parameterDefs[paramName];
    let vFn: Function[] = <ValidatorFn[]> paramDef.inputType.dataType.validators();
    vFn.push(CustomValidators.noQuotes());
    let control = new FormControl(
        model.getParameterValue(param.key),
        Validators.compose( <ValidatorFn[]> vFn ));
    return control;
  }

  constructor(key: string, type: ServerSideTypeModel, priority = 1) {
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
        let x = type.parameters[key];
        let paramDef = ParameterDefinition.fromJson(x);
        let defaultValue = paramDef.defaultValue || paramDef.inputType.dataType.defaultValue;
        this.parameterDefs[key] = paramDef;
        this.parameters[key] = {key: key, priority: paramDef.priority, value: defaultValue };
      });
    }
  }

  setParameter(key: string, value: any, priority = 1): void {
    if (this.parameterDefs[key] === undefined) {
      console.log('Unsupported parameter: ', key, 'Valid parameters: ', Object.keys(this.parameterDefs));
      return;
    }
    this.parameters[key] = {key: key, priority: priority, value: value};
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
      Object.keys(this.parameterDefs).some(key => {
        let paramDef = this.getParameterDef(key);
        let param = this.parameters[key];
        let value = param.value;
        try {
          valid = valid && ( paramDef.inputType.verify(value) == null );
        } catch (e) {
          console.error(e);
        }
        if (paramDef.inputType.name === 'comparison' && paramDef.inputType['options'][value].rightHandArgCount === 0) {
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
  parameters: { [key: string]: ParameterDefinition};
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