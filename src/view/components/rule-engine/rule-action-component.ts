/// <reference path="../../../../typings/angular2/angular2.d.ts" />
/// <reference path="../../../../typings/coreweb/coreweb-api.d.ts" />

import {NgFor, NgIf, Component, Directive, View} from 'angular2/angular2';

import {ruleActionTemplate} from './templates/index'
import {SetSessionValueAction} from './actionlets/set-session-value-actionlet/set-session-value-action'
import {ActionTypeModel, ActionConfigModel, RuleActionModel} from "api/rule-engine/rule-action"


var actionletsAry = []
var actionTypesMap:Map<string,ActionTypeModel> = new Map()
var actionletsPromise;


export let initActionlets = function () {
  let actionletsRef:EntityMeta = new EntityMeta('/api/v1/system/ruleengine/actionlets')
  actionletsPromise = new Promise((resolve, reject) => {
    actionletsRef.once('value', (snap) => {
      let actionlets = snap['val']()
      let results = (Object.keys(actionlets).map((key) => {
        let actionType = actionlets[key]
        actionTypesMap.set(key, new ActionTypeModel(key, actionType.i18nKey))
        return actionlets[key]
      }))

      Array.prototype.push.apply(actionletsAry,results);
      resolve(snap);
    })
  });
}
@Component({
  selector: 'rule-action',
  properties: ["actionMeta"]
})
@View({
  template: ruleActionTemplate,
  directives: [NgIf, NgFor, SetSessionValueAction],
})
export class RuleActionComponent {
  _actionMeta:any;
  actionModel:RuleActionModel;
  actionTypes:Array<any>;

  constructor() {
    this.actionTypes = []
    actionletsPromise.then(()=> {
      this.actionTypes = actionletsAry
    })
    this.actionModel = new RuleActionModel()
  }

  onSetActionMeta(snapshot){
    let val = snapshot.val()
    this.actionModel.id = val.id
    this.actionModel.name = val.name
    this.actionModel.owningRuleId = val.owningRule
    this.actionModel.priority = val.priority
    this.actionModel.setActionType(actionTypesMap.get(val.actionlet), val.parameters)
  }


  set actionMeta(actionMeta) {
    this._actionMeta = actionMeta
    this._actionMeta.once('value', this.onSetActionMeta.bind(this))
  }

  get actionMeta() {
    return this._actionMeta;
  }

  setActionlet(actionletId){
    this.actionModel.setActionType(actionTypesMap.get(actionletId))
    this.updateAction()
  }

  actionConfigChanged(event){
    this.actionModel.actionConfig = event.ngTarget.value
    this.updateAction()
  }

  updateAction() {
    this.actionMeta.set(this.actionModel.out())
  }

  removeRuleAction() {
    this.actionMeta.remove()
  }
}
