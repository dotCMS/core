import {Inject} from 'angular2/angular2';
import {ApiRoot} from 'api/persistence/ApiRoot';
import {EntityMeta} from "api/persistence/EntityBase";


export class ActionTypeModel {
  id:string;
  i18nKey:string;


  constructor(id:string = 'NoSelection', i18nKey:string = null) {
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

export class ActionModelOld {
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

  clone():ActionModelOld {
    return new ActionModelOld(this.id, this.actionConfig.clone())
  }
}


export class ActionTypesProvider {
  actionsRef:EntityMeta
  ary:Array
  map:Map<string,ActionTypeModel>
  promise:Promise

  constructor(@Inject(ApiRoot) apiRoot) {
    this.map = new Map()
    this.ary = []
    this.actionsRef = apiRoot.root.child('system/ruleengine/actionlets')
    this.init();

  }

  init() {
    this.promise = new Promise((resolve, reject) => {
      this.actionsRef.once('value', (snap) => {
        let actionlets = snap['val']()
        let results = (Object.keys(actionlets).map((key) => {
          let actionType = actionlets[key]
          this.map.set(key, new ActionTypeModel(key, actionType.i18nKey))
          return actionlets[key]
        }))

        Array.prototype.push.apply(this.ary, results);
        resolve(this);
      })
    });
  }

  getType(id:string):ActionTypeModel {
    return this.map.get(id);
  }
}
