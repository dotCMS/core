import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Observable } from 'rxjs';
import { RuleModel, RULE_CREATE } from './services/Rule';
import { I18nService } from './services/system/locale/I18n';
import { CwFilter } from './services/util/CwFilter';
import { ServerSideTypeModel } from './services/ServerSideFieldModel';
import {
    ConditionActionEvent,
    RuleActionActionEvent,
    RuleActionEvent,
    ConditionGroupActionEvent
} from './rule-engine.container';
import { IPublishEnvironment } from './services/bundle-service';
import { RuleViewService } from './services/dot-view-rule-service';

const I8N_BASE = 'api.sites.ruleengine';

/**
 *
 */
@Component({
    selector: 'cw-rule-engine',
    template: `
  <div class="cw-modal-glasspane"  [class.cw-loading]="loading" *ngIf="loading"></div>
  <div *ngIf="!loading && globalError" class="ui negative message cw-message">
    <div class="header">{{globalError}}</div>
    <p>Please contact an administrator</p>

    <i class="material-icons" class="close-button" (click)="globalError = ''">X</i>
  </div>
<div class="cw-rule-engine" *ngIf="!loading && showRules">
  <div class="cw-header">
    <div flex layout="row" >
      <input pInputText placeholder="{{rsrc('inputs.filter.placeholder') | async}}"
        [value]="filterText" (keyup)="filterText = $event.target.value"/>
      <div flex="2"></div>
      <button class="dot-icon-button" (click)="addRule()" >
          <i class="material-icons">add</i>
      </button>
    </div>
    <div class="cw-filter-links">
      <span>{{rsrc('inputs.filter.status.show.label') | async}}:</span>
      <a href="javascript:void(0)" class="cw-filter-link" [class.active]="!isFilteringField('enabled')"
        (click)="setFieldFilter('enabled',null)">{{rsrc('inputs.filter.status.all.label') | async}}</a>
      <span>&#124;</span>
      <a href="javascript:void(0)" class="cw-filter-link" [class.active]="isFilteringField('enabled',true)"
        (click)="setFieldFilter('enabled',true)">{{rsrc('inputs.filter.status.active.label') | async}}</a>
      <span>&#124;</span>
      <a href="javascript:void(0)" class="cw-filter-link" [class.active]="isFilteringField('enabled',false)"
        (click)="setFieldFilter('enabled',false)">{{rsrc('inputs.filter.status.inactive.label') | async}}</a>
    </div>
  </div>
  <div class="cw-rule-engine__empty" *ngIf="!rules.length">
      <i class="material-icons">tune</i>
      <h2>{{rsrc('inputs.no.rules') | async }} {{rsrc((pageId && !isContentletHost) ? 'inputs.on.page' : 'inputs.on.site') | async}}{{rsrc('inputs.add.one.now') | async}}</h2>
      <span *ngIf="pageId && !isContentletHost">{{rsrc('inputs.page.rules.fired.every.time') | async}}</span>
      <button pButton label="{{rsrc('inputs.addRule.label') | async}}" (click)="addRule()" icon="fa fa-plus"></button>
  </div>
  <rule *ngFor="let rule of rules" [rule]="rule" [hidden]="isFiltered(rule) == true"
        [environmentStores]="environmentStores"
        [ruleActions]="rule._ruleActions"
        [ruleActionTypes]="ruleActionTypes"
        [conditionTypes]="conditionTypes"
        [saved]="rule._saved"
        [saving]="rule._saving"
        [errors]="rule._errors"
        (updateName)="updateName.emit($event)"
        (updateFireOn)="updateFireOn.emit($event)"
        (updateEnabledState)="updateEnabledState.emit($event)"
        (updateExpandedState)="updateExpandedState.emit($event)"

        (createRuleAction)="createRuleAction.emit($event)"
        (updateRuleActionType)="updateRuleActionType.emit($event)"
        (updateRuleActionParameter)="updateRuleActionParameter.emit($event)"
        (deleteRuleAction)="deleteRuleAction.emit($event)"

        (createCondition)="createCondition.emit($event)"
        (createConditionGroup)="createConditionGroup.emit($event)"
        (updateConditionGroupOperator)="updateConditionGroupOperator.emit($event)"
        (updateConditionType)="updateConditionType.emit($event)"
        (updateConditionParameter)="updateConditionParameter.emit($event)"
        (updateConditionOperator)="updateConditionOperator.emit($event)"
        (deleteCondition)="deleteCondition.emit($event)"

        (deleteRule)="deleteRule.emit($event)"></rule>
</div>

`
})
export class RuleEngineComponent {
    @Input() rules: RuleModel[];
    @Input() ruleActionTypes: { [key: string]: ServerSideTypeModel } = {};
    @Input() loading: boolean;
    @Input() showRules: boolean;
    @Input() pageId: string;
    @Input() isContentletHost: boolean;
    @Input() conditionTypes: { [key: string]: ServerSideTypeModel } = {};
    @Input() environmentStores: IPublishEnvironment[];

