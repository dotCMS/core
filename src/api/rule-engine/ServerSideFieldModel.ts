import {CwModel} from "../util/CwModel";
import {ParameterDefinition} from "../util/CwInputModel";
import {ParameterModel} from "./Rule";
import {Control, Validators} from "angular2/common";
import {CustomValidators} from "../validation/CustomValidators";

export class ServerSideFieldModel extends CwModel {
  private _type:ServerSideTypeModel
  parameters:{[key:string]: ParameterModel}
  parameterDefs:{[key:string]: ParameterDefinition}
  priority:number

  constructor(key:string, type:ServerSideTypeModel, priority:number = 1) {
    super(key)
    this.parameters = {}
    this.parameterDefs = {}
  
  }

  get type():ServerSideTypeModel {
    return this._type;
  }

  set type(type:ServerSideTypeModel) {
    if (type && this._type != type) {
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
      console.log("Unsupported parameter: ", key, "Valid parameters: ", Object.keys(this.parameterDefs))
      return;
    }
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
        try {
          valid = valid && ( paramDef.inputType.verify(value) == null )
        } catch (e) {
          console.error(e)
        }
      })
    }
    valid = valid && this._type && this._type.key && this._type.key != 'NoSelection'
    return valid
  }

  static createNgControl(model:ServerSideFieldModel, paramName:string):Control {
    let param = model.parameters[paramName]
    let paramDef = model.parameterDefs[paramName]
    let vFn:Function[] = paramDef.inputType.dataType.validators()
    vFn.push(CustomValidators.noQuotes())
    let control = new Control(
        model.getParameterValue(param.key),
        Validators.compose(vFn))

    control.statusChanges.subscribe((value) => {

    })
    return control
  }
  
}


export class ServerSideTypeModel{

  key:string
  priority:number
  i18nKey:string
  parameters:{ [key:string]:ParameterDefinition}
  _opt:any

  constructor(key:string = 'NoSelection', i18nKey:string = null, parameters:any = {}) {
    this.key = key ? key : 'NoSelection'
    this.i18nKey = i18nKey
    this.parameters = parameters
  }

  isValid():boolean {
    return !!this.i18nKey && !!this.parameters
  }
  
  static fromJson(json:any):ServerSideTypeModel {
    return new ServerSideTypeModel(json.key, json.i18nKey, json.parameterDefinitions)
  }

}