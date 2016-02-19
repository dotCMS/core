import {Http} from 'angular2/http'
import {EventEmitter, Injectable} from 'angular2/core'
import {Observable} from 'rxjs/Rx'


import {ConditionGroupService, ConditionGroupModel} from "./ConditionGroup";
import {CwModel} from "../util/CwModel";
import {EntitySnapshot} from "../persistence/EntityBase";
import {EntityMeta} from "../persistence/EntityBase";
import {ApiRoot} from "../persistence/ApiRoot";
import {ActionService} from "./Action";
import {I18nService} from "../system/locale/I18n";


export class RuleModel extends CwModel {
  snapshot:EntitySnapshot // annoying hack, this should die.
  name:string
  enabled:boolean
  fireOn:string
  groups:{ [key: string]: ConditionGroupModel }
  actions:{ [key: string]: boolean }

  constructor(key:string, name:string = '', fireOn:string = 'EVERY_PAGE') {
    super(key)
    this.name = name
    this.fireOn = fireOn
    this.enabled = false
    this.actions = {}
    this.groups = {}
  }

  addGroup(group:ConditionGroupModel) {
    this.groups[group.key] = group
  }

  removeGroup(group:ConditionGroupModel) {
    delete this.groups[group.key]
  }

  isValid() {
    let valid = !!this.name
    valid = valid && this.name.trim().length > 0
    return valid
  }

  static fromJson(key:string, json:any):RuleModel {
    let rule = new RuleModel(key, json.name, json.fireOn);
    rule.enabled = json.enabled
    rule.priority = json.priority

    return rule;
  }
}


@Injectable()
export class RuleService {
  ref:EntityMeta
  private _rsrcService:I18nService;

  http:Http
  apiRoot:ApiRoot
  rules;

  constructor(apiRoot:ApiRoot,
              http:Http,
              actionService:ActionService,
              conditionGroupService:ConditionGroupService,
              rsrcService:I18nService) {
    this.apiRoot = apiRoot
    this.http = http
    this.ref = apiRoot.defaultSite.child('ruleengine/rules')
    this._rsrcService = rsrcService
  }


  static toJson(rule:RuleModel):any {
    let json:any = {}
    json.key = rule.key
    json.enabled = rule.enabled
    json.fireOn = rule.fireOn
    json.name = rule.name
    json.priority = rule.priority
    json.conditionGroups = ConditionGroupService.toJsonList(rule.groups)
    json.ruleActions = rule.actions
    return json
  }

  static fromSnapshot(key, snapshot:EntitySnapshot):RuleModel {
    let rule = new RuleModel(key)
    let val:any = snapshot.val()
    rule.snapshot = snapshot
    rule.enabled = val.enabled
    rule.fireOn = val.fireOn
    rule.name = val.name
    rule.priority = val.priority
    rule.actions = val.ruleActions
    let groups = snapshot.child('conditionGroups')
    if (groups.exists()) {
      groups.forEach((groupSnap) => {
        rule.addGroup(ConditionGroupService._fromSnapshot(rule, groupSnap))
      })
    }
    return rule

  }

  get(key:string, cb:Function) {
    this.ref.child(key).once('value', (snap) => {
      let rule = RuleService.fromSnapshot(key, snap)
      cb(rule)
    }, (e)=> {
      throw e
    })
  }

  list():Observable<Array<RuleModel>> {
    let emitter = new EventEmitter()
    this.ref.once('value', (snap) => {
      let rules = snap['val']()
      let parsed = []
      Object.keys(rules).forEach((key) => {
        let rule = RuleService.fromSnapshot(key, snap.child(key))
        parsed.push(rule)
      })
      emitter.emit(parsed)
    }, (e)=> {
      console.log("RuleService", "list", "error", e)
      throw e
    })
    return emitter
  }

  add(rule:RuleModel, cb:Function = null) {
    this.ref.push(RuleService.toJson(rule), (e, resultSnapshot) => {
      if (e) {
        throw e
      }
      rule.snapshot = resultSnapshot
      rule.key = resultSnapshot.key()
      if (cb) {
        cb(rule)
      }
    })
  }

  save(rule:RuleModel, cb:Function = null) {
    if (!rule.isValid()) {
      throw new Error("Rule is not valid, cannot save.")
    }
    if (!rule.isPersisted()) {
      this.add(rule, cb)
    } else {
      this.ref.child(rule.key).set(RuleService.toJson(rule), ()=> {
        if (cb) {
          cb(rule)
        }
      })
    }
  }

  remove(rule:RuleModel, cb:Function=null) {
    if (rule.isPersisted()) {
      rule.snapshot.ref().remove((key)=> {
        if(cb){
          cb()
        }
      }).catch((e) => {
        console.log("Error removing rule", e)
        if(cb){
          cb(e)
        }
        throw e
      })
    }
  }
}

