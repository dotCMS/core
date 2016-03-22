import {Component, EventEmitter, ElementRef, Input, Output, ChangeDetectionStrategy} from 'angular2/core';
import {
    CORE_DIRECTIVES, Control, Validators, FORM_DIRECTIVES, NgFormModel, FormBuilder,
    ControlGroup
} from 'angular2/common';

import {Observable} from 'rxjs/Rx'


import {RuleActionComponent} from './rule-action-component';
import {ConditionGroupComponent} from './rule-condition-group-component';

import {InputToggle} from '../../../view/components/input/toggle/InputToggle'

import {
    RuleModel, RULE_UPDATE_ENABLED_STATE,
    RULE_UPDATE_NAME,
    RULE_UPDATE_FIRE_ON,
    RULE_DELETE,
    RULE_RULE_ACTION_UPDATE_TYPE,
    RULE_RULE_ACTION_UPDATE_PARAMETER,
    V_RULE_UPDATE_EXPANDED_STATE, RULE_CONDITION_UPDATE_PARAMETER, RULE_CONDITION_UPDATE_OPERATOR,
    RULE_CONDITION_UPDATE_TYPE, ConditionGroupModel, ActionModel, RULE_RULE_ACTION_DELETE, RULE_RULE_ACTION_CREATE,
    RULE_CONDITION_GROUP_CREATE
} from "../../../api/rule-engine/Rule";

import {Dropdown, InputOption} from "../semantic/modules/dropdown/dropdown";
import {InputText} from "../semantic/elements/input-text/input-text";
import {I18nService} from "../../../api/system/locale/I18n";
import {UserModel} from "../../../api/auth/UserModel";
import {ApiRoot} from "../../../api/persistence/ApiRoot";
import {
    ConditionActionEvent,
    RuleActionActionEvent, RuleActionEvent, ConditionGroupActionEvent
} from "./rule-engine.container";
import {ServerSideTypeModel} from "../../../api/rule-engine/ServerSideFieldModel";


const I8N_BASE:string = 'api.sites.ruleengine'
var rsrc = {
  fireOn: {
    EVERY_PAGE: 'Every Page',
    ONCE_PER_VISIT: 'Once per visit',
    ONCE_PER_VISITOR: 'Once per visitor',
    EVERY_REQUEST: 'Every Request'
  }
}

