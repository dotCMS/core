import { ActionModel, ConditionGroupModel, ConditionModel, RuleModel } from '../api/rule/Rule';

/**
 * These events bubble in two stages.
 *
 * An inner component (`DotRuleActionComponent`, `DotRuleConditionComponent`,
 * `DotConditionGroupComponent`) emits only the entity it owns. `DotRuleComponent` catches that
 * event and re-emits it with the `rule` — and, where the inner component cannot know it, the
 * `conditionGroup` — attached. Only then does `DotRuleEngineContainerComponent` see it.
 *
 * A single interface used to describe both stages, which is why every field was optional: it
 * fitted neither end. The container dereferenced 74 possibly-undefined fields, and the inner
 * emitters needed `as ConditionActionEvent` casts to compile. The `*EmitEvent` types below
 * describe stage one; the rest describe what the container actually receives, with each field
 * required exactly where a handler reads it.
 */

// ---------------------------------------------------------------------------
// Stage one — emitted by the component that owns the entity
// ---------------------------------------------------------------------------

/** A rule action changed. `DotRuleComponent` adds the `rule`. */
export interface RuleActionEmitEvent {
    type: string;
    payload: {
        ruleAction: ActionModel;
        index: number;
        name?: string;
        value?: string;
    };
}

/** A condition changed. `DotRuleComponent` adds the `rule` and the owning `conditionGroup`. */
export interface ConditionEmitEvent {
    type: string;
    payload: {
        condition: ConditionModel;
        index: number;
        name?: string;
        value?: string;
    };
}

/** A condition was deleted. No index: the container filters the group's conditions by key. */
export interface ConditionDeleteEmitEvent {
    type: string;
    payload: {
        condition: ConditionModel;
    };
}

/** A condition group changed, or wants a new condition. `DotRuleComponent` adds the `rule`. */
export interface ConditionGroupEmitEvent {
    type: string;
    payload: {
        conditionGroup: ConditionGroupModel;
        index: number;
        value?: string;
    };
}

// ---------------------------------------------------------------------------
// Stage two — what the container receives
// ---------------------------------------------------------------------------

/**
 * The rule itself changed: deleted, renamed, expanded, enabled, or its fire-on set. Also the
 * "add a rule action" event, which carries nothing but the rule that will own the new action.
 *
 * `value` stays optional because deletion has no value, and the handlers that do read it cast
 * to `boolean` or `string` — the payload cannot say which without splitting this five ways.
 */
export interface RuleActionEvent {
    type: string;
    payload: {
        rule: RuleModel;
        value?: string | boolean;
    };
}

/** An existing rule action was deleted, retyped, or had a parameter set. */
export interface RuleActionActionEvent {
    type: string;
    payload: {
        rule: RuleModel;
        ruleAction: ActionModel;
        index: number;
        name?: string;
        value?: string | boolean;
    };
}

/**
 * A new condition group is wanted. There is no group yet, so `priority` is what the emitter can
 * supply — though the container recomputes it from the rule's existing groups and ignores this.
 */
export interface ConditionGroupCreateEvent {
    type: string;
    payload: {
        rule: RuleModel;
        priority: number;
    };
}

/** An existing condition group changed — currently only its AND/OR operator. */
export interface ConditionGroupActionEvent {
    type: string;
    payload: {
        rule: RuleModel;
        conditionGroup: ConditionGroupModel;
        index: number;
        value?: string | boolean;
    };
}

/** A new condition is wanted inside an existing group. */
export interface ConditionCreateEvent {
    type: string;
    payload: {
        rule: RuleModel;
        conditionGroup: ConditionGroupModel;
        index: number;
    };
}

/** An existing condition was deleted. Carries no index: the container filters by key. */
export interface ConditionDeleteEvent {
    type: string;
    payload: {
        rule: RuleModel;
        conditionGroup: ConditionGroupModel;
        condition: ConditionModel;
    };
}

/**
 * An existing condition was retyped, or had its operator or a parameter set. `index` is
 * required: `onUpdateConditionType` writes back through `group._conditions[index]`.
 */
export interface ConditionActionEvent {
    type: string;
    payload: {
        rule: RuleModel;
        conditionGroup: ConditionGroupModel;
        condition: ConditionModel;
        index: number;
        name?: string;
        value?: string | boolean;
    };
}
