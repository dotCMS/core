import { Component, EventEmitter, ElementRef, Input, Output} from 'angular2/core';
import {
    CORE_DIRECTIVES, Control, Validators, FORM_DIRECTIVES, NgFormModel, FormBuilder,
    ControlGroup
} from 'angular2/common';

import {Observable} from 'rxjs/Rx'


import {RuleActionComponent} from './rule-action-component';
import {ConditionGroupComponent} from './rule-condition-group-component';

import {InputToggle} from '../../../view/components/input/toggle/InputToggle'

import {RuleService, RuleModel} from "../../../api/rule-engine/Rule";
import {ActionService, ActionModel} from "../../../api/rule-engine/Action";
import {ConditionGroupModel, ConditionGroupService} from "../../../api/rule-engine/ConditionGroup";

import {Dropdown, InputOption} from "../semantic/modules/dropdown/dropdown";
import {InputText} from "../semantic/elements/input-text/input-text";
import {ServerSideTypeModel} from "../../../api/rule-engine/ServerSideFieldModel";
import {I18nService} from "../../../api/system/locale/I18n";
import {UserModel} from "../../../api/auth/UserModel";


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
  <div class="cw-rule" [class.cw-hidden]="hidden" [class.cw-disabled]="!rule.enabled">
  <div flex layout="row" class="cw-header" *ngIf="!hidden" (click)="toggleCollapsed()">
    <div flex="70" layout="row" layout-align="start center" class="cw-header-info" *ngIf="!hidden">
      <i flex="none" class="caret icon cw-rule-caret large" [class.right]="collapsed" [class.down]="!collapsed" aria-hidden="true"></i>
      <div flex="70" layout="column">
      <cw-input-text class="cw-rule-name-input"
                     focused="{{rule.key == null}}"
                     placeholder="{{rsrc('inputs.name.placeholder') | async}}"
                     ngControl="name"
                     (click)="$event.stopPropagation()" #fName="ngForm">
      </cw-input-text>
      <div flex="50" [hidden]="!fName.touched || fName.valid" class="name cw-warn basic label">Name is required</div>
      </div>
      <span class="cw-fire-on-label">{{rsrc('inputs.fireOn.label') | async}}</span>
      <cw-input-dropdown flex="none"
                         class="cw-fire-on-dropdown"
                         [value]="fireOn.value"
                         placeholder="{{fireOn.placeholder | async}}"
                         (change)="onFireOnChange($event)"
                         (click)="$event.stopPropagation()">
        <cw-input-option
            *ngFor="#opt of fireOn.options"
            [value]="opt.value"
            [label]="opt.label | async"
            icon="{{opt.icon}}"></cw-input-option>
      </cw-input-dropdown>
    </div>
    <div flex="30" layout="row" layout-align="end center" class="cw-header-actions" *ngIf="!hidden">
      <cw-toggle-input class="cw-input"
                       [on-text]="rsrc('inputs.onOff.on.label') | async"
                       [off-text]="rsrc('inputs.onOff.off.label') | async"
                       [value]="rule.enabled"
                       (change)="handleEnabledToggle($event)"
                       (click)="$event.stopPropagation()">
      </cw-toggle-input>
      <div class="cw-btn-group">
        <div class="ui basic icon buttons">
          <button class="ui button cw-delete-rule" aria-label="Delete Rule" (click)="removeRule($event)">
            <i class="trash icon"></i>
          </button>
          <button class="ui button cw-add-group" arial-label="Add Group" (click)="addGroup(); collapsed=false; $event.stopPropagation()" [disabled]="!rule.isPersisted()">
            <i class="plus icon" aria-hidden="true"></i>
          </button>
        </div>
      </div>
    </div>
  </div>
  <div class="cw-accordion-body" [class.cw-hidden]="collapsed">
    <condition-group *ngFor="var group of groups; var i=index"
                     [rule]="rule"
                     [group]="group"
                     [groupIndex]="i"
                     (remove)="onConditionGroupRemove($event)"
                     (change)="onConditionGroupChange($event)"></condition-group>
    <div class="cw-action-group">
      <div class="cw-action-separator">
        {{rsrc('inputs.action.firesActions') | async}}
      </div>
      <div flex layout="column" class="cw-rule-actions">
        <div layout="row" class="cw-action-row" *ngFor="var ruleAction of actions; #i=index">
          <rule-action flex layout="row" [action]="ruleAction" [index]="i" (change)="onActionChange($event)" (remove)="onActionRemove($event)"></rule-action>
          <div class="cw-btn-group cw-add-btn">
            <div class="ui basic icon buttons" *ngIf="i === (actions.length - 1)">
              <button class="cw-button-add-item ui button" arial-label="Add Action" (click)="addAction();" [disabled]="!ruleAction.isPersisted()">
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
})
class RuleComponent {
  @Input() rule:RuleModel
  @Input() hidden:boolean

