import {Component, EventEmitter} from 'angular2/core'
import {CORE_DIRECTIVES} from 'angular2/common'
import {RuleModel, RuleService, ConditionGroupModel, ConditionModel, ActionModel} from "../../../api/rule-engine/Rule"
import {CwChangeEvent} from "../../../api/util/CwEvent";
import {ServerSideFieldModel, ServerSideTypeModel} from "../../../api/rule-engine/ServerSideFieldModel";
import {RuleEngineComponent} from "./rule-engine";
import {ConditionService} from "../../../api/rule-engine/Condition";
import {ActionService} from "../../../api/rule-engine/Action";
import {ConditionGroupService} from "../../../api/rule-engine/ConditionGroup";
import {ActionTypeService} from "../../../api/rule-engine/ActionType";
import {I18nService} from "../../../api/system/locale/I18n";
import {ConditionTypeService} from "../../../api/rule-engine/ConditionType";
import {Observable} from "rxjs/Observable";

const I8N_BASE:string = 'api.sites.ruleengine'

export interface ParameterChangeEvent extends CwChangeEvent {
  rule?:RuleModel
  source?:ServerSideFieldModel
  name:string
  value:string
}


export interface TypeChangeEvent extends CwChangeEvent {
  rule?:RuleModel
  source:ServerSideFieldModel
  value:any
  index:number
}

export interface RuleActionEvent {type:string, payload:{rule?:RuleModel, value?:string|boolean}}
export interface RuleActionActionEvent extends RuleActionEvent {payload:{rule?:RuleModel, value?:string|boolean, ruleAction?: ActionModel, index?:number, name?:string}}
export interface ConditionGroupActionEvent extends RuleActionEvent {payload:{rule?:RuleModel, value?:string|boolean, conditionGroup?: ConditionGroupModel, index?:number}}
export interface ConditionActionEvent extends RuleActionEvent {
  payload:{
    rule?:RuleModel,
    value?:string|boolean,
    condition?: ConditionModel,
    conditionGroup?: ConditionGroupModel,
    index?:number,
    name?:string}
}

/**
 *
 */
@Component({
  selector: 'cw-rule-engine-container',
  directives: [CORE_DIRECTIVES, RuleEngineComponent],
  template: `
    <cw-rule-engine
      [rules]="rules"
      [ruleActionTypes]="_ruleActionTypes"
      [conditionTypes]="_conditionTypes"
      (createRule)="onCreateRule($event)"
      (deleteRule)="onDeleteRule($event)"
      (updateName)="onUpdateRuleName($event)"
      (updateFireOn)="onUpdateFireOn($event)"
      (updateEnabledState)="onUpdateEnabledState($event)"
      (updateExpandedState)="onUpdateExpandedState($event)"
      
      (createRuleAction)="onCreateRuleAction($event)"
      (updateRuleActionType)="onUpdateRuleActionType($event)"
      (updateRuleActionParameter)="onUpdateRuleActionParameter($event)"
      (deleteRuleAction)="onDeleteRuleAction($event)"
      
      (createCondition)="onCreateCondition($event)"
      (updateConditionType)="onUpdateConditionType($event)"
      (updateConditionParameter)="onUpdateConditionParameter($event)"
      (updateConditionOperator)="onUpdateConditionOperator($event)"
      (deleteCondition)="onDeleteCondition($event)"
      
    ></cw-rule-engine>
`
})
export class RuleEngineContainer {

  rules:RuleModel[] = []

  private _ruleActionTypes:{[key:string]: ServerSideTypeModel} = {}
  private _conditionTypes:{[key:string]: ServerSideTypeModel} = {}



  rules$:EventEmitter<RuleModel[]> = new EventEmitter()
  ruleActions$:EventEmitter<ActionModel[]> = new EventEmitter()
  conditionGroups$:EventEmitter<ConditionGroupModel[]> = new EventEmitter()

  constructor(private _ruleService:RuleService,
              private _ruleActionTypeService:ActionTypeService,
              private _ruleActionService:ActionService,
              private _conditionGroupService:ConditionGroupService,
              private _conditionService:ConditionService,
              private _conditionTypeService:ConditionTypeService,
              private _resources:I18nService
  ) {

    this.rules$.subscribe(( rules ) => {
      console.log("RuleEngineContainer", "rules$.subscribe", rules)
      this.rules = rules
    })
    this.preCacheCommonResources(_resources)
    this.initRules()
    this.initActionTypes()
    this.initConditionTypes()

  }

