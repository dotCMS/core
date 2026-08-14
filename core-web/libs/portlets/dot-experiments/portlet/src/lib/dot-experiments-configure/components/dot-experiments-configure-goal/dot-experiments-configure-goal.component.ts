import { injectDispatch } from '@ngrx/signals/events';

import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { FormField, disabled, form, maxLength, required } from '@angular/forms/signals';

import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { TagModule } from 'primeng/tag';

import { DotMessageService } from '@dotcms/data-access';
import {
    DotDropdownSelectOption,
    GOAL_OPERATORS,
    GOAL_PARAMETERS,
    GOAL_TYPES,
    Goal,
    Goals,
    GOALS_METADATA_MAP,
    MAX_INPUT_DESCRIPTIVE_LENGTH,
    ReachPageGoalCondition,
    UrlParameterGoalCondition
} from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import {
    CONFIGURE_GOAL_TYPES,
    GOAL_TYPES_WITH_CONDITIONS,
    GoalsConditionsOperatorsListByType,
    GoalsConditionsParametersListByType
} from '../../../shared/constants';
import { GoalConditionFormModel } from '../../../shared/models';
import { dotExperimentsConfigurePageEvents } from '../../../store/dot-experiments-configure-page.events';
import { DotExperimentsConfigureStore } from '../../../store/dot-experiments-configure.store';

/**
 * The goal being edited, flattened.
 *
 * Both goal types that carry conditions carry exactly one, so the whole card fits in a single
 * signal-forms slice: no nested list, and no shape that differs per goal type. The two persisted
 * shapes are rebuilt on the way out — see {@link DotExperimentsConfigureGoalComponent}.
 */
interface GoalFormModel extends GoalConditionFormModel {
    /** Empty until a goal type is picked, which is what the SET/REQUIRED chip reads. */
    type: GOAL_TYPES | '';
    name: string;
}

const EMPTY_GOAL: GoalFormModel = {
    type: '',
    name: '',
    parameter: '',
    operator: '',
    parameterName: '',
    value: ''
};

/**
 * Condition parameter each goal type is persisted with. The user never picks it for
 * URL_PARAMETER — the parameter *name* is what they type — and REACH_PAGE only has one, so both
 * are fixed here rather than derived from a selection.
 *
 * `queryParameter` is what the backend validates for URL_PARAMETER, which is not the
 * `GOAL_PARAMETERS.QUERY_PARAM` spelling the (unused) parameter list carries.
 */
const CONDITION_PARAMETER_BY_TYPE: Partial<Record<GOAL_TYPES, string>> = {
    [GOAL_TYPES.REACH_PAGE]: GOAL_PARAMETERS.URL,
    [GOAL_TYPES.URL_PARAMETER]: 'queryParameter'
};

/** Operator each condition panel opens on, so the panel is usable without touching the select. */
const DEFAULT_OPERATOR_BY_TYPE: Partial<Record<GOAL_TYPES, GOAL_OPERATORS>> = {
    [GOAL_TYPES.REACH_PAGE]: GOAL_OPERATORS.CONTAINS,
    [GOAL_TYPES.URL_PARAMETER]: GOAL_OPERATORS.EQUALS
};

/** i18n keys of the name each goal type is proposed with, same copy as the old screen. */
const DEFAULT_NAME_KEY_BY_TYPE: Record<GOAL_TYPES, string> = {
    [GOAL_TYPES.BOUNCE_RATE]: 'experiments.goal.conditions.minimize.bounce.rate',
    [GOAL_TYPES.EXIT_RATE]: 'experiments.goal.conditions.detect.exit.rate',
    [GOAL_TYPES.REACH_PAGE]: 'experiments.goal.conditions.maximize.reach.page',
    [GOAL_TYPES.URL_PARAMETER]: 'experiments.goal.conditions.detect.queryparam.in.url',
    [GOAL_TYPES.CLICK_ON_ELEMENT]: 'experiments.goal.click_on_element.name'
};

/** One selectable goal type, with its copy already resolved. */
interface GoalTypeOption {
    value: GOAL_TYPES;
    labelKey: string;
    descriptionKey: string;
}

const GOAL_TYPE_OPTIONS: GoalTypeOption[] = CONFIGURE_GOAL_TYPES.map((type) => ({
    value: type,
    labelKey: GOALS_METADATA_MAP[type].label,
    descriptionKey: GOALS_METADATA_MAP[type].description
}));

/** Two goals are the same when every field the form holds matches. */
function isSameGoal(a: GoalFormModel, b: GoalFormModel): boolean {
    return (
        a.type === b.type &&
        a.name === b.name &&
        a.parameter === b.parameter &&
        a.operator === b.operator &&
        a.parameterName === b.parameterName &&
        a.value === b.value
    );
}

