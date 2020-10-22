import { Component, EventEmitter, Input, Output, OnChanges } from '@angular/core';

import { ServerSideTypeModel } from './services/ServerSideFieldModel';
import { I18nService } from './services/system/locale/I18n';
import { Observable } from 'rxjs';
import { ConditionActionEvent, ConditionGroupActionEvent } from './rule-engine.container';
import {
    RULE_CONDITION_GROUP_UPDATE_OPERATOR,
    RULE_CONDITION_CREATE,
    ConditionGroupModel,
    ConditionModel
} from './services/Rule';
import { LoggerService } from 'dotcms-js';

@Component({
    selector: 'condition-group',
    template: `<div class="cw-rule-group">
        <div class="cw-condition-group-separator" *ngIf="groupIndex === 0">
            {{ rsrc('inputs.group.whenConditions.label') | async }}
        </div>
        <div class="cw-condition-group-separator" *ngIf="groupIndex !== 0">
            <button
                pButton
                tiny
                class="p-button-secondary p-button-sm"
                (click)="toggleGroupOperator()"
                [label]="group.operator"
            ></button>
            <span flex class="cw-header-text">
                {{ rsrc('inputs.group.whenFurtherConditions.label') | async }}
            </span>
        </div>
        <div flex layout="column" class="cw-conditions">
            <div
                layout="row"
                class="cw-condition-row"
                *ngFor="let condition of group?._conditions; trackBy: trackByFn; let i = index"
            >
                <rule-condition
                    flex
                    layout="row"
                    [condition]="condition"
                    [conditionTypes]="conditionTypes"
                    [conditionTypePlaceholder]="conditionTypePlaceholder"
                    [index]="i"
                    (deleteCondition)="deleteCondition.emit($event)"
                    (updateConditionType)="updateConditionType.emit($event)"
                    (updateConditionParameter)="updateConditionParameter.emit($event)"
                    (updateConditionOperator)="updateConditionOperator.emit($event)"
                ></rule-condition>
                <div class="cw-btn-group cw-add-btn">
                    <div class="ui basic icon buttons" *ngIf="i === group?._conditions.length - 1">
                        <button
                            pButton
                            type="button"
                            icon="pi pi-plus"
                            class="p-button-rounded p-button-success p-button-text"
                            arial-label="Add Condition"
                            (click)="onCreateCondition()"
                            [disabled]="!condition.isPersisted()"
                        ></button>
                    </div>
                </div>
            </div>
        </div>
    </div> `
})
export class ConditionGroupComponent implements OnChanges {
    private static I8N_BASE = 'api.sites.ruleengine.rules';

    @Input() group: ConditionGroupModel;
    @Input() conditionTypePlaceholder: string;

    @Input() groupIndex = 0;
    @Input() conditionTypes: { [key: string]: ServerSideTypeModel };

    @Output() deleteConditionGroup: EventEmitter<ConditionGroupModel> = new EventEmitter(false);
    @Output()
    updateConditionGroupOperator: EventEmitter<ConditionGroupActionEvent> = new EventEmitter(false);

    @Output() createCondition: EventEmitter<ConditionActionEvent> = new EventEmitter(false);
    @Output() deleteCondition: EventEmitter<ConditionActionEvent> = new EventEmitter(false);
    @Output() updateConditionType: EventEmitter<ConditionActionEvent> = new EventEmitter(false);
    @Output()
    updateConditionParameter: EventEmitter<ConditionActionEvent> = new EventEmitter(false);
    @Output() updateConditionOperator: EventEmitter<ConditionActionEvent> = new EventEmitter(false);

    private resources: I18nService;
    private _rsrcCache: { [key: string]: Observable<string> };

    constructor(resources: I18nService, private loggerService: LoggerService) {
        this.resources = resources;
        this._rsrcCache = {};
    }

    ngOnChanges(changes): void {
        if (changes.group && this.group && this.group._conditions.length === 0) {
            this.group._conditions.push(new ConditionModel({ _type: new ServerSideTypeModel() }));
        }
    }

    rsrc(subkey: string): Observable<String> {
        let x = this._rsrcCache[subkey];
        if (!x) {
            x = this.resources.get(ConditionGroupComponent.I8N_BASE + '.' + subkey);
            this._rsrcCache[subkey] = x;
        }
        return x;
    }

    onCreateCondition(): void {
        this.loggerService.info('ConditionGroupComponent', 'onCreateCondition');
        this.createCondition.emit(<ConditionActionEvent>{
            payload: {
                conditionGroup: this.group,
                index: this.groupIndex,
                type: RULE_CONDITION_CREATE
            }
        });
    }

    toggleGroupOperator(): void {
        // tslint:disable-next-line:prefer-const
        let value = this.group.operator === 'AND' ? 'OR' : 'AND';
        this.updateConditionGroupOperator.emit(<ConditionActionEvent>{
            payload: {
                conditionGroup: this.group,
                index: this.groupIndex,
                type: RULE_CONDITION_GROUP_UPDATE_OPERATOR,
                value: value
            }
        });
    }

    trackByFn(index) {
        return index;
    }
}
