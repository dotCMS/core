/// <reference path="../../../../jspm_packages/npm/angular2@2.0.0-alpha.44/angular2.d.ts" />

import {NgFor, NgIf, Component, Directive, View, Inject} from 'angular2/angular2';

import {SetSessionValueAction} from './actionlets/set-session-value-actionlet/set-session-value-action'
import {ActionTypesProvider, ActionTypeModel} from "api/rule-engine/ActionType"
import {ActionService, ActionModel} from "api/rule-engine/Action";

import {Dropdown, DropdownModel, DropdownOption} from 'view/components/semantic/modules/dropdown/dropdown'

@Component({
  selector: 'rule-action',
  properties: ["action"]
})
@View({
  template: `<div flex="grow" layout="column" class="cw-rule-action cw-entry">
  <div flex="grow" layout="row" layout-align="space-between-center">

    <cw-input-dropdown [model]="actionTypesDropdown" (change)="handleActionTypeChange($event)"></cw-input-dropdown>

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
  directives: [NgIf, NgFor, SetSessionValueAction, Dropdown],
})
export class RuleActionComponent {
  _action:ActionModel;
  actionTypesDropdown:DropdownModel

  private typesProvider:ActionTypesProvider
  private actionService:ActionService;

  constructor( typesProvider:ActionTypesProvider, actionService:ActionService){
    this.actionService = actionService;
    this.actionTypesDropdown = new DropdownModel('actionType', "Select an Action", [], [])

    this.typesProvider = typesProvider
    let action  = new ActionModel()
    action.actionType = new ActionTypeModel()
    this.action = action;

    typesProvider.promise.then(()=> {
      let opts = []
      typesProvider.ary.forEach((type)=>{
        opts.push(new DropdownOption(type.id))
      })
      this.actionTypesDropdown.addOptions(opts)
    })
  }

  set action(action:ActionModel){
    this._action = action
    if(this._action.actionType){
      this.actionTypesDropdown.selected = [this._action.actionType.id]
    }
    action.onChange.subscribe((self)=>{
      if(action.isValid() && action.isPersisted()){
        this.actionService.save(action)
      }
      if(this._action.actionType){
        this.actionTypesDropdown.selected = [this._action.actionType.id]
      }
    })
  }

  get action():ActionModel {
    return this._action
  }
  handleActionTypeChange(event){
    this.action.actionType = this.typesProvider.getType(event.target.model.selected[0])
    //this.action.clearParameters()
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
