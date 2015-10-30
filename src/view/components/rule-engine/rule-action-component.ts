/// <reference path="../../../../jspm_packages/npm/angular2@2.0.0-alpha.44/angular2.d.ts" />

import {NgFor, NgIf, Component, Directive, View, Inject} from 'angular2/angular2';

import {SetSessionValueAction} from './actionlets/set-session-value-actionlet/set-session-value-action'
import {ActionTypesProvider, ActionTypeModel} from "api/rule-engine/ActionType"
import {ActionService, ActionModel} from "api/rule-engine/Action";


@Component({
  selector: 'rule-action',
  properties: ["action"]
})
@View({
  template: `<div flex="grow" layout="column" class="cw-rule-action cw-entry">
  <div flex="grow" layout="row" layout-align="space-between-center">
    <select class="form-control clause-selector" [value]="action.actionType.actionTypeId" (change)="setActionType($event.target.value)">
      <option value="NoSelection" [selected]="action.actionType.id == 'NoSelection'">Select ActionType</option>
      <option value="{{actionTypeModel.id}}" [selected]="action.actionType.id == actionTypeModel.id" *ng-for="var actionTypeModel of actionTypes">{{actionTypeModel.name}}
      </option>
    </select>

    <cw-set-session-value-action flex="grow"
                                  *ng-if="action.actionType.id == 'SetSessionAttributeActionlet'"
                                 [action]="action"
                                 (config-change)="actionConfigChanged($event)">

    </cw-set-session-value-action>
    <div class="cw-btn-group">
      <div class="ui basic icon buttons">
        <button class="ui button" aria-label="Delete Action" (click)="removeAction()">
          <i class="trash icon"></i>
        </button>
      </div>
    </div>
  </div>`,
  directives: [NgIf, NgFor, SetSessionValueAction],
})
export class RuleActionComponent {
  _action:ActionModel;
  actionTypes:Array<any>;
  private typesProvider:ActionTypesProvider
  private actionService:ActionService;

  constructor( typesProvider:ActionTypesProvider, actionService:ActionService){
    this.actionService = actionService;
    this.actionTypes = []
    this.typesProvider = typesProvider
    this._action = new ActionModel()
    typesProvider.promise.then(()=> {
      this.actionTypes = typesProvider.ary
    })
    this._action.actionType = new ActionTypeModel()
  }

  set action(action:ActionModel){
    this._action = action
    action.onChange.subscribe((self)=>{
      if(action.isValid() && action.isPersisted()){
        this.actionService.save(action)
      }
    })
  }

  get action():ActionModel {
    return this._action
  }

  setActionType(value){
    this._action.actionType = this.actionTypes.find((type) => {
      return type.id === value
    })
  }

  actionConfigChanged(event){
    if(event.type == 'actionParameterChanged'){
      Object.keys(event.params).forEach((key)=>{
        this.action.setParameter(key, event.params[key])
      })
    }
  }

  removeAction() {
    this.actionService.remove(this.action)
  }
}
