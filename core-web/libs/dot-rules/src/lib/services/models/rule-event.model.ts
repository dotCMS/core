import { ChangeEvent } from './event.model';

import {
    ActionModel,
    ConditionGroupModel,
    ConditionModel,
    RuleModel
} from '../api/rule/Rule';
import { ServerSideFieldModel, ServerSideTypeModel } from '../api/serverside-field/ServerSideFieldModel';


/**
 * Event emitted when a parameter value changes
 */
export interface ParameterChangeEvent extends ChangeEvent {
    rule?: RuleModel;
    source?: ServerSideFieldModel;
    name: string;
    value: string;
}

/**
 * Event emitted when a type selection changes
 */
export interface TypeChangeEvent extends ChangeEvent {
    rule?: RuleModel;
    source: ServerSideFieldModel;
    value: ServerSideTypeModel | string;
    index: number;
}

/**
 * Base event interface for rule-related actions
 */
export interface RuleActionEvent {
    type: string;
    payload: {
        rule?: RuleModel;
        value?: string | boolean;
    };
}

/**
 * Event emitted for rule action operations (add, update, delete)
 */
export interface RuleActionActionEvent extends RuleActionEvent {
    payload: {
        rule?: RuleModel;
        value?: string | boolean;
        ruleAction?: ActionModel;
        index?: number;
        name?: string;
    };
}

/**
 * Event emitted for condition group operations
 */
export interface ConditionGroupActionEvent extends RuleActionEvent {
    payload: {
        rule?: RuleModel;
        value?: string | boolean;
        conditionGroup?: ConditionGroupModel;
        index?: number;
        priority?: number;
    };
}

/**
 * Event emitted for condition operations
 */
export interface ConditionActionEvent extends RuleActionEvent {
    payload: {
        rule?: RuleModel;
        value?: string | boolean;
        condition?: ConditionModel;
        conditionGroup?: ConditionGroupModel;
        index?: number;
        name?: string;
        type?: string;
    };
}