@Component({
  selector: 'rule',
  directives: [CORE_DIRECTIVES, FORM_DIRECTIVES, NgFormModel, InputToggle, RuleActionComponent, ConditionGroupComponent, InputText, Dropdown, InputOption],
  template: `<form [ngFormModel]="formModel" #rf="ngForm">
  <div class="ui dimmer modals page transition visible active" *ngIf="showAddToBundle">
    <div class="ui modal" style="display: block; width: 400px; margin: -103px 0 0 -200px;">
      <i class="close icon" (click)="toggleAddToBundleModal($event)"></i>
      <div class="header">Add to Bundle</div>
      <div class="content">
        <div class="field">
          <select class="ui search dropdown">
            <option>Select a option</option>
          </select>
        </div>
      </div>
      <div class="actions">
        <div class="ui black deny button" (click)="toggleAddToBundleModal($event)">
          Cancel
        </div>
        <div class="ui positive right labeled icon button">
          Add to Bundle
          <i class="checkmark icon"></i>
        </div>
      </div>
    </div>
  </div>
  <div class="cw-rule" [class.cw-hidden]="hidden" [class.cw-disabled]="!rule.enabled" [class.cw-saving]="saving" [class.cw-saved]="saved" [class.cw-out-of-sync]="!saved && !saving">
  <div flex layout="row" class="cw-header" *ngIf="!hidden" (click)="setRuleExpandedState(!rule._expanded)">
    <div flex="70" layout="row" layout-align="start center" class="cw-header-info" >
      <i flex="none" class="caret icon cw-rule-caret large" [class.right]="!rule._expanded" [class.down]="rule._expanded" aria-hidden="true"></i>
      <div flex="70" layout="column">
      <cw-input-text class="cw-rule-name-input"
                     focused="{{rule.key == null}}"
                     placeholder="{{rsrc('inputs.name.placeholder') | async}}"
                     ngControl="name"
                     (click)="$event.stopPropagation()" #fName="ngForm">
      </cw-input-text>
      <div flex="50" [hidden]="!fName.touched || fName.valid" class="name cw-warn basic label">Name is required</div>
      </div>
      <span class="cw-fire-on-label" *ngIf="!hideFireOn">{{rsrc('inputs.fireOn.label') | async}}</span>
      <cw-input-dropdown flex="none"
                         *ngIf="!hideFireOn"
                         class="cw-fire-on-dropdown"
                         [value]="fireOn.value"
                         placeholder="{{fireOn.placeholder | async}}"
                         (change)="updateFireOn.emit({type: 'RULE_UPDATE_FIRE_ON', payload:{rule:rule, value:$event}})"
                         (click)="$event.stopPropagation()">
        <cw-input-option
            *ngFor="#opt of fireOn.options"
            [value]="opt.value"
            [label]="opt.label | async"
            icon="{{opt.icon}}"></cw-input-option>
      </cw-input-dropdown>
    </div>
    <div flex="30" layout="row" layout-align="end center" class="cw-header-actions" >
      <span class="cw-rule-status-text" title="{{statusText()}}">{{statusText(30)}}</span>
      <cw-toggle-input class="cw-input"
                       [on-text]="rsrc('inputs.onOff.on.label') | async"
                       [off-text]="rsrc('inputs.onOff.off.label') | async"
                       [disabled]="!saved"
                       [value]="rule.enabled"
                       (change)="setRuleEnabledState($event)"
                       (click)="$event.stopPropagation()">
      </cw-toggle-input>
      <div class="cw-btn-group">
        <div class="ui basic icon buttons">
          <button class="ui button cw-delete-rule" aria-label="More Actions" (click)="toggleExtraMenu($event)">
            <i class="ellipsis vertical icon"></i>
          </button>
          <button class="ui button cw-add-group" arial-label="Add Group" (click)="onCreateConditionGroupClicked(); setRuleExpandedState(true); $event.stopPropagation()" [disabled]="!rule.isPersisted()">
            <i class="plus icon" aria-hidden="true"></i>
          </button>
        </div>
      </div>
      <div class="ui vertical menu" *ngIf="showExtraMenu">
        <a class="item" (click)=toggleAddToBundleModal($event)>Add to bundle</a>
        <a class="item" (click)="deleteRuleClicked($event)">Delete rule</a>
      </div>
    </div>
  </div>
  <div class="cw-accordion-body" *ngIf="rule._expanded">
    <condition-group *ngFor="#group of rule._conditionGroups; var i=index"
                     [rule]="rule"
                     [group]="group"
                     [conditionTypes]="conditionTypes"
                     [groupIndex]="i"
                     (createCondition)="createCondition.emit($event)"
                     (deleteCondition)="onDeleteCondition($event, group)"
                     (updateConditionGroupOperator)="updateConditionGroupOperator.emit($event)"
                     (updateConditionType)="onUpdateConditionType($event, group)"
                     (updateConditionParameter)="onUpdateConditionParameter($event, group)"
                     (updateConditionOperator)="onUpdateConditionOperator($event, group)"
                     ></condition-group>
    <div class="cw-action-group">
      <div class="cw-action-separator">
        {{rsrc('inputs.action.firesActions') | async}}
      </div>
      <div flex layout="column" class="cw-rule-actions">
        <div layout="row" class="cw-action-row" *ngFor="#ruleAction of ruleActions; #i=index">
          <rule-action flex layout="row" [action]="ruleAction" [index]="i" 
              [ruleActionTypes]="ruleActionTypes"
              (updateRuleActionType)="onUpdateRuleActionType($event)"
               (updateRuleActionParameter)="onUpdateRuleActionParameter($event)"
              (deleteRuleAction)="onDeleteRuleAction($event)"></rule-action>
          <div class="cw-btn-group cw-add-btn">
            <div class="ui basic icon buttons" *ngIf="i === (ruleActions.length - 1)">
              <button class="cw-button-add-item ui button" arial-label="Add Action" (click)="onCreateRuleAction();" [disabled]="!ruleAction.isPersisted()">
                <i class="plus icon" aria-hidden="true"></i>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</div>
</form>
`,
  changeDetection: ChangeDetectionStrategy.Default
})
class RuleComponent {
  @Input() rule:RuleModel
  @Input() saved:boolean
  @Input() saving:boolean
  @Input() errors:{[key:string]:any}
  @Input() ruleActions:ActionModel[]
  @Input() ruleActionTypes:{[key:string]: ServerSideTypeModel} = {}
  @Input() conditionTypes:{[key:string]: ServerSideTypeModel}

  @Input() hidden:boolean = false

  @Output() deleteRule:EventEmitter<RuleActionEvent> = new EventEmitter(false)
  @Output() updateExpandedState:EventEmitter<RuleActionEvent> = new EventEmitter(false)
  @Output() updateName:EventEmitter<RuleActionEvent> = new EventEmitter(false)
  @Output() updateEnabledState:EventEmitter<RuleActionEvent> = new EventEmitter(false)
  @Output() updateFireOn:EventEmitter<RuleActionEvent> = new EventEmitter(false)