  private preCacheCommonResources(resources:I18nService) {
    resources.get('api.sites.ruleengine').subscribe((rsrc)=> {})
    resources.get('api.ruleengine.system').subscribe((rsrc)=> {})
    resources.get('api.system.ruleengine').subscribe((rsrc)=> {})
  }

  private initRules() {
    this._ruleService.loadRules().subscribe((rules:RuleModel[]) => {
      rules.sort(function (a, b) {
        return b.priority - a.priority;
      });
      this.rules$.emit(rules)
    })
  }

  private initActionTypes() {
    this._ruleActionTypeService.list().subscribe((types:ServerSideTypeModel[])=> {
      types.forEach(type => {
        type._opt = {value: type.key, label: this._resources.get(type.i18nKey + '.name', type.i18nKey)}
        this._ruleActionTypes[type.key] = type
      })
    })
  }

  private initConditionTypes() {
    this._conditionTypeService.allAsArray().subscribe((types:ServerSideTypeModel[])=> {
      types.sort(this.alphaSort('key'))
      console.log("RuleEngineContainer", "initConditionTypes", types)
      types.forEach(type => {
        type._opt = {value: type.key, label: this._resources.get(type.i18nKey + '.name', type.i18nKey)}
        this._conditionTypes[type.key] = type
      })
    })
  }

  alphaSort(key) {
    return (a, b) => {
      let x
      if(a[key] > b[key]) {
        x = 1
      } else if(a[key] < b[key]){
        x = -1
      }
      else {
            x = 0
          }
      return x
    }
  }

  /**
   *
   export const RULE_CREATE = 'RULE_CREATE'
   export const RULE_DELETE = 'RULE_DELETE'
   export const RULE_UPDATE_NAME = 'RULE_UPDATE_NAME'
   export const RULE_UPDATE_FIRE_ON = 'RULE_UPDATE_FIRE_ON'

   * @param event
   */

  onCreateRule(event) {
    console.log("RuleEngineContainer", "onCreateRule", event)
    let priority = this.rules.length ? this.rules[0].priority + 1 : 1;
    let rule = new RuleModel({ priority})
    this.rules$.emit([rule].concat(this.rules))
  }
  
  onDeleteRule(event:RuleActionEvent) {
    let rule = event.payload.rule
    if (rule.isPersisted()) {
      this._ruleService.deleteRule(rule.key)
    }
    let rules = this.rules.filter((arrayRule) => arrayRule.key !== rule.key)
    this.rules$.emit(rules)
  }

  onUpdateEnabledState(event:RuleActionEvent) {
    console.log("RuleEngineContainer", "onUpdateEnabledState", event)
    event.payload.rule.enabled = <boolean>event.payload.value
    this.patchRule(event.payload.rule)
  }

  onUpdateRuleName(event:RuleActionEvent){
    console.log("RuleEngineContainer", "onUpdateRuleName", event)
    event.payload.rule.name = <string>event.payload.value
    this.patchRule(event.payload.rule)
  }


  onUpdateFireOn(event:RuleActionEvent){
    console.log("RuleEngineContainer", "onUpdateFireOn", event)
    event.payload.rule.fireOn = <string>event.payload.value
    this.patchRule(event.payload.rule)
  }

  onUpdateExpandedState(event:RuleActionEvent) {
    /**
     * .flatMap(conditionJson => {
      return this._conditionTypeService.get(conditionJson.conditionlet).map((type:ServerSideTypeModel) => {
        return ConditionService.fromJson(group, conditionId, conditionJson, type)
      })
    })
     */
    let rule = event.payload.rule
    rule._expanded = <boolean>event.payload.value
    let obs2:Observable<ConditionGroupModel>
    if (rule._conditionGroups.length == 0) {
      let obs:Observable<ConditionGroupModel[]> = this._conditionGroupService.allAsArray(rule.key, Object.keys(rule.conditionGroups))
      obs2 = obs.flatMap((groups:ConditionGroupModel[]) => Observable.from(groups))
    } else {
      obs2 = Observable.from(rule._conditionGroups)
    }
    let obs3:Observable<ConditionGroupModel> = obs2.flatMap(
        (group:ConditionGroupModel) => this._conditionService.listForGroup(group, this._conditionTypes),
        (group:ConditionGroupModel, conditions:ConditionModel[])=> {
          if (conditions) {
            conditions.forEach((condition:ConditionModel)=> {
              condition.type = this._conditionTypes[condition.conditionlet]
            })
          }
          group._conditions = conditions
          return group
        }
    )

    let obs4:Observable<ConditionGroupModel[]> = obs3.reduce(
        (acc:ConditionGroupModel[], group:ConditionGroupModel) => {
          if (group._conditions.length == 0) {
            debugger
          }
          acc.push(group)
          return acc
        }, [])

    obs4.subscribe((groups:ConditionGroupModel[]) => {
      rule._conditionGroups = groups
      if (rule._conditionGroups.length === 0) {
        console.log("RuleEngineContainer", "conditionGroups", "Add stub group")
        let group = new ConditionGroupModel({operator: 'AND', priority: 1})
        group._conditions.push(new ConditionModel({priority: 1, _type: new ServerSideTypeModel(), operator: 'AND'}))
        rule._conditionGroups.push(group)
      } else {
        rule._conditionGroups.sort(this.prioritySortFn)
        rule._conditionGroups.forEach((group:ConditionGroupModel) => group._conditions.sort(this.prioritySortFn))
      }
    }, (e)=> {
      console.error("RuleEngineContainer", e)
    })

    if (rule._ruleActions.length == 0) {
      this._ruleActionService.allAsArray(rule.key, Object.keys(rule.ruleActions), this._ruleActionTypes).subscribe((actions) => {
        rule._ruleActions = actions
        if (rule._ruleActions.length === 0) {
          let action = new ActionModel(null, new ServerSideTypeModel(), rule, 1)
          rule._ruleActions.push(action)
          rule._ruleActions.sort(this.prioritySortFn)
        } else {
          rule._ruleActions.sort(this.prioritySortFn)
        }
      })
    }
  }

