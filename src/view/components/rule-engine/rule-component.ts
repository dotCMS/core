import {Component, Directive, View, ElementRef, Inject, Input, Output, EventEmitter} from 'angular2/angular2';
import {CORE_DIRECTIVES} from 'angular2/angular2';

import * as Rx from 'rxjs/Rx.KitchenSink'

import {RuleActionComponent} from './rule-action-component';
import {ConditionGroupComponent} from './rule-condition-group-component';

import {InputToggle} from '../../../view/components/input/toggle/InputToggle'
import {ApiRoot} from '../../../api/persistence/ApiRoot';

import {RuleService, RuleModel} from "../../../api/rule-engine/Rule";
import {ActionService, ActionModel} from "../../../api/rule-engine/Action";
import {CwChangeEvent} from "../../../api/util/CwEvent";
import {EntityMeta} from "../../../api/persistence/EntityBase";
import {ConditionGroupModel, ConditionGroupService} from "../../../api/rule-engine/ConditionGroup";

import {Dropdown, DropdownModel, DropdownOption} from "../semantic/modules/dropdown/dropdown";
import {InputText} from "../semantic/elements/input-text/input-text";
import {ServerSideTypeModel} from "../../../api/rule-engine/ServerSideFieldModel";

var rsrc = {
  fireOn: {
    EVERY_PAGE: 'Every Page',
    ONCE_PER_VISIT: 'Once per visit',
    ONCE_PER_VISITOR: 'Once per visitor',
    EVERY_REQUEST: 'Every Request'
  }
}

@Component({
  selector: 'rule'
})
@View({
  template: `<div flex layout="column" class="cw-rule" [class.cw-hidden]="hidden" [class.cw-disabled]="!rule.enabled">
  <div flex="grow" layout="row" layout-align="space-between-center" class="cw-header" *ngIf="!hidden" (click)="toggleCollapsed()">
    <div flex="70" layout="row" layout-align="start-center" class="cw-header-right" *ngIf="!hidden">
      <i flex="none" class="caret icon cw-rule-caret large" [class.right]="collapsed" [class.down]="!collapsed" aria-hidden="true"></i>
      <cw-input-text flex="70"
                     class="cw-rule-name-input"
                     (blur)="handleRuleNameChange($event)"
                     (focus)="collapsed = false"
                     focused="{{rule.key == null}}"
                     (click)="$event.stopPropagation()"
                     name="rule-{{rule.key}}-name"
                     placeholder="{{rsrc.inputs.name.placeholder}}"
                     [value]="rule.name">
      </cw-input-text>
      <span class="cw-fire-on-label">{{rsrc.inputs.fireOn.label}}</span>
      <cw-input-dropdown flex="none" class="cw-fire-on-dropdown" [model]="fireOnDropdown" (change)="onFireOnChange($event)" (click)="$event.stopPropagation()"></cw-input-dropdown>
    </div>
    <div flex="30" layout="row" layout-align="end-center" class="cw-header-left" *ngIf="!hidden">
      <cw-toggle-input class="cw-input"
                       [on-text]="rsrc.inputs.onOff.on.label"
                       [off-text]="rsrc.inputs.onOff.off.label"
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
  <div flex layout="column" class="cw-accordion-body" [class.cw-hidden]="collapsed">
    <condition-group flex="grow" layout="row" *ngFor="var group of groups; var i=index"
                     [rule]="rule"
                     [group]="group"
                     [group-index]="i"
                     (remove)="onConditionGroupRemove($event)"
                     (change)="onConditionGroupChange($event)"></condition-group>
    <div flex layout="column" layout-align="center-start" class="cw-action-separator">
      {{rsrc.inputs.action.firesActions}}
    </div>
    <div flex="100" layout="column" class="cw-rule-actions">
      <div flex layout="row" layout-align="space-between-center" class="cw-action-row" *ngFor="var ruleAction of actions; #i=index">
        <div flex layout="row" layout-align="start-center" layout-fill>
          <rule-action flex [action]="ruleAction" [index]="i" (change)="onActionChange($event)" (remove)="onActionRemove($event)"></rule-action>
        </div>
        <div flex="0" layout="row" layout-align="end-center">
          <div class="cw-spacer cw-add-condition" *ngIf="i !== (actions.length - 1)"></div>
          <div class="cw-btn-group" *ngIf="i === (actions.length - 1)">
            <div class="ui basic icon buttons">
              <button class="cw-button-add-item ui small basic button" arial-label="Add Action" (click)="addAction();" [disabled]="!ruleAction.isPersisted()">
                <i class="plus icon" aria-hidden="true"></i>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</div>

`,
  directives: [CORE_DIRECTIVES, InputToggle, RuleActionComponent, ConditionGroupComponent, InputText, Dropdown]
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
  rsrc:any
  private ruleService:RuleService
  private actionService:ActionService
  private _groupService:ConditionGroupService

  private fireOnDropdown:DropdownModel


  constructor(elementRef:ElementRef,
              ruleService:RuleService,
              actionService:ActionService,
              groupService:ConditionGroupService) {
    this.change = new EventEmitter()
    this.remove = new EventEmitter()
    this.rsrc = ruleService.rsrc
    this.elementRef = elementRef
    this.actionService = actionService
    this.ruleService = ruleService;
    this._groupService = groupService;

    this.groups = []
    this.actions = []

    this.hidden = false
    this.collapsed = false


    var fireOnOptions = [
      new DropdownOption('EVERY_PAGE', 'EVERY_PAGE', this.rsrc.inputs.fireOn.options.EveryPage),
      new DropdownOption('ONCE_PER_VISIT', 'ONCE_PER_VISIT', this.rsrc.inputs.fireOn.options.OncePerVisit),
      new DropdownOption('ONCE_PER_VISITOR', 'ONCE_PER_VISITOR', this.rsrc.inputs.fireOn.options.OncePerVisitor),
      new DropdownOption('EVERY_REQUEST', 'EVERY_REQUEST', this.rsrc.inputs.fireOn.options.EveryRequest),
    ]
    this.fireOnDropdown = new DropdownModel('fireOn', "Select One", ['EVERY_PAGE'], fireOnOptions)
  }

  ngOnChanges(change) {
    if (change.rule) {
      let rule:RuleModel = change.rule.currentValue
      this.rule = rule
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
          console.log("RuleComponent", "askdlfaslkdfjlj", actions)
          this.actions = actions || []
          if (this.actions.length === 0) {
            this.addAction()
          } else {
            this.sort()
          }
        })
        this.fireOnDropdown.selected = [rule.fireOn]
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

  handleRuleNameChange(name:string) {
    console.log("handleRuleNameChange")
    this.rule.name = name
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
    if ((event.altKey && event.shiftKey) || confirm('Are you sure you want delete this rule?')) {
      this.remove.emit(this.rule)
    }
  }
}

export {RuleComponent}