/**
 * Goal card of the Configure screen: which conversion event the variants are compared on.
 *
 * An experiment has exactly one goal, and only REACH_PAGE and URL_PARAMETER carry a condition —
 * one each — so the card holds a single flat form slice rather than a list of conditions.
 * BOUNCE_RATE and EXIT_RATE are complete as soon as they are picked, and render no panel.
 *
 * The goal is only dispatched once it is complete: a half-typed condition would otherwise be
 * PATCHed on every keystroke and rejected. Until then it lives in this card, which is exactly
 * what the Start validation reports as missing.
 *
 * Errors are never shown before Start/Schedule is pressed (AC28): each message is gated on the
 * store publishing the matching validation rule, and hides again once the field is filled in.
 */
@Component({
    selector: 'dot-experiments-configure-goal',
    imports: [FormField, InputTextModule, SelectModule, TagModule, DotMessagePipe],
    templateUrl: './dot-experiments-configure-goal.component.html'
})
export class DotExperimentsConfigureGoalComponent {
    readonly #store = inject(DotExperimentsConfigureStore);
    readonly #dispatch = injectDispatch(dotExperimentsConfigurePageEvents);
    readonly #dotMessageService = inject(DotMessageService);

    protected readonly goalTypes = GOAL_TYPES;
    protected readonly goalTypeOptions = GOAL_TYPE_OPTIONS;
    protected readonly maxNameLength = MAX_INPUT_DESCRIPTIVE_LENGTH;
    protected readonly $isLocked = this.#store.$isLocked;

    protected readonly $model = signal<GoalFormModel>(EMPTY_GOAL);