  onCreateRuleAction(event:RuleActionActionEvent) {
    console.log("RuleEngineContainer", "onCreateRuleAction", event)
    let rule = event.payload.rule
    let priority = rule._ruleActions.length ? rule._ruleActions[rule._ruleActions.length - 1].priority + 1 : 1
    let entity = new ActionModel(null, new ServerSideTypeModel(), rule, priority)
    if (entity.isValid()) {
      this._ruleActionService.add(rule.key, entity).subscribe((result)=>{
        rule._ruleActions.push(entity)
        rule._ruleActions.sort(this.prioritySortFn)
      })
    } else {
      rule._ruleActions.push(entity)
      rule._ruleActions.sort(this.prioritySortFn)
    }

  }

  onDeleteRuleAction(event:RuleActionActionEvent) {
    console.log("RuleEngineContainer", "onDeleteRuleAction", event)
    let rule = event.payload.rule
    let ruleAction = event.payload.ruleAction
    if (ruleAction.isPersisted()) {
      this._ruleActionService.remove(rule.key, ruleAction).subscribe((result)=>{
        rule._ruleActions = rule._ruleActions.filter((aryAction)=> {
          return aryAction.key != ruleAction.key
        })
        if (rule._ruleActions.length === 0) {
          //this.addAction()
        }
      })
    }

  }


  onUpdateRuleActionType(event:RuleActionActionEvent) {
    console.log("RuleEngineContainer", "onUpdateRuleActionType")
    try {
      let ruleAction = event.payload.ruleAction
      let rule = event.payload.rule
      let idx = event.payload.index
      let type:ServerSideTypeModel = this._ruleActionTypes[<string>event.payload.value]
      rule._ruleActions[idx] = new ActionModel(ruleAction.key, type, rule, ruleAction.priority)
      this.patchAction(rule, ruleAction)
    } catch (e) {
      console.error("RuleComponent", "onActionTypeChange", e)
    }
  }
  
  onUpdateRuleActionParameter(event:RuleActionActionEvent) {
    console.log("RuleEngineContainer", "onUpdateRuleActionParameter")
    let ruleAction = event.payload.ruleAction
    ruleAction.setParameter(event.payload.name, event.payload.value)
    this.patchAction(event.payload.rule, ruleAction)
  }
  
  onCreateConditionGroup(event:ConditionGroupActionEvent) {
    let rule = event.payload.rule
    let groupCount = rule._conditionGroups.length
    let priority = groupCount ? rule._conditionGroups[groupCount - 1].priority + 1 : 1
    let group = new ConditionGroupModel({operator: 'AND', priority:1})

    rule._conditionGroups.push(group)
    rule._conditionGroups.sort(this.prioritySortFn)
  }

  onUpdateConditionGroupOperator(event:ConditionGroupActionEvent) {
    console.log("RuleEngineContainer", "onUpdateConditionGroupOperator")
    let group = event.payload.conditionGroup
    group.operator = <string>event.payload.value
    this.patchConditionGroup(event.payload.rule, group)
  }

