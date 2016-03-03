import {CwModel} from "../util/CwModel";
import {ParameterDefinition} from "../util/CwInputModel";

export interface ParameterModel {
  key:string
  value:string
  priority:number
}

export class ServerSideFieldModel extends CwModel {
  comparison:string
  operator:string
  private _type:ServerSideTypeModel
  parameters:{[key:string]: ParameterModel}
  parameterDefs:{[key:string]: ParameterDefinition}
  priority:number

  constructor(key:string, type:ServerSideTypeModel, priority:number = 1) {
    super(key)
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
        valid = valid && ( paramDef.inputType.verify(value) == null )
      })
    }
    valid = valid && this._type && this._type.key && this._type.key != 'NoSelection'
    console.log("validate => Result: ", valid)

    return valid
  }

  toJson():any {
    return {
      comparison: this.comparison,
      typeId: this._type.i18nKey,
      operator: this.operator,
      parameters: this.parameters,
      parameterDefs: this.parameterDefs
    }
  }

}


export class ServerSideTypeModel extends CwModel{

  i18nKey:string
  parameters:{ [key:string]:ParameterDefinition}

  constructor(key:string = 'NoSelection', i18nKey:string = null, parameters:any = {}) {
    super(key ? key : 'NoSelection')
    this.i18nKey = i18nKey
    this.parameters = parameters
  }

  isValid():boolean {
    return this.isPersisted() && !!this.i18nKey && !!this.parameters
  }

  static fromJson(json:any):ServerSideTypeModel {
    return new ServerSideTypeModel(json.key, json.i18nKey, json.parameterDefinitions)
  }

}