  @Output() createRuleAction:EventEmitter<RuleActionActionEvent> = new EventEmitter(false)
  @Output() updateRuleActionType:EventEmitter<RuleActionActionEvent> = new EventEmitter(false)
  @Output() updateRuleActionParameter:EventEmitter<RuleActionActionEvent> = new EventEmitter(false)
  @Output() deleteRuleAction:EventEmitter<RuleActionActionEvent> = new EventEmitter(false)

  @Output() onUpdateConditionGroupOperator:EventEmitter<ConditionGroupActionEvent> = new EventEmitter(false)
  @Output() createConditionGroup:EventEmitter<ConditionGroupActionEvent> = new EventEmitter(false)

  @Output() createCondition:EventEmitter<ConditionActionEvent> = new EventEmitter(false)
  @Output() deleteCondition:EventEmitter<ConditionActionEvent> = new EventEmitter(false)
  @Output() updateConditionType:EventEmitter<ConditionActionEvent> = new EventEmitter(false)
  @Output() updateConditionParameter:EventEmitter<ConditionActionEvent> = new EventEmitter(false)
  @Output() updateConditionOperator:EventEmitter<ConditionActionEvent> = new EventEmitter(false)

  private _updateEnabledStateDelay:EventEmitter<{type:string, payload: {rule:RuleModel, value:boolean}}> = new EventEmitter(false)

  hideFireOn:boolean
  formModel:ControlGroup
  elementRef:ElementRef

  private fireOn:any
  private _rsrcCache:{[key:string]:Observable<string>}
  private _user:UserModel
  resources:I18nService
  private showExtraMenu:Boolean;
  private showAddToBundle:Boolean;


  constructor(fb:FormBuilder,
              user:UserModel,
              elementRef:ElementRef,
              resources:I18nService,
              apiRoot:ApiRoot) {
    this._user = user
    this.elementRef = elementRef
    this.resources = resources
    this._rsrcCache = {}
    this.hideFireOn = apiRoot.hideFireOn
    this.showExtraMenu = false
    this.showAddToBundle = false;


    /* Need to delay the firing of the state change toggle, to give any blur events time to fire. */
    this._updateEnabledStateDelay.debounceTime(20).subscribe((event:RuleActionEvent)=> {
      this.updateEnabledState.emit(event)
    })

    this.fireOn = {
      value: 'EVERY_PAGE',
      placeholder: this.rsrc('inputs.fireOn.placeholder', "Select One"),
      options: [
        {value: 'EVERY_PAGE', label: this.rsrc('inputs.fireOn.options.EveryPage')},
        {value: 'ONCE_PER_VISIT', label: this.rsrc('inputs.fireOn.options.OncePerVisit')},
        {value: 'ONCE_PER_VISITOR', label: this.rsrc('inputs.fireOn.options.OncePerVisitor')},
        {value: 'EVERY_REQUEST', label: this.rsrc('inputs.fireOn.options.EveryRequest')}
      ]
    }
    this.initFormModel(fb)

  }

  initFormModel(fb:FormBuilder) {
    let vFns = []
    vFns.push(Validators.required)
    vFns.push(Validators.minLength(3))
    this.formModel = fb.group({
      name: new Control(this.rule ? this.rule.name : '', Validators.compose(vFns))
    })
  }

  rsrc(subkey:string, defVal:string = null) {
    let msgObserver = this._rsrcCache[subkey]
    if (!msgObserver) {
      msgObserver = this.resources.get(I8N_BASE + '.rules.' + subkey, defVal)
      this._rsrcCache[subkey] = msgObserver
    }
    return msgObserver
  }

  ngOnChanges(change) {
    console.log("RuleComponent", "ngOnChanges", change, change.rule)
    if (change.rule) {
      let rule = this.rule
      let ctrl:Control = <Control>this.formModel.controls['name']
      ctrl.updateValue(this.rule.name, {})
      ctrl.valueChanges.debounceTime(250).subscribe((name:string)=> {
        if (ctrl.valid) {
          this.updateName.emit({type: RULE_UPDATE_NAME, payload: {rule: this.rule, value: name}})
        }
      })
      if (rule.isPersisted()) {
        this.fireOn.value = rule.fireOn
      }
    }

  }

  statusText(length:number=0) {
    let t = "";
    if (this.saved)
    { t = "All changes saved"
    }
    else if(this.saving){
      t = "Saving..."
    } else if(this.errors){
      t = this.errors['invalid'] || this.errors['serverError'] || "Unsaved changes..."
    }
    if(length){
      t = t.substring(0, length) + '...'
    }
    return t;
  }

