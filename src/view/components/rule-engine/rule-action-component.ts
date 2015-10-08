/// <reference path="../../../../typings/angular2/angular2.d.ts" />
/// <reference path="../../../../typings/coreweb/coreweb-api.d.ts" />

import {NgFor, NgIf, Component, Directive, View, Inject} from 'angular2/angular2';

import {ruleActionTemplate} from './templates/index'
import {SetSessionValueAction} from './actionlets/set-session-value-actionlet/set-session-value-action'
import {ActionTypeModel, ActionConfigModel, RuleActionModel} from "api/rule-engine/rule-action"
import {ApiRoot} from 'api/persistence/ApiRoot';
import {ActionTypesProvider} from 'api/rule-engine/ActionTypes';


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
  typesProvider:ActionTypesProvider
  actionTypes:Array<any>;

  constructor(@Inject(ApiRoot) apiRoot:ApiRoot, @Inject(ActionTypesProvider) typesProvider:ActionTypesProvider){
    this.actionTypes = []
    this.typesProvider = typesProvider
    typesProvider.promise.then(()=> {
      this.actionTypes = typesProvider.ary
    })
    this.actionModel = new RuleActionModel()
  }

  onSetActionMeta(snapshot){
    let val = snapshot.val()
    this.actionModel.id = val.id
    this.actionModel.name = val.name
    this.actionModel.owningRuleId = val.owningRule
    this.actionModel.priority = val.priority
    this.actionModel.setActionType(this.typesProvider.map.get(val.actionlet), val.parameters)
  }


  set actionMeta(actionMeta) {
    this._actionMeta = actionMeta
    this._actionMeta.once('value', this.onSetActionMeta.bind(this))
  }

  get actionMeta() {
    return this._actionMeta;
  }

  setActionlet(actionletId){
    this.actionModel.setActionType(this.typesProvider.map.get(actionletId))
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
