import {bootstrap, Provider, NgFor, NgIf, Component, Directive, View, Inject, Injector, NgClass} from 'angular2/angular2';

//import * as Rx from '../../../../node_modules/angular2/node_modules/@reactivex/rxjs/src/Rx.KitchenSink'


import {ApiRoot} from '../../../api/persistence/ApiRoot';
import {EntityMeta, EntitySnapshot} from '../../../api/persistence/EntityBase';
import {UserModel} from "../../../api/auth/UserModel";
import {I18NCountryProvider} from '../../../api/system/locale/I18NCountryProvider'


import {RuleComponent} from './rule-component';
import {RuleService, RuleModel} from "../../../api/rule-engine/Rule";
import {ActionService} from "../../../api/rule-engine/Action";
import {CwChangeEvent} from "../../../api/util/CwEvent";
import {RestDataStore} from "../../../api/persistence/RestDataStore";
import {DataStore} from "../../../api/persistence/DataStore";
import {ConditionGroupService} from "../../../api/rule-engine/ConditionGroup";
import {ConditionTypeService} from "../../../api/rule-engine/ConditionType";
import {ConditionService} from "../../../api/rule-engine/Condition";
import {I18nService} from "../../../api/system/locale/I18n";
import {ActionTypeService} from "../../../api/rule-engine/ActionType";
import {CwFilter} from "../../../api/util/CwFilter"

  /**
   *
   */
  @Component({
    selector: 'cw-rule-engine'
  })
  @View({
    template: `<div flex layout="column" class="cw-rule-engine">
  <div flex layout="column" layout-align="start start" class="cw-header">
  <div flex layout="row" layout-align="space-between center">
    <div flex layout="row" layout-align="space-between center" class="ui icon input">
      <i class="filter icon"></i>
      <input type="text" placeholder="{{rsrc.inputs.filter.placeholder}}" [value]="filterTextField" (keyup)="filterText = $event.target.value">
    </div>
    <div flex="2"></div>
    <button class="ui button cw-button-add" aria-label="Create a new rule" (click)="addRule()" [disabled]="ruleStub != null">
      <i class="plus icon" aria-hidden="true"></i>{{rsrc.inputs.addRule.label}}
    </button>
  </div>
    <div class="cw-filter-links">
      <span>Show:</span>
      <a href="javascript:void(0)" [ng-class]="{'active': !isFilteringField('enabled'),'cw-filter-link': true}" (click)="setFieldFilter('enabled',null)">{{rsrc.inputs.filter.status?.all}}</a>
      <span>&#124;</span>
      <a href="javascript:void(0)" [ng-class]="{'active': isFilteringField('enabled',true),'cw-filter-link': true}" (click)="setFieldFilter('enabled',true)">{{rsrc.inputs.filter.status?.active}}</a>
      <span>&#124;</span>
      <a href="javascript:void(0)" [ng-class]="{'active': isFilteringField('enabled',false),'cw-filter-link': true}" (click)="setFieldFilter('enabled',false)">{{rsrc.inputs.filter.status?.inactive}}</a>
    </div>
  </div>

  <rule flex layout="row" *ng-for="var r of rules" [rule]="r" [hidden]="isFiltered(r)"></rule>
</div>

`,
    directives: [RuleComponent, NgFor, NgIf, NgClass]
  })
  export class RuleEngineComponent {
    rules:RuleModel[];
    filterText:string
    filterTextField:string
    status:string
    activeRules:number
    rsrc:any
    private ruleService:RuleService;
    private ruleStub:RuleModel
    private stubWatch:Rx.Subscription<RuleModel>

    constructor(@Inject(ActionTypeService) actionTypeService:ActionTypeService,
                @Inject(ConditionTypeService) conditionTypeService:ConditionTypeService,
                @Inject(RuleService) ruleService:RuleService) {
      actionTypeService.list() // load types early in a single place rather than calling list repeatedly.
      conditionTypeService.list() // load types early in a single place rather than calling list repeatedly.
      this.rsrc = ruleService.rsrc
      ruleService.onResourceUpdate.subscribe((messages)=> {
        this.rsrc = messages
      })
      this.ruleService = ruleService;
      this.filterText ='';
      this.filterTextField ='';
      this.rules = []
      this.status = null
      this.ruleService.onAdd.subscribe(
          (rule:RuleModel) => {
            this.handleAdd(rule)
          },
          (err) => {
            this.handleAddError(err)
          })
      this.ruleService.onRemove.subscribe(
          (rule:RuleModel) => {
            this.handleRemove(rule)
          },
          (err) => {
            this.handleRemoveError(err)
          })
      this.ruleService.list()
      this.getFilteredRulesStatus()
    }

    handleAdd(rule:RuleModel) {
      if (rule === this.ruleStub) {
        this.stubWatch.unsubscribe()
        this.stubWatch = null
        this.ruleStub = null
      } else {
        this.rules.push(rule)
        this.rules.sort(function (a, b) {
          return b.priority - a.priority;
        });
      }

      rule.onChange.subscribe((event:CwChangeEvent<RuleModel>) => {
        this.handleRuleChange(event)
      })
      this.getFilteredRulesStatus()
    }

    handleRuleChange(event:CwChangeEvent<RuleModel>) {
      if (event.target.valid) {
        this.ruleService.save(event.target)
      }
      this.getFilteredRulesStatus()
    }

    handleRemove(rule:RuleModel) {
      this.rules = this.rules.filter((arrayRule) => {
        return arrayRule.key !== rule.key
      })
      this.getFilteredRulesStatus()
      // @todo ggranum: we're leaking Subscribers here, sadly. Might cause issues for long running edit sessions.
    }

    handleAddError(err) {
      console.log('Could not add rule: ', err.message, err);
    }

    handleRemoveError(err) {
      console.log('Something went wrong: ' + err.message);

    }

    addRule() {
      this.ruleStub = new RuleModel()
      this.ruleStub.priority = this.rules.length ? this.rules[0].priority + 1 : 1;
      this.rules.unshift(this.ruleStub)
      this.stubWatch = this.ruleStub.onChange.subscribe((event) => {
        if (event.target.valid) {
          this.ruleService.save(this.ruleStub)
        }
      })
      this.setFieldFilter('enabled')
    }

    getFilteredRulesStatus() {
    	this.activeRules = 0;
    	for (var i = 0;  i < this.rules.length ; i++){
    		if (this.rules[i].enabled){
    			this.activeRules++;
    		}
    	}
    }

    setFieldFilter(field:string, value:string=null){
      // remove old status
        var re = new RegExp(field + ':[\\w]*')
        this.filterTextField = this.filterText;
        this.filterTextField = this.filterTextField.replace(re, '' ) // whitespace issues: "blah:foo enabled:false mahRule"
      if(value != null) {
        this.filterTextField = field + ':' + value + ' ' + this.filterTextField.trim()
        this.filterText = this.filterTextField
      }else{
        this.filterText = this.filterTextField
      }
    }

    isFilteringField(field:string, value:any=null):boolean{
      let isFiltering
      if(value === null){
        var re = new RegExp(field + ':[\\w]*')
        isFiltering =  this.filterText.match(re) != null
      } else{
        isFiltering = this.filterText.indexOf(field + ':' + value) >= 0
      }
      return isFiltering
    }

    isFiltered(rule:RuleModel){
        return CwFilter.isFiltered(rule,this.filterText)
    }
  }


export class RuleEngineApp {
  static main() {

    let app = bootstrap(RuleEngineComponent, [ApiRoot,
      I18nService,
      ActionTypeService,
      UserModel,
      I18NCountryProvider,
      RuleService,
      ActionService,
      ConditionGroupService,
      ConditionService,
      ConditionTypeService,
      new Provider(DataStore, {useClass: RestDataStore})
    ])


    app.then((appRef) => {
      console.log("Bootstrapped App: ", appRef)
    }).catch((e) => {
      console.log("Error bootstrapping app: ", e)
      throw e;
    });
    return app
  }
}