  setRuleExpandedState(expanded:boolean) {
    this.updateExpandedState.emit({type: V_RULE_UPDATE_EXPANDED_STATE, payload: {rule:this.rule, value:expanded}})
  }

  setRuleEnabledState(enabled:boolean) {
    this._updateEnabledStateDelay.emit({
      type: RULE_UPDATE_ENABLED_STATE,
      payload: {rule: this.rule, value: enabled}
    })
  }

  onCreateRuleAction(){
    console.log("RuleComponent", "onCreateRuleAction")
    this.createRuleAction.emit( { type:RULE_RULE_ACTION_CREATE, payload:{rule:this.rule}} )
  }

  onDeleteCondition(event:ConditionActionEvent, conditionGroup:ConditionGroupModel){
    debugger
    Object.assign(event.payload, { rule:this.rule, conditionGroup:conditionGroup })
    this.deleteCondition.emit( event )
  }

  onCreateConditionGroupClicked(){
    let len = this.rule._conditionGroups.length
    let priority:number = len ? this.rule._conditionGroups[len - 1].priority : 1;
    this.createConditionGroup.emit({type:RULE_CONDITION_GROUP_CREATE, payload:{rule:this.rule, priority}})
  }

  onCreateCondition(event:ConditionActionEvent){
    Object.assign(event.payload, { rule:this.rule })
    this.createCondition.emit( event )
  }

  onUpdateRuleActionType(event:{type:string, payload:{value:string, index:number}}){
    console.log("RuleComponent", "onUpdateRuleActionType")
    this.updateRuleActionType.emit( { type:RULE_RULE_ACTION_UPDATE_TYPE, payload:Object.assign({rule:this.rule}, event.payload) } )
  }

  onUpdateRuleActionParameter(event){
    console.log("RuleComponent", "onUpdateRuleActionParameter")
    this.updateRuleActionParameter.emit( { type: RULE_RULE_ACTION_UPDATE_PARAMETER, payload:Object.assign({rule:this.rule}, event.payload) } )
  }

  onDeleteRuleAction(event:{type:string, payload:{value:string, index:number}}){
    console.log("RuleComponent", "onDeleteRuleAction")
    this.deleteRuleAction.emit( { type:RULE_RULE_ACTION_DELETE, payload:Object.assign({rule:this.rule}, event.payload) } )
  }



  onUpdateConditionType(event:{type:string, payload:{value:string, index:number}}, conditionGroup:ConditionGroupModel){
    console.log("RuleComponent", "onUpdateConditionType")
    this.updateConditionType.emit( { type:RULE_CONDITION_UPDATE_TYPE, payload:Object.assign({rule:this.rule, conditionGroup: conditionGroup}, event.payload) } )
  }

  onUpdateConditionParameter(event, conditionGroup:ConditionGroupModel){
    console.log("RuleComponent", "onUpdateConditionParameter")
    this.updateConditionParameter.emit( { type: RULE_CONDITION_UPDATE_PARAMETER, payload:Object.assign({rule:this.rule, conditionGroup: conditionGroup}, event.payload) } )
  }

  onUpdateConditionOperator(event, conditionGroup:ConditionGroupModel){
    console.log("RuleComponent", "onUpdateConditionOperator")
    this.updateConditionOperator.emit( { type: RULE_CONDITION_UPDATE_OPERATOR, payload:Object.assign({rule:this.rule, conditionGroup: conditionGroup}, event.payload) } )
  }

  deleteRuleClicked(event:any) {
    event.stopPropagation()
    let noWarn = this._user.suppressAlerts || (event.altKey && event.shiftKey)
    if (!noWarn) {
      noWarn = this.ruleActions.length === 1 && !this.ruleActions[0].isPersisted()
      noWarn = noWarn && this.rule._conditionGroups.length === 1
      if (noWarn) {
        let conditions = this.rule._conditionGroups[0].conditions
        let keys = Object.keys(conditions)
        noWarn = noWarn && (keys.length === 0)
      }
    }

    if (noWarn || confirm('Are you sure you want delete this rule?')) {
      this.deleteRule.emit({type: RULE_DELETE, payload: {rule: this.rule}})
    }
  }

  toggleExtraMenu(event:any) {
    event.stopPropagation()
    this.showExtraMenu = !this.showExtraMenu
  }

  toggleAddToBundleModal(event:any) {
    event.stopPropagation()
    this.showAddToBundle = !this.showAddToBundle
    if (this.showExtraMenu) {
      this.showExtraMenu = false
    }
  }
}

export {RuleComponent}