    @Output() createRule: EventEmitter<{ type: string }> = new EventEmitter(false);
    @Output() deleteRule: EventEmitter<RuleActionEvent> = new EventEmitter(false);
    @Output() updateName: EventEmitter<RuleActionEvent> = new EventEmitter(false);
    @Output() updateExpandedState: EventEmitter<RuleActionEvent> = new EventEmitter(false);
    @Output() updateEnabledState: EventEmitter<RuleActionEvent> = new EventEmitter(false);
    @Output() updateFireOn: EventEmitter<RuleActionEvent> = new EventEmitter(false);

    @Output() createRuleAction: EventEmitter<RuleActionActionEvent> = new EventEmitter(false);
    @Output() deleteRuleAction: EventEmitter<RuleActionActionEvent> = new EventEmitter(false);
    @Output() updateRuleActionType: EventEmitter<RuleActionActionEvent> = new EventEmitter(false);
    @Output()
    updateRuleActionParameter: EventEmitter<RuleActionActionEvent> = new EventEmitter(false);

    @Output()
    createConditionGroup: EventEmitter<ConditionGroupActionEvent> = new EventEmitter(false);
    @Output()
    updateConditionGroupOperator: EventEmitter<ConditionGroupActionEvent> = new EventEmitter(false);

    @Output() createCondition: EventEmitter<ConditionActionEvent> = new EventEmitter(false);
    @Output() deleteCondition: EventEmitter<ConditionActionEvent> = new EventEmitter(false);
    @Output() updateConditionType: EventEmitter<ConditionActionEvent> = new EventEmitter(false);
    @Output()
    updateConditionParameter: EventEmitter<ConditionActionEvent> = new EventEmitter(false);
    @Output() updateConditionOperator: EventEmitter<ConditionActionEvent> = new EventEmitter(false);

    globalError: string;

    filterText: string;
    status: string;
    activeRules: number;

    private resources: I18nService;
    private _rsrcCache: { [key: string]: Observable<string> };

    constructor(resources: I18nService, private ruleViewService: RuleViewService) {
        this.resources = resources;
        resources.get(I8N_BASE).subscribe(rsrc => {});
        this.filterText = '';
        this.rules = [];
        this._rsrcCache = {};
        this.status = null;

        this.ruleViewService.message.subscribe((message: string) => {
            this.globalError = message;
        });
    }

    rsrc(subkey: string): Observable<any> {
        let x = this._rsrcCache[subkey];
        if (!x) {
            x = this.resources.get(I8N_BASE + '.rules.' + subkey);
            this._rsrcCache[subkey] = x;
        }
        return x;
    }

    ngOnChange(change): void {
        if (change.rules) {
            this.updateActiveRuleCount();
        }
    }

    addRule(): void {
        this.createRule.emit({ type: RULE_CREATE });
    }

    updateActiveRuleCount(): void {
        this.activeRules = 0;
        for (let i = 0; i < this.rules.length; i++) {
            if (this.rules[i].enabled) {
                this.activeRules++;
            }
        }
    }

    setFieldFilter(field: string, value: boolean = null): void {
        // remove old status
        const re = new RegExp(field + ':[\\w]*');
        this.filterText = this.filterText.replace(re, ''); // whitespace issues: "blah:foo enabled:false mahRule"
        if (value !== null) {
            this.filterText = field + ':' + value + ' ' + this.filterText;
        }
    }

    isFilteringField(field: string, value: any = null): boolean {
        let isFiltering;
        if (value === null) {
            const re = new RegExp(field + ':[\\w]*');
            isFiltering = this.filterText.match(re) != null;
        } else {
            isFiltering = this.filterText.indexOf(field + ':' + value) >= 0;
        }
        return isFiltering;
    }

    isFiltered(rule: RuleModel): boolean {
        return CwFilter.isFiltered(rule, this.filterText);
    }
}

