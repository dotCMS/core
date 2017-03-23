import {Component, EventEmitter, ViewEncapsulation} from '@angular/core'
import {
    RuleModel, RuleService, ConditionGroupModel, ConditionModel, ActionModel,
    RuleEngineState, RULE_CONDITION_CREATE
} from "../../../api/rule-engine/Rule"
import {CwChangeEvent} from "../../../api/util/CwEvent";
import {ServerSideFieldModel, ServerSideTypeModel} from "../../../api/rule-engine/ServerSideFieldModel";
import {RuleEngineComponent} from "./rule-engine";
import {ConditionService} from "../../../api/rule-engine/Condition";
import {ActionService} from "../../../api/rule-engine/Action";
import {ConditionGroupService} from "../../../api/rule-engine/ConditionGroup";
import {I18nService} from "../../../api/system/locale/I18n";
import {Observable} from "rxjs/Observable";
import {CwError} from "../../../api/system/http-response-util";
import {BundleService, IPublishEnvironment} from "../../../api/services/bundle-service";
import {SiteChangeListener} from "../../../api/util/site-change-listener";
import {SiteService} from "../../../api/services/site-service";
import {ActivatedRoute} from '@angular/router';

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
export interface ConditionGroupActionEvent extends RuleActionEvent {payload:{rule?:RuleModel, value?:string|boolean, conditionGroup?: ConditionGroupModel, index?:number, priority?:number}}
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
  encapsulation: ViewEncapsulation.None,

  selector: 'cw-rule-engine-container',
  styleUrls: ['styles/rule-engine.css', 'styles/angular-material.layouts.css', 'styles/semantic.ui.css'],
  template: `
    <cw-rule-engine
      [environmentStores]="environments"
      [rules]="rules"
      [ruleActionTypes]="_ruleService._ruleActionTypes"
      [conditionTypes]="_ruleService._conditionTypes"
      [loading]="state.loading"
      [showRules]="state.showRules"
      [globalError]="state.globalError"
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

      (createConditionGroup)="onCreateConditionGroup($event)"
      (updateConditionGroupOperator)="onUpdateConditionGroupOperator($event)"
      (createCondition)="onCreateCondition($event)"
      (updateConditionType)="onUpdateConditionType($event)"
      (updateConditionParameter)="onUpdateConditionParameter($event)"
      (updateConditionOperator)="onUpdateConditionOperator($event)"
      (deleteCondition)="onDeleteCondition($event)"
    ></cw-rule-engine>
`
})
export class RuleEngineContainer {

  rules: RuleModel[];
  state:RuleEngineState = new RuleEngineState()

  environments:IPublishEnvironment[] = []

  rules$:EventEmitter<RuleModel[]> = new EventEmitter()
  ruleActions$:EventEmitter<ActionModel[]> = new EventEmitter()
  conditionGroups$:EventEmitter<ConditionGroupModel[]> = new EventEmitter()
  globalError:string;

  constructor(private _ruleService:RuleService,
              private _ruleActionService:ActionService,
              private _conditionGroupService:ConditionGroupService,
              private _conditionService:ConditionService,
              private _resources:I18nService,
              public bundleService:BundleService,
              private route: ActivatedRoute
  ) {
    this.rules$.subscribe(( rules ) => {
      this.rules = rules;
    })

    this.bundleService.loadPublishEnvironments().subscribe((environments) => this.environments = environments);
    this.initRules();

  }

  private preCacheCommonResources(resources:I18nService) {
    resources.get('api.sites.ruleengine').subscribe((rsrc)=> {})
    resources.get('api.ruleengine.system').subscribe((rsrc)=> {})
    resources.get('api.system.ruleengine').subscribe((rsrc)=> {})
  }

  private initRules(): void {
    this.state.loading = true;
    
    let siteId = '';
    this.route.queryParams.subscribe(params => {
      siteId = params['realmId'];
    });
    this._ruleService.requestRules(siteId).subscribe( (rules: RuleModel[]) => {
      console.log('requestRules success', rules);
      this.loadRules(rules);

      this._ruleService.loadRules().subscribe((rules: RuleModel[]) => {
        this.loadRules(rules);
      });
    });
  }

