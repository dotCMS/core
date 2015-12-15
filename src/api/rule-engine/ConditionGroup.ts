import {Inject, EventEmitter} from 'angular2/angular2';
import {Observable, ConnectableObservable} from 'rxjs/Rx.KitchenSink'


import {RuleModel} from "./Rule";
import {ConditionService, ConditionModel} from "./Condition";
import {CwModel} from "../util/CwModel";
import {EntitySnapshot} from "../persistence/EntityBase";
import {ApiRoot} from "../persistence/ApiRoot";


export class ConditionGroupModel extends CwModel {

  owningRule:RuleModel
  operator:string
  conditions:{ [key: string]: boolean }

  constructor(key:string = null, owningRule:RuleModel, operator:string, priority:number) {
    super(key)
    this.owningRule = owningRule
    this.operator = operator
    this.priority = priority
    this.conditions = {}
  }

  isValid() {
    let valid = !!this.owningRule
    valid = valid && this.owningRule.isValid() && this.owningRule.isPersisted()
    valid = valid && this.operator && (this.operator === 'AND' || this.operator === 'OR')
    return valid
  }
}

export class ConditionGroupService {
  private apiRoot;
  private ref;

  constructor(@Inject(ApiRoot) apiRoot, @Inject(ConditionService) conditionService:ConditionService) {
    this.ref = apiRoot.defaultSite.child('ruleengine/rules')
    this.apiRoot = apiRoot
  }

  static _fromSnapshot(rule:RuleModel, snapshot:EntitySnapshot):ConditionGroupModel {
    let val:any = snapshot.val()
    let ra = new ConditionGroupModel(snapshot.key(), rule, val.operator, val.priority)
    ra.conditions = val.conditions
    return ra
  }

  static _toJson(conditionGroup:ConditionGroupModel):any {
    let json:any = {}

    json.id = conditionGroup.key
    json.operator = conditionGroup.operator
    json.priority = conditionGroup.priority
    json.conditions = conditionGroup.conditions
    return json
  }

  static toJsonList(models:{[key:string]: ConditionGroupModel}):any {
    let list = {}
    Object.keys(models).forEach((key)=> {
      list[key] = ConditionGroupService._toJson(models[key])
    })
    return list
  }

  list(rule:RuleModel):Observable<ConditionGroupModel[]> {
    let ee = new EventEmitter()
    /* @todo ggranum remove stupid hack (ee returning after an emit means no fire on subscribe) */
    window.setTimeout(()=> {
      if (rule.isPersisted()) {
        let groups = []
        let conditionGroupsSnap = rule.snapshot.child('conditionGroups')
        if (conditionGroupsSnap.exists()) {
          conditionGroupsSnap.forEach((conditionGroupSnap:EntitySnapshot) => {
            groups.push(ConditionGroupService._fromSnapshot(rule, conditionGroupSnap))
          })
          ee.emit(groups)
        } else {
          ee.emit([])
        }
      }
    }, 50)
    ee.subscribe((d)=> {
      console.log("ConditionGroupService", "eh?")
    })
    return ee
  }

  add(model:ConditionGroupModel, cb:Function = null) {
    console.log("api.rule-engine.ConditionGroupService", "add", model)
    if(!model.isValid()){
      throw new Error("This should be thrown from a checkValid function on the model, and should provide the info needed to make the user aware of the fix.")
    }
    let json = ConditionGroupService._toJson(model)
    this.ref.child(model.owningRule.key).child('conditionGroups').push(json, (e, result)=> {
      if(e){
        throw e;
      }
      model.key = result.key()
      if (cb) {
        cb(model)
      }
    })
  }

  save(model:ConditionGroupModel, cb:Function = null) {
    console.log("api.rule-engine.ConditionGroupService", "save", model)
    if(!model.isValid()){
      throw new Error("This should be thrown from a checkValid function on the model, and should provide the info needed to make the user aware of the fix.")
    }
    if (!model.isPersisted()) {
      this.add(model, cb)
    } else {
      let json = ConditionGroupService._toJson(model)
      this.ref.child(model.owningRule.key).child('conditionGroups').child(model.key).set(json, (result)=> {
        if (cb) {
          cb(model)
        }
      })
    }
  }

  remove(model:ConditionGroupModel, cb:Function = null) {
    console.log("api.rule-engine.ConditionGroupService", "remove", model)
    // ConditionGroup is a special case. Have to delete the group from
    if (model.isPersisted()) {
      this.ref.child(model.owningRule.key).child('conditionGroups').child(model.key).remove(()=> {
        if (cb) {
          cb(model)
        }
      })
    }
  }


}

