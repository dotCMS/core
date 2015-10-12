export class ActionTypeModel {
  id:string;
  i18nKey:string;


  constructor(id:string = null, i18nKey:string = null) {
    this.id = id;
    this.i18nKey = i18nKey;
  }

  clone():ActionTypeModel {
    throw new Error("Extending classes must implement their own clone method and should not call super.")
  }


}

export class ActionConfigModel {
  actionTypeId:string;


  constructor(actionTypeId:string) {
    this.actionTypeId = actionTypeId;
  }

  clone():ActionConfigModel {
    throw new Error("Extending classes must implement their own clone method and should not call super.")
  }


  out():any {
    return {}
  }
}


export class SetSessionValueActionModel extends ActionConfigModel {
  sessionKey:string;
  sessionValue:string;

  constructor(parameters:Object = {}) {
    super("SetSessionAttributeActionlet")
    this.sessionKey = ''
    this.sessionValue = ''
    Object.keys(parameters).forEach((paramId) => {
      let param = parameters[paramId]
      switch (param.key) {
        case 'sessionValue':
        {
          this.sessionValue = param.value || ''
          break
        }
        case 'sessionKey':
        {
          this.sessionKey = param.value || ''
          break
        }
      }

    })
  }

  clone():SetSessionValueActionModel {
    var model = new SetSessionValueActionModel()
    model.sessionKey = this.sessionKey
    model.sessionValue = this.sessionValue
    return model
  }

  out():any {
    return {
      sessionKey: {
        id: 'sessionKey',
        key: 'sessionKey',
        value: this.sessionKey
      }, sessionValue: {
        id: 'sessionValue',
        key: 'sessionValue',
        value: this.sessionValue
      }
    }
  }
}

export class RuleActionModel {
  id:string;
  actionConfig:ActionConfigModel;
  priority:number;
  owningRuleId:string;
  name:string;


  constructor(id = null, actionConfig = null) {
    this.id = id;
    this.actionConfig = actionConfig;
    this.priority = 0;
  }

  setActionType(actionType:ActionTypeModel, parameters:any = {}) {
    if (actionType.id === 'SetSessionAttributeActionlet') {
      this.actionConfig = new SetSessionValueActionModel(parameters);
    } else {
      this.actionConfig = new ActionConfigModel(actionType.id);
    }
  }

  out():any {
    return {
      id: this.id,
      actionlet: this.actionConfig.actionTypeId,
      priority: this.priority,
      name: this.name,
      owningRule: this.owningRuleId,
      parameters: this.actionConfig.out(),
    }
  }

  clone():RuleActionModel {
    return new RuleActionModel(this.id, this.actionConfig.clone())
  }
}