  private loadRules(rules: RuleModel[]): void {
    rules.sort(function (a, b) {
      return b.priority - a.priority;
    });
    this.rules$.emit(rules);
    this.state.loading = false;
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
    let group = new ConditionGroupModel({priority:1, operator:'AND'})
    group._conditions.push(new ConditionModel({_type: new ServerSideTypeModel()}))
    rule._conditionGroups.push(group)
    let action = new ActionModel(null, new ServerSideTypeModel())
    action._owningRule = rule
    rule._ruleActions.push(action)
    rule._saved = false
    rule._expanded = true
    this.rules$.emit([rule].concat(this.rules))
  }
  
  onDeleteRule(event:RuleActionEvent) {
    //console.log("RuleEngineContainer", "onDeleteRule")
    let rule = event.payload.rule
    rule._deleting = true
    this.state.deleting = true
    if (rule.isPersisted()) {
      this._ruleService.deleteRule(rule.key).subscribe(( result ) => {
        this.state.deleting = false
      })
    }
    let rules = this.rules.filter((arrayRule) => arrayRule.key !== rule.key)
    this.rules$.emit(rules)
  }

  onUpdateEnabledState(event:RuleActionEvent) {
    //console.log("RuleEngineContainer", "onUpdateEnabledState", event)
    event.payload.rule.enabled = <boolean>event.payload.value
    this.patchRule(event.payload.rule, false)
  }

  onUpdateRuleName(event:RuleActionEvent){
    //console.log("RuleEngineContainer", "onUpdateRuleName", event)
    event.payload.rule.name = <string>event.payload.value
    this.patchRule(event.payload.rule, false)
  }


