import {NgFor, NgIf, Component, Directive, View, Inject} from 'angular2/angular2';

import {ServersideAction} from './action-types/serverside-action/serverside-action'

import {ActionModel} from "../../../api/rule-engine/Action";
import {ActionTypeService, ActionTypeModel} from "../../../api/rule-engine/ActionType";
import {ActionService} from "../../../api/rule-engine/Action";
import {Dropdown, DropdownModel, DropdownOption} from "../semantic/modules/dropdown/dropdown";
import {I18nService} from "../../../api/system/locale/I18n";

@Component({
  selector: 'rule-action',
  properties: ["action"]
})
@View({
  template: `<div flex layout="row" layout-align="space-between-center" class="cw-rule-action cw-entry">
  <div flex="30" layout="row" layout-align="end-center" class="cw-row-start-area">
    <cw-input-dropdown class="cw-action-type-dropdown" [model]="actionTypesDropdown" (change)="handleActionTypeChange($event)"></cw-input-dropdown>
  </div>


  <cw-serverside-action flex="60"
                        [model]="action"
                        (config-change)="actionConfigChanged($event)">

  </cw-serverside-action>
  <div flex="5" class="cw-btn-group">
    <div class="ui basic icon buttons">
      <button class="ui button" aria-label="Delete Action" (click)="removeAction()">
        <i class="trash icon"></i>
      </button>
    </div>
  </div>
</div>`,
  directives: [NgIf, NgFor, ServersideAction, Dropdown],
})
export class RuleActionComponent {
  _action:ActionModel;
  actionTypesDropdown:DropdownModel

  private typeService:ActionTypeService
  private actionService:ActionService;
  private _msgService:I18nService;

  constructor( msgService:I18nService, typeService:ActionTypeService, actionService:ActionService){
    this._msgService = msgService;
    this.actionService = actionService;
    this.actionTypesDropdown = new DropdownModel('actionType', "Select an Action", [], [])

    this.typeService = typeService
    let action  = new ActionModel()
    action.actionType = new ActionTypeModel()
    this.action = action;

    typeService.onAdd.subscribe((actionType:ActionTypeModel) => {
        msgService.get('en-US', 'com.dotmarketing.osgi.ruleengine', (magic)=>{
          console.log("Resource Received: ", magic)
        })
        this.actionTypesDropdown.addOptions([new DropdownOption(actionType.key, actionType, actionType.i18nKey)])
    })
  }

  set action(action:ActionModel){
    this._action = action
    if(this._action.actionType){
      this.actionTypesDropdown.selected = [this._action.actionType.key]
    }
    action.onChange.subscribe((self)=>{
      if(action.isValid() && action.isPersisted()){
        this.actionService.save(action)
      }
      if(this._action.actionType){
        this.actionTypesDropdown.selected = [this._action.actionType.key]
      }
    })
  }

  get action():ActionModel {
    return this._action
  }

  handleActionTypeChange(event){
    this.action.clearParameters()
    this.action.actionType = event.target.model.selectedValues()[0]
    if(!this.action.isPersisted()){
      this.actionService.add(this.action)
    }
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
