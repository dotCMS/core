import {NgFor, NgIf, Component, Directive, View, Inject} from 'angular2/angular2';

import {SetSessionValueAction} from './action-types/set-session-value-action/set-session-value-action'

import {ActionModel} from "../../../api/rule-engine/Action";
import {ActionTypesProvider} from "../../../api/rule-engine/ActionType";
import {ActionService} from "../../../api/rule-engine/Action";
import {ActionTypeModel} from "../../../api/rule-engine/ActionType";
import {Dropdown, DropdownModel, DropdownOption} from "../semantic/modules/dropdown/dropdown";

@Component({
  selector: 'rule-action',
  properties: ["action"]
})
@View({
  template: `<div flex layout="row" layout-align="space-between-center" class="cw-rule-action cw-entry">
<div flex="30" layout="row" layout-align="end-center" class="cw-row-start-area">
    <cw-input-dropdown class="cw-action-type-dropdown" [model]="actionTypesDropdown" (change)="handleActionTypeChange($event)"></cw-input-dropdown>
  </div>


  <cw-set-session-value-action flex="60"
                               *ng-if="action.actionType.id == 'SetSessionAttributeActionlet'"
                               [action]="action"
                               (config-change)="actionConfigChanged($event)">

  </cw-set-session-value-action>
  <div flex="5" class="cw-btn-group">
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