  onUpdateFireOn(event:RuleActionEvent){
    console.log("RuleEngineContainer", "onUpdateFireOn", event)
    event.payload.rule.fireOn = <string>event.payload.value
    this.patchRule(event.payload.rule, false)
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
    if (rule._expanded) {
      let obs2:Observable<ConditionGroupModel>
      if (rule._conditionGroups.length == 0) {
        let obs:Observable<ConditionGroupModel[]> = this._conditionGroupService.allAsArray(rule.key, Object.keys(rule.conditionGroups))
        obs2 = obs.flatMap((groups:ConditionGroupModel[]) => Observable.from(groups))
      } else {
        obs2 = Observable.from(rule._conditionGroups)
      }
      let obs3:Observable<ConditionGroupModel> = obs2.flatMap(
          (group:ConditionGroupModel) => this._conditionService.listForGroup(group, this._ruleService._conditionTypes),
          (group:ConditionGroupModel, conditions:ConditionModel[])=> {
            if (conditions) {
              conditions.forEach((condition:ConditionModel)=> {
                condition.type = this._ruleService._conditionTypes[condition.conditionlet]
              })
            }
            group._conditions = conditions
            return group
          }
      )

      let obs4:Observable<ConditionGroupModel[]> = obs3.reduce(
          (acc:ConditionGroupModel[], group:ConditionGroupModel) => {
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
          rule._conditionGroups.forEach((group:ConditionGroupModel) => {
            group._conditions.sort(this.prioritySortFn)
            if (group._conditions.length === 0){
              group._conditions.push(new ConditionModel({priority: 1, _type: new ServerSideTypeModel(), operator: 'AND'}))
            }
          })
        }
      }, (e)=> {
        console.error("RuleEngineContainer", e)
      })

      if (rule._ruleActions.length == 0) {
        this._ruleActionService.allAsArray(rule.key, Object.keys(rule.ruleActions), this._ruleService._ruleActionTypes).subscribe((actions) => {
          rule._ruleActions = actions
          if (rule._ruleActions.length === 0) {
            let action = new ActionModel(null, new ServerSideTypeModel(), 1)
            rule._ruleActions.push(action)
            rule._ruleActions.sort(this.prioritySortFn)
          } else {
            rule._ruleActions.sort(this.prioritySortFn)
          }
        })
      }
    }
  }

  onCreateRuleAction(event:RuleActionActionEvent) {
    console.log("RuleEngineContainer", "onCreateRuleAction", event)
    let rule = event.payload.rule
    let priority = rule._ruleActions.length ? rule._ruleActions[rule._ruleActions.length - 1].priority + 1 : 1
    let entity = new ActionModel(null, new ServerSideTypeModel(), priority)

    this.patchRule(rule, true)
    rule._ruleActions.push(entity)
    rule._saved = false
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
          rule._ruleActions.push(new ActionModel(null, new ServerSideTypeModel(), 1))
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
      let type:ServerSideTypeModel = this._ruleService._ruleActionTypes[<string>event.payload.value]
      rule._ruleActions[idx] = new ActionModel(ruleAction.key, type, ruleAction.priority)
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
    console.log("RuleEngineContainer", "onCreateConditionGroup")
    let rule = event.payload.rule
    let priority = rule._conditionGroups.length ? rule._conditionGroups[rule._conditionGroups.length - 1].priority + 1 : 1
    let group = new ConditionGroupModel({operator: 'AND', priority:priority})
    group._conditions.push(new ConditionModel({priority: 1, _type: new ServerSideTypeModel(), operator: 'AND'}))
    rule._conditionGroups.push(group)
    rule._conditionGroups.sort(this.prioritySortFn)
  }

  onUpdateConditionGroupOperator(event:ConditionGroupActionEvent) {
    console.log("RuleEngineContainer", "onUpdateConditionGroupOperator")
    let group = event.payload.conditionGroup
    group.operator = <string>event.payload.value
    if(group.key != null) {
      this.patchConditionGroup(event.payload.rule, group)
      this.patchRule(event.payload.rule)
    }
  }

  onDeleteConditionGroup(event:ConditionGroupActionEvent) {
    let rule = event.payload.rule
    let group = event.payload.conditionGroup
    this._conditionGroupService.remove(rule.key, group).subscribe()
    rule._conditionGroups = rule._conditionGroups.filter((aryGroup)=> aryGroup.key != group.key )
  }

  onCreateCondition(event:ConditionActionEvent){

    let rule = event.payload.rule
    this.ruleUpdating(rule, true)
    try {
      let group = event.payload.conditionGroup
      let priority = group._conditions.length ? group._conditions[group._conditions.length - 1].priority + 1 : 1
      let entity = new ConditionModel({priority: priority, _type: new ServerSideTypeModel(), operator: 'AND'})
      group._conditions.push(entity)
      this.ruleUpdated(rule)
    } catch(e){
      console.error("RuleEngineContainer", "onCreateCondition", e)
      this.ruleUpdated(rule, [{unhandledError: e}])
    }

  }

  onUpdateConditionType(event:ConditionActionEvent) {
    console.log("RuleEngineContainer", "onUpdateConditionType")
    try {
      let condition = event.payload.condition
      let group = event.payload.conditionGroup
      let rule = event.payload.rule
      let idx = event.payload.index
      let type:ServerSideTypeModel = this._ruleService._conditionTypes[<string>event.payload.value]
      // replace the condition rather than mutate it to force event for 'onPush' NG2 components.
      condition = new ConditionModel({id: condition.key, _type: type, priority:condition.priority, operator:condition.operator})
      group._conditions[idx] = condition
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
    console.log("RuleEngineContainer", "onDeleteCondition", event)
    let rule = event.payload.rule
    let group = event.payload.conditionGroup
    let condition = event.payload.condition
    if (condition.isPersisted()) {
      this._conditionService.remove(condition).subscribe((result)=>{
        group._conditions = group._conditions.filter((aryCondition)=> {
          return aryCondition.key != condition.key
        })
        if (group._conditions.length === 0) {
          console.log("RuleEngineContainer", "condition", "Remove Condition and remove Groups is empty")
          this._conditionGroupService.remove(rule.key, group).subscribe()
          rule._conditionGroups = rule._conditionGroups.filter((aryGroup)=> aryGroup.key != group.key )
        }
        if (rule._conditionGroups.length === 0) {
          console.log("RuleEngineContainer", "conditionGroups", "Add stub group if Groups are empty")
          let group = new ConditionGroupModel({operator: 'AND', priority: 1})
          group._conditions.push(new ConditionModel({priority: 1, _type: new ServerSideTypeModel(), operator: 'AND'}))
          rule._conditionGroups.push(group)
        }
      })
    }
  }

  ruleUpdating(rule, disable:boolean = true){
    if(disable && rule.enabled && rule.key){
      console.log("RuleEngineContainer", "ruleUpdating", "disabling rule due for edit.")
      this.patchRule(rule, true)
    }
    rule._saved = false
    rule._saving = true
    rule._errors = null
  }

  ruleUpdated(rule:RuleModel, errors?:{[key:string]: any}){
    rule._saving = false
    if (!errors) {
      rule._saved = true
    }
    else {
      console.error(errors)
      rule._errors = errors
    }
  }
  
  patchConditionGroup(rule:RuleModel, group:ConditionGroupModel, disable:boolean=true){
    this.ruleUpdating(rule, false)
    if(disable && rule.enabled){
      rule.enabled = false
    }
    this._conditionGroupService.updateConditionGroup(rule.key, group).subscribe(( result ) => {
      
    })
  }

  patchRule(rule:RuleModel, disable:boolean=true) {
    this.ruleUpdating(rule, false)
    if(disable && rule.enabled){
      rule.enabled = false
    }
    if (rule.isValid()) {
      if (rule.isPersisted()) {
        this._ruleService.updateRule(rule.key, rule).subscribe(() => {
          this.ruleUpdated(rule)
        }, (e:CwError)=>{
          let ruleError = this._handle403Error(e) ? null : {invalid: e.message}
          this.ruleUpdated(rule, ruleError)
        })
      }
      else {
        this._ruleService.createRule(rule).subscribe(() => {
          this.ruleUpdated(rule)
        }, (e:CwError)=>{
          let ruleError = this._handle403Error(e) ? null : {invalid: e.message}
          this.ruleUpdated(rule, ruleError)
        })
      }
    } else{
      this.ruleUpdated(rule, {
        invalid: "Cannot save, rule is not valid."
      })
    }
    // this.updateActiveRuleCount()
  }

  patchAction(rule:RuleModel, ruleAction:ActionModel) {
    this.ruleUpdating(rule)
    if (ruleAction.isValid()) {
      if (!ruleAction.isPersisted()) {
        this._ruleActionService.createRuleAction(rule.key, ruleAction).subscribe((result)=>{
          this.ruleUpdated(rule)
        }, (e:CwError)=>{
          let ruleError = this._handle403Error(e) ? null : {invalid: e.message}
          this.ruleUpdated(rule, ruleError)
        })
      } else {
        this._ruleActionService.updateRuleAction(rule.key, ruleAction).subscribe((result)=>{
          this.ruleUpdated(rule)
        }, (e:any)=>{
          let ruleError = this._handle403Error(e) ? null : {invalid: e.message}
          this.ruleUpdated(rule, ruleError)
        })
      }
    }else{
      this.ruleUpdated(rule, {
        invalid: "Cannot save, action is not valid."
      })
    }
  }

  patchCondition(rule:RuleModel, group:ConditionGroupModel, condition:ConditionModel) {
    this.ruleUpdating(rule)
    try {
      if (condition.isValid()) {
        if (condition.isPersisted()) {
          this._conditionService.save(group.key, condition).subscribe((result)=>{
            this.ruleUpdated(rule)
          }, (e:any)=>{
            let ruleError = this._handle403Error(e) ? null : {invalid: e.message}
            this.ruleUpdated(rule, ruleError)
          })
        } else {
          if (!group.isPersisted()) {
            this._conditionGroupService.createConditionGroup(rule.key, group).subscribe((foo) => {
              this._conditionService.add(group.key, condition).subscribe(() => {
                group.conditions[condition.key] = true
                this.ruleUpdated(rule)
              }, (e:CwError)=>{
                let ruleError = this._handle403Error(e) ? null : {invalid: e.message}
                this.ruleUpdated(rule, ruleError)
              })
            })
          } else {
            this._conditionService.add(group.key, condition).subscribe(() => {
              group.conditions[condition.key] = true
              this.ruleUpdated(rule)
            }, (e:CwError)=>{
              let ruleError = this._handle403Error(e) ? null : {invalid: e.message}
              this.ruleUpdated(rule, ruleError)
            })
          }
        }
      } else {
        console.log("RuleEngineContainer", "patchCondition", "Not valid")
        rule._saving = false
        rule._errors = { invalid: "Condition not valid." }
      }
    } catch (e) {
      console.error(e)
      this.ruleUpdated(rule, {invalid: e.message})
    }
  }


  prioritySortFn(a:any, b:any) {
    return a.priority - b.priority;
  }
}




