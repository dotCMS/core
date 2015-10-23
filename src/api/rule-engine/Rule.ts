import {Inject, EventEmitter} from 'angular2/angular2';

import {ApiRoot} from 'api/persistence/ApiRoot';
import {CwEvent} from "api/util/CwEvent";
import {CwModel} from "api/util/CwModel";

import {EntityMeta} from 'api/persistence/EntityBase'


export class RuleModel extends CwModel{

  private _name:string
  private _enabled:boolean
  private _fireOn:string


  conditionGroups:Array<any>
  actions:Array<any>

  constructor(key:string = null) {
    super(key)
    this._fireOn = 'EVERY_PAGE'
    this.valid = this.isValid()
  }

  get fireOn():string {
    return this._fireOn;
  }

  set fireOn(value:string) {
    this._fireOn = value;
    this._changed();
  }


  get enabled():boolean {
    return this._enabled;
  }

  set enabled(value:boolean) {
    this._enabled = value;
    this._changed();
  }

  get name():string {
    return this._name;
  }

  set name(value:string) {
    this._name = value;
    this._changed();
  }

  isValid() {
    let valid = !!this.name
    valid = valid && this.name.trim().length > 0
    return valid
  }

}

export class RuleService {
  ref:EntityMeta
  onAdd:EventEmitter

  constructor(@Inject(ApiRoot) apiRoot:ApiRoot) {
    this.onAdd = new EventEmitter()
    this.ref = apiRoot.defaultSite.child('ruleengine/rules')
    this.init();
  }

  init() {
    this.ref.once('value', (snap) => {
      let rules = snap['val']()
      Object.keys(rules).forEach((key) => {
        let ruleVal = rules[key]
        let rule = this._fromRefVal(key, ruleVal)
        this._watchRule(rule)
        this.onAdd.next(new CwEvent("ruleAdded", rule))
      })
    });

    this.ref.on('child_added', (snap) => {
      debugger
      //this.ref= this.rules.concat(snap)
    })
    this.ref.on('child_removed', (snap) => {
      debugger
      //this.rules = this.rules.filter((rule)=> {
      //  return rule.key() !== snap.key()
      //})
    })

  }

  _fromRefVal(key, val):RuleModel {
    let rule = new RuleModel(key)
    rule.enabled = val.enabled
    rule.fireOn = val.fireOn
    rule.key = val.id
    rule.name = val.name
    rule.priority = val.priority
    return rule

  }

  _watchRule(rule) {
    rule.validityChange.observer((event)=> {
      if (event.target.valid) {
        this.save(rule)
      }
    })
  }

  add() {
    let rule = new RuleModel();
    this._watchRule(rule)
  }

  save(rule:RuleModel) {
    if (rule.isValid()) {
      this.ref.push(rule).catch((e)=> {
        console.log("Error pushing new rule: ", e)
        throw e
      })
    } else {
      throw new Error("Rule is not valid, cannot save.")
    }
  }


}

