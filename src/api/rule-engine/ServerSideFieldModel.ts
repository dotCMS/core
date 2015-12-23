import {EventEmitter, Injectable} from 'angular2/angular2';
import {Observable, ConnectableObservable} from 'rxjs/Rx.KitchenSink'

import {RuleModel} from "./Rule";
import {CwModel} from "../util/CwModel";
import {CwI18nModel} from "../util/CwModel";
import {ApiRoot} from "../persistence/ApiRoot";
import {CwChangeEvent} from "../util/CwEvent";
import {ParameterDefinition} from "../util/CwInputModel";
import {CwInputDefinition} from "../util/CwInputModel";


let noop = (...arg:any[])=> {
}

export interface ParameterModel {
  key:string
  value:string
  priority:number
}

export class ServerSideFieldModel extends CwModel {
  name:string // @todo ggranum: vestigial field, kill on server side.
  comparison:string
  operator:string
  private _type:ServerSideTypeModel
  parameters:{[key:string]: ParameterModel}
  parameterDefs:{[key:string]: ParameterDefinition}
  priority:number

  constructor(key:string, type:ServerSideTypeModel, priority:number = 1) {
    super(key)
    this.name = "asdfasdf-" + new Date().getTime()
    this.parameters = {}
    this.parameterDefs = {}
    this.operator = 'AND'
    this.priority = priority || 1
    this.type = type
  }

  get type():ServerSideTypeModel {
    return this._type;
  }

  set type(type:ServerSideTypeModel) {
    if (this._type != type) {
      this._type = type;
      this.parameterDefs = {}
      this.parameters = {}

      Object.keys(type.parameters).forEach((key)=> {
        let x = type.parameters[key]
        let paramDef = ParameterDefinition.fromJson(x)
        this.parameterDefs[key] = paramDef
        this.parameters[key] = {key: key, value: paramDef.defaultValue, priority: paramDef.priority}
      })
    }
  }


  setParameter(key:string, value:any, priority:number = 1) {
    if (this.parameterDefs[key] === undefined) {
      console.log("Unsupported parameter: ", key)
      return;
    }
    let existing = this.parameters[key]
    this.parameters[key] = {key: key, value: value, priority: priority}
  }

  getParameter(key:string):ParameterModel {
    let v:any = ''
    if (this.parameters[key] !== undefined) {
      v = this.parameters[key]
    }
    return v
  }

  getParameterValue(key:string):string {
    let v:any = null
    if (this.parameters[key] !== undefined) {
      v = this.parameters[key].value
    }
    return v
  }

  getParameterDef(key:string):ParameterDefinition {
    let v:any = ''
    if (this.parameterDefs[key] !== undefined) {
      v = this.parameterDefs[key]
    }
    return v
  }

  isValid():boolean {
    let valid = true
    if (this.parameterDefs) {
      Object.keys(this.parameterDefs).forEach(key=> {
        let paramDef = this.getParameterDef(key)
        let param = this.parameters[key]
        var value = param.value;
        valid = valid && paramDef.inputType.verify(value).valid
        console.log("validate => key: ", key, "  value: ", value, "  valid: ", valid)
      })
    }
    valid = valid && this._type && this._type.key && this._type.key != 'NoSelection'
    console.log("validate => Result: ", valid)

    return valid
  }

  toJson():any {
    let json = {
      name: this.name,
      comparison: this.comparison,
      typeId: this._type.i18nKey,
      operator: this.operator,
      parameters: this.parameters,
      parameterDefs: this.parameterDefs
    }
    return json
  }

}


export class ServerSideTypeModel extends CwI18nModel {

  i18nKey:string
  parameters:{ [key:string]:ParameterDefinition}

  constructor(key:string = 'NoSelection', i18nKey:string = null, parameters:any = {}) {
    super(key ? key : 'NoSelection', i18nKey, {name: i18nKey})
    this.parameters = parameters
  }

  isValid():boolean {
    return this.isPersisted() && !!this.i18nKey && !!this.parameters
  }

  static fromJson(json:any):ServerSideTypeModel {
    let model = new ServerSideTypeModel(json.key, json.i18nKey, json.parameterDefinitions);
    return model
  }

}