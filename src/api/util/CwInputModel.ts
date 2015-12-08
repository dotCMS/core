


export class DataTypeModel {
  type:string


  constructor(typeDef:any) {
    this.type = typeDef.type;
  }
}

var Registry = {};

export class CwInputDefinition {
  type:string
  placeholder:string
  dataType: DataTypeModel
  name:string


  constructor(id:string, name:string) {
    this.type = id
    this.name = name
  }

  static registerType(typeId:string, type:Function){
    Registry[typeId] = type
  }

  static fromJson(json:any, name:string):CwInputDefinition{
    let m = Registry[json.id].fromJson(json, name);
    m.placeholder = json.placeholder
    m.dataType = json.dataType
    return m;
  }
}

export class CwSpacerInputDefinition extends CwInputDefinition {
  private flex;

  constructor(flex:number) {
    super("spacer", null);
    this.flex = flex
  }
}


export class CwTextInputModel extends CwInputDefinition {
  minLength:number = 0
  maxLength:number = 255


  constructor(id:string, name:string) {
    super(id, name);
  }

  static fromJson(json:any, name:string):CwTextInputModel {
    let m = new CwTextInputModel(json.id, name);
    m.minLength = json.dataType.minLength
    m.maxLength = json.dataType.maxLength
    return m
  }
}
CwInputDefinition.registerType("text", CwTextInputModel)

export class CwDropdownInputModel extends CwInputDefinition {
  options: {[key:string]: any}
  allowAdditions: boolean
  selected:Array<any>

  constructor(id:string, name:string) {
    super(id, name)
  }

  static fromJson(json:any, name:string):CwDropdownInputModel {
    let m = new CwDropdownInputModel(json.id, name);
    m.options = json.options
    m.allowAdditions = json.allowAdditions
    m.selected = [json.dataType.defaultValue]
    return m
  }
}

CwInputDefinition.registerType("dropdown", CwDropdownInputModel)



export class ParameterDefinition {
  defaultValue:string
  priority: number
  key:string
  inputType: CwInputDefinition


  constructor() {
  }

  static fromJson(json:any):ParameterDefinition{
    let m = new ParameterDefinition
    m.defaultValue = json.defaultValue
    m.priority = json.priority
    m.key = json.key
    m.inputType = CwInputDefinition.fromJson(json.inputType, m.key)
    return m;
  }
}