  @Output() change:EventEmitter<RuleModel>
  @Output() remove:EventEmitter<RuleModel>

  collapsed:boolean
  actions:Array<ActionModel>
  groups:Array<ConditionGroupModel>

  elementRef:ElementRef
  private ruleService:RuleService
  private actionService:ActionService
  private _groupService:ConditionGroupService

  private fireOn:any
  private _rsrcCache:{[key:string]:Observable<string>}
  private _user:UserModel
  resources:I18nService

  formModel:ControlGroup

  constructor(fb:FormBuilder,
              user:UserModel,
              elementRef:ElementRef,
              ruleService:RuleService,
              actionService:ActionService,
              groupService:ConditionGroupService,
              resources:I18nService) {
    this._user = user
    this.change = new EventEmitter()
    this.remove = new EventEmitter()
    this.elementRef = elementRef
    this.actionService = actionService
    this.ruleService = ruleService;
    this._groupService = groupService
    this.resources = resources
    this._rsrcCache = {}

    this.groups = []
    this.actions = []

    this.hidden = false
    this.collapsed = false


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
      msgObserver = this.resources.get(I8N_BASE + '.rules.' + subkey, defVal )
      this._rsrcCache[subkey] = msgObserver.map(v => {
        return v
      })
    }
    return msgObserver

  }

  ngOnChanges(change) {
    if (change.rule) {
      let rule:RuleModel = change.rule.currentValue
      let ctrl:Control = <Control>this.formModel.controls['name']
      ctrl.updateValue(this.rule.name, {})
      ctrl.valueChanges.debounceTime(250).subscribe((name:string)=> {
        this.rule.name = name
        this.change.emit(this.rule)
      })
      if (rule.isPersisted()) {
        this._groupService.list(rule).subscribe((groups) => {
          this.groups = groups || []
          if (this.groups.length === 0) {
            this.addGroup()
          } else {
            this.sort()
          }
        })
        this.actionService.list(rule).subscribe((actions) => {
          this.actions = actions || []
          if (this.actions.length === 0) {
            this.addAction()
          } else {
            this.sort()
          }
        })
        this.fireOn.value = rule.fireOn
        this.collapsed = rule.isPersisted()
      } else {
        this.addGroup()
        this.addAction()
      }
    }
  }

  sort() {
    this.groups.sort(function (a, b) {
      return a.priority - b.priority;
    });
    this.actions.sort(function (a, b) {
      return a.priority - b.priority;
    });
  }

  toggleCollapsed() {
    this.collapsed = !this.collapsed
  }

  onFireOnChange(value:string) {
    this.rule.fireOn = value
    this.change.emit(this.rule)
  }

  handleEnabledToggle(enabled:boolean) {
    console.log("RuleComponent", "handleEnabledToggle", enabled)
    this.rule.enabled = enabled
    this.change.emit(this.rule)
  }

  addAction() {
    let priority = this.actions.length ? this.actions[this.actions.length - 1].priority + 1 : 1
    this.actions.push(new ActionModel(null, new ServerSideTypeModel(), this.rule, priority))
    this.sort()
  }

  onActionChange(action:ActionModel) {
    if (action.isValid()) {
      if (!action.isPersisted()) {
        action.owningRule = this.rule
        this.actionService.add(action)
      } else {
        this.actionService.save(action)
      }
    }
  }

  onActionRemove(action:ActionModel) {
    if (action.isPersisted()) {
      this.actionService.remove(action)
    }
    this.actions = this.actions.filter((aryAction)=> {
      return aryAction.key != action.key
    })
    if (this.actions.length === 0) {
      this.addAction()
    }
  }

  addGroup() {
    let priority = this.groups.length ? this.groups[this.groups.length - 1].priority + 1 : 1
    let group = new ConditionGroupModel(null, this.rule, 'AND', priority)
    this.groups.push(group)
    this.sort()
  }

  onConditionGroupChange(group:ConditionGroupComponent) {
  }

  onConditionGroupRemove(group:ConditionGroupModel) {
    this._groupService.remove(group)
    this.groups = this.groups.filter((aryGroup)=> {
      return aryGroup.key != group.key
    })
    if (this.groups.length === 0) {
      this.addGroup()
    }
  }

  removeRule(event:any) {
    event.stopPropagation()
    let noWarn = this._user.suppressAlerts || (event.altKey && event.shiftKey)
    if (!noWarn) {
      noWarn = this.actions.length === 1 && !this.actions[0].isPersisted()
      noWarn = noWarn && this.groups.length === 1
      if(noWarn){
        let conditions = this.groups[0].conditions
        let keys = Object.keys(conditions)
        noWarn = noWarn && (keys.length === 0)
      }
    }

    if (noWarn || confirm('Are you sure you want delete this rule?')) {
      this.remove.emit(this.rule)
    }
  }
}

export {RuleComponent}