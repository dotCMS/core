import { Component, computed, inject, input } from '@angular/core';
import { disabled, FieldTree, FormField, maxLength, SchemaFn } from '@angular/forms/signals';

import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { TagModule } from 'primeng/tag';

import { DotMessageService } from '@dotcms/data-access';
import {
    DotDropdownSelectOption,
    GOAL_OPERATORS,
    GOAL_TYPES,
    GOALS_METADATA_MAP,
    MAX_INPUT_DESCRIPTIVE_LENGTH
} from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import {
    CONFIGURE_GOAL_TYPES,
    GOAL_TYPES_WITH_CONDITIONS,
    GoalsConditionsOperatorsListByType,
    GoalsConditionsParametersListByType
} from '../../../shared/constants';
import { GoalFormSlice } from '../../../shared/models';
import { DotExperimentsConfigureStore } from '../../../store/dot-experiments-configure.store';
import { CONDITION_PARAMETER_BY_TYPE } from '../../../util/dot-experiments-configure-form.util';

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

/**
 * Live constraints of the Goal slice, applied to the root form by the Configure shell.
 *
 * Declared here rather than in the shell so the rules sit with the card that renders the fields
 * they are about; the shell only says where they go — `apply(path.goal, goalFormSchema(…))`.
 *
 * One `disabled` rule covers the whole slice: a disabled field disables everything under it, so
 * every condition control follows without being named.
 *
 * The goal's name being required is *not* a rule here (AC28): `required` reaches the DOM as the
 * native attribute, which would paint an untouched field red. The store checks it when
 * Start/Schedule is pressed, and the card reveals it only then.
 *
 * @param isLocked - Whether the experiment is no longer a draft (AC34)
 */
export function goalFormSchema(isLocked: () => boolean): SchemaFn<GoalFormSlice> {
    return (goal) => {
        maxLength(goal.name, MAX_INPUT_DESCRIPTIVE_LENGTH);
        disabled(goal, { when: isLocked });
    };
}

/**
 * Goal card of the Configure screen: which conversion event the variants are compared on.
 *
 * An experiment has exactly one goal, and only REACH_PAGE and URL_PARAMETER carry a condition —
 * one each — so the whole card is a single flat slice of the shell's root form rather than a list
 * of conditions. BOUNCE_RATE and EXIT_RATE are complete as soon as they are picked, and render no
 * panel.
 *
 * The card never persists anything itself: it edits the slice it was handed, and the shell's one
 * autosave decides when the goal is complete enough to send (a half-typed condition would be
 * PATCHed on every keystroke and rejected). Until then the goal lives in the form, which is exactly
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
    /** The goal slice of the root form: the card's whole editable surface. */
    readonly $field = input.required<FieldTree<GoalFormSlice>>({ alias: 'field' });

    readonly #store = inject(DotExperimentsConfigureStore);
    readonly #dotMessageService = inject(DotMessageService);

    protected readonly goalTypes = GOAL_TYPES;
    protected readonly goalTypeOptions = GOAL_TYPE_OPTIONS;

    /** The goal as edited, read from the slice the shell owns. */
    protected readonly $goal = computed<GoalFormSlice>(() => this.$field()().value());

    /** Read off the field: the schema disables the slice, so the card need not ask the store. */
    protected readonly $isLocked = computed<boolean>(() => this.$field()().disabled());

    /** Which condition panel to render, if any. */
    protected readonly $goalType = computed<GOAL_TYPES | ''>(() => this.$goal().type);

    /** Whether the picked type has a condition sub-panel at all — the rest are complete as picked. */
    protected readonly $showConditions = computed<boolean>(() => {
        const type = this.$goalType();

        return !!type && GOAL_TYPES_WITH_CONDITIONS.includes(type);
    });

    /** EXISTS only asks whether the parameter is there, so no value is offered for it (AC18). */
    protected readonly $showConditionValue = computed<boolean>(
        () => this.$goal().operator !== GOAL_OPERATORS.EXISTS
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
    protected readonly $isGoalSet = computed<boolean>(() => !!this.$goal().type);

    protected readonly $showGoalTypeError = computed<boolean>(
        () => this.#hasValidationError('goalType') && !this.$goal().type
    );

    protected readonly $showGoalNameError = computed<boolean>(
        () => this.#hasValidationError('goalName') && !this.$goal().name.trim()
    );

    protected readonly $showConditionValueError = computed<boolean>(
        () =>
            this.#hasValidationError('goalConditionValue') &&
            this.$showConditionValue() &&
            !this.$goal().value.trim()
    );

    protected readonly $showParameterNameError = computed<boolean>(
        () => this.#hasValidationError('goalParameterName') && !this.$goal().parameterName.trim()
    );

    /**
     * Picks a goal type, proposing its default name and starting its condition from scratch: the
     * condition of the type being left behind means nothing to the one being picked.
     *
     * The whole slice is written at once — a type, a name and a condition are one choice, and
     * writing them field by field would report each intermediate state as an edit.
     */
    protected selectGoalType(type: GOAL_TYPES): void {
        const goal = this.$goal();

        if (this.$isLocked() || goal.type === type) {
            return;
        }

        this.$field()().value.set({
            type,
            name: this.#nameFor(type, goal.name),
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