  onDeleteConditionGroup(event:ConditionGroupActionEvent) {
    let rule = event.payload.rule
    let group = event.payload.conditionGroup
    this._conditionGroupService.remove(rule.key, group)
    rule._conditionGroups = rule._conditionGroups.filter((aryGroup)=> aryGroup.key != group.key )
  }

  onCreateCondition(event:ConditionActionEvent){
    let rule = event.payload.rule
    let parent = event.payload.conditionGroup
    let priority = parent._conditions.length ? parent._conditions[parent._conditions.length - 1].priority + 1 : 1
    let entity = new ConditionModel({ priority:priority, _type:new ServerSideTypeModel(), operator: 'AND'})

    if (entity.isValid()) {
      this._conditionService.add(parent.key, entity).subscribe((result)=>{
        console.log("RuleEngineContainer", "createdCondition", result)
      })
    } else {
      parent._conditions.push(entity)
    }
    this.patchCondition(rule, parent, entity)
  }

  onUpdateConditionType(event:ConditionActionEvent) {
    console.log("RuleEngineContainer", "onUpdateConditionType")
    try {
      let condition = event.payload.condition
      let group = event.payload.conditionGroup
      let rule = event.payload.rule
      let idx = event.payload.index
      let type:ServerSideTypeModel = this._conditionTypes[<string>event.payload.value]
      group._conditions[idx] = new ConditionModel({id: condition.key, _type: type, priority:condition.priority})
      this.patchCondition(rule, group, condition)
    } catch (e) {
      console.error("RuleComponent", "onActionTypeChange", e)
    }
  }

  onUpdateConditionParameter(event:ConditionActionEvent) {
    console.log("RuleEngineContainer", "onUpdateConditionParameter")
    let condition = event.payload.condition
    condition.setParameter(event.payload.name, event.payload.value)
    this.patchCondition(event.payload.rule, event.payload.conditionGroup, condition)
  }

  onUpdateConditionOperator(event:ConditionActionEvent) {
    console.log("RuleEngineContainer", "onUpdateConditionOperator")
    let condition = event.payload.condition
    condition.operator = <string>event.payload.value
    this.patchCondition(event.payload.rule, event.payload.conditionGroup, condition)
  }

  onDeleteCondition(event:ConditionActionEvent) {
    console.log("RuleEngineContainer", "onDeleteRuleAction", event)
    let group = event.payload.conditionGroup
    let condition = event.payload.condition
    if (condition.isPersisted()) {
      this._conditionService.remove(condition).subscribe((result)=>{
        group._conditions = group._conditions.filter((aryCondition)=> {
          return aryCondition.key != condition.key
        })
        if (group._conditions.length === 0) {
          // removeConditionGroup
        }
      })
    }

  }


  patchRule(rule:RuleModel) {
    if (rule.isValid()) {
      if (rule.isPersisted()) {
        this._ruleService.updateRule(rule.key, rule)
      }
      else {
        this._ruleService.createRule(rule);
      }
    }
    // this.updateActiveRuleCount()
  }

  patchAction(rule:RuleModel, ruleAction:ActionModel) {
    if (ruleAction.isValid()) {
      if (!ruleAction.isPersisted()) {
        this._ruleActionService.add(rule.key, ruleAction).subscribe((result)=>{
        })
      } else {
        this._ruleActionService.save(rule.key, ruleAction).subscribe((result)=>{
          console.log("RuleEngineContainer", "patchAction", "ruleAction patched")
        })
      }
    }
  }

  patchConditionGroup(rule:RuleModel, conditionGroup:ConditionGroupModel) {
    if (conditionGroup.isValid()) {
      if (!conditionGroup.isPersisted()) {
        this._conditionGroupService.add(rule.key, conditionGroup).subscribe((result)=>{ })
      } else {
        this._conditionGroupService.save(rule.key, conditionGroup)
      }
    }
  }

  patchCondition(rule:RuleModel, group:ConditionGroupModel, condition:ConditionModel) {
    try {
      if (condition.isValid()) {
        if (condition.isPersisted()) {
          this._conditionService.save(group.key, condition).subscribe((result)=>{
            console.log("RuleEngineContainer", "conditionSaved", result)
          })
        } else {
          if (!group.isPersisted()) {
            this._conditionGroupService.add(rule.key, group).subscribe((foo) => {
              this._conditionService.add(group.key, condition).subscribe(() => {
                group.conditions[condition.key] = true
              })
            })
          } else {
            this._conditionService.add(group.key, condition).subscribe(() => {
              group.conditions[condition.key] = true
            })
          }
        }
      }
    } catch (e) {
      console.error(e)
    }
  }


  prioritySortFn(a:any, b:any) {
    return a.priority - b.priority;
  }

}