    protected readonly formTree = form(this.$model, (f) => {
        required(f.name);
        maxLength(f.name, this.maxNameLength);
        disabled(f.name, { when: () => this.#store.$isLocked() });
        disabled(f.operator, { when: () => this.#store.$isLocked() });
        disabled(f.parameter, { when: () => this.#store.$isLocked() });
        disabled(f.parameterName, { when: () => this.#store.$isLocked() });
        disabled(f.value, { when: () => this.#store.$isLocked() });
    });

    /** Which condition panel to render, if any. */
    protected readonly $goalType = computed<GOAL_TYPES | ''>(() => this.$model().type);

    /** Whether the picked type has a condition sub-panel at all — the rest are complete as picked. */
    protected readonly $showConditions = computed<boolean>(() => {
        const type = this.$goalType();

        return !!type && GOAL_TYPES_WITH_CONDITIONS.includes(type);
    });

    /** EXISTS only asks whether the parameter is there, so no value is offered for it (AC18). */
    protected readonly $showConditionValue = computed<boolean>(
        () => this.$model().operator !== GOAL_OPERATORS.EXISTS
    );

    /** Parameter options of the REACH_PAGE panel — only what the backend validates. */
    protected readonly reachPageParameterOptions = this.#translateOptions(
        GoalsConditionsParametersListByType[GOAL_TYPES.REACH_PAGE]
    );

    protected readonly $operatorOptions = computed<Array<DotDropdownSelectOption<string>>>(() => {
        const type = this.$goalType();

        return type ? this.#translateOptions(GoalsConditionsOperatorsListByType[type]) : [];
    });

    /** Reads as SET once a goal type is picked, and as REQUIRED while none is. */
    protected readonly $isGoalSet = computed<boolean>(() => !!this.$model().type);

    protected readonly $showGoalTypeError = computed<boolean>(
        () => this.#hasValidationError('goalType') && !this.$model().type
    );

    protected readonly $showGoalNameError = computed<boolean>(
        () => this.#hasValidationError('goalName') && !this.$model().name.trim()
    );

    protected readonly $showConditionValueError = computed<boolean>(
        () =>
            this.#hasValidationError('goalConditionValue') &&
            this.$showConditionValue() &&
            !this.$model().value.trim()
    );

    protected readonly $showParameterNameError = computed<boolean>(
        () => this.#hasValidationError('goalParameterName') && !this.$model().parameterName.trim()
    );

    /** Identifier of the experiment whose goal is already in the form. */
    readonly #hydratedExperimentId = signal<string | null>(null);

    /**
     * Fills the form from the experiment's goal once per experiment.
     *
     * Keyed on the identifier, not on the goal: every autosave response replaces `experiment`, and
     * re-reading it would undo whatever is being typed while that PATCH is in flight.
     */
    protected readonly hydrateEffect = effect(() => {
        const experiment = this.#store.experiment();

        if (!experiment || experiment.id === untracked(this.#hydratedExperimentId)) {
            return;
        }

        untracked(() => {
            this.#hydratedExperimentId.set(experiment.id);
            this.$model.set(this.#toFormModel(experiment.goals));
        });
    });

    /**
     * Publishes the goal once it is complete. The store owns the debounce (AC6), so nothing is
     * timed here; an incomplete goal is simply not sent, since the endpoint would reject it and
     * the Start validation already reports it as missing.
     */
    protected readonly dispatchGoalEffect = effect(() => {
        const model = this.$model();

        untracked(() => {
            if (!this.#isComplete(model)) {
                return;
            }

            const stored = this.#toFormModel(this.#store.experiment()?.goals);

            if (isSameGoal(stored, model)) {
                return;
            }

            this.#dispatch.goalChanged(this.#toGoals(model));
        });
    });

    /**
     * Picks a goal type, proposing its default name and starting its condition from scratch: the
     * condition of the type being left behind means nothing to the one being picked.
     */
    protected selectGoalType(type: GOAL_TYPES): void {
        if (this.$isLocked() || this.$model().type === type) {
            return;
        }

        this.$model.set({
            type,
            name: this.#nameFor(type, this.$model().name),
            parameter: CONDITION_PARAMETER_BY_TYPE[type] ?? '',
            operator: DEFAULT_OPERATOR_BY_TYPE[type] ?? '',
            parameterName: '',
            value: ''
        });
    }

    #hasValidationError(
        rule: 'goalType' | 'goalName' | 'goalConditionValue' | 'goalParameterName'
    ) {
        return this.#store.validationErrors().includes(rule);
    }

    /**
     * The default name of the picked type, unless the user has written one of their own — a name
     * they never touched is still one of the defaults, and following the type is what they expect.
     */
    #nameFor(type: GOAL_TYPES, currentName: string): string {
        const isUntouched =
            !currentName.trim() ||
            Object.values(DEFAULT_NAME_KEY_BY_TYPE).some(
                (key) => this.#dotMessageService.get(key) === currentName
            );

        return isUntouched
            ? this.#dotMessageService.get(DEFAULT_NAME_KEY_BY_TYPE[type])
            : currentName;
    }

    /** A goal is only worth sending when the server would accept it. */
    #isComplete(model: GoalFormModel): boolean {
        if (!model.type || !model.name.trim()) {
            return false;
        }

        if (model.type === GOAL_TYPES.REACH_PAGE) {
            return !!model.operator && !!model.value.trim();
        }

        if (model.type === GOAL_TYPES.URL_PARAMETER) {
            return (
                !!model.operator &&
                !!model.parameterName.trim() &&
                (model.operator === GOAL_OPERATORS.EXISTS || !!model.value.trim())
            );
        }

        return true;
    }

    /** Rebuilds the persisted shape: one condition for the two types that have one, none for the rest. */
    #toGoals(model: GoalFormModel): Goals {
        const primary: Goal = {
            name: model.name.trim(),
            type: model.type as GOAL_TYPES,
            conditions: this.#toConditions(model)
        };

        return { primary };
    }

    #toConditions(model: GoalFormModel): Array<ReachPageGoalCondition | UrlParameterGoalCondition> {
        const operator = model.operator as GOAL_OPERATORS;

        if (model.type === GOAL_TYPES.REACH_PAGE) {
            return [{ parameter: model.parameter, operator, value: model.value.trim() }];
        }

        if (model.type === GOAL_TYPES.URL_PARAMETER) {
            return [
                {
                    // `queryParameter` is outside the `GOAL_PARAMETERS` enum, which the persisted
                    // shape is typed against — the backend validates this spelling, not the enum's.
                    parameter: model.parameter as GOAL_PARAMETERS,
                    operator,
                    value: {
                        name: model.parameterName.trim(),
                        value: operator === GOAL_OPERATORS.EXISTS ? '' : model.value.trim()
                    }
                }
            ];
        }

        return [];
    }

    /** The inverse of {@link #toGoals}, used both to hydrate the form and to detect no-op edits. */
    #toFormModel(goals: Goals | null | undefined): GoalFormModel {
        const primary = goals?.primary;

        if (!primary?.type) {
            return EMPTY_GOAL;
        }

        const condition = primary.conditions?.[0];
        const isUrlParameter = primary.type === GOAL_TYPES.URL_PARAMETER;
        const urlParameterValue = isUrlParameter
            ? (condition?.value as UrlParameterGoalCondition['value'] | undefined)
            : undefined;

        return {
            type: primary.type,
            name: primary.name ?? '',
            parameter: condition?.parameter ?? CONDITION_PARAMETER_BY_TYPE[primary.type] ?? '',
            operator: condition?.operator ?? '',
            parameterName: urlParameterValue?.name ?? '',
            value: isUrlParameter
                ? (urlParameterValue?.value ?? '')
                : ((condition?.value as string | undefined) ?? '')
        };
    }

    /** Option labels arrive as i18n keys, and `p-select` renders whatever it is given. */
    #translateOptions(
        options: Array<DotDropdownSelectOption<string>> | undefined
    ): Array<DotDropdownSelectOption<string>> {
        return (options ?? []).map((option) => ({
            ...option,
            label: this.#dotMessageService.get(option.label)
        }));
    }
}
