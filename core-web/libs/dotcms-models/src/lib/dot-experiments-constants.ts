import { GOAL_OPERATORS, GOAL_PARAMETERS, GOAL_TYPES, Goals } from './dot-experiments.model';
import { DotDropdownSelectOption } from './shared-models';

export const MAX_VARIANTS_ALLOWED = 2;

export const DEFAULT_VARIANT_ID = 'DEFAULT';

export const DEFAULT_VARIANT_NAME = 'Original';

export const SESSION_STORAGE_VARIATION_KEY = 'variantName';

export enum TrafficProportionTypes {
    SPLIT_EVENLY = 'SPLIT_EVENLY',
    CUSTOM_PERCENTAGES = 'CUSTOM_PERCENTAGES'
}

export enum DotExperimentStatusList {
    RUNNING = 'RUNNING',
    SCHEDULED = 'SCHEDULED',
    ENDED = 'ENDED',
    DRAFT = 'DRAFT',
    ARCHIVED = 'ARCHIVED'
}

export const ExperimentsStatusList: Array<DotDropdownSelectOption<string>> = [
    {
        label: 'draft',
        value: DotExperimentStatusList.DRAFT
    },
    {
        label: 'running',
        value: DotExperimentStatusList.RUNNING
    },
    {
        label: 'ended',
        value: DotExperimentStatusList.ENDED
    },
    {
        label: 'archived',
        value: DotExperimentStatusList.ARCHIVED
    },
    {
        label: 'scheduled',
        value: DotExperimentStatusList.SCHEDULED
    }
];

export const GoalsConditionsParametersList: Array<DotDropdownSelectOption<GOAL_PARAMETERS>> = [
    {
        label: 'experiments.goal.conditions.params.url.label',
        value: GOAL_PARAMETERS.URL,
        inactive: false
    }
];

export const GoalsConditionsOperatorsList: Array<DotDropdownSelectOption<GOAL_OPERATORS>> = [
    {
        label: 'experiments.goal.conditions.operators.contains.label',
        value: GOAL_OPERATORS.CONTAINS,
        inactive: false
    },
    {
        label: 'experiments.goal.conditions.operators.equals.label',
        value: GOAL_OPERATORS.EQUALS,
        inactive: false
    }
];

export enum SIDEBAR_STATUS {
    OPEN = 'OPEN',
    CLOSE = 'CLOSED'
}

export const DefaultGoalConfiguration: Goals = {
    primary: {
        name: 'default',
        type: GOAL_TYPES.REACH_PAGE,
        conditions: [
            {
                parameter: GOAL_PARAMETERS.URL,
                operator: GOAL_OPERATORS.EQUALS,
                value: 'to-define'
            }
        ]
    }
};

export const GOALS_METADATA_MAP: Record<GOAL_TYPES, { label: string; description: string }> = {
    [GOAL_TYPES.REACH_PAGE]: {
        label: 'experiments.goal.reach_page.name',
        description: 'experiments.goal.reach_page.description'
    },
    [GOAL_TYPES.BOUNCE_RATE]: {
        label: 'experiments.goal.bounce_rate.name',
        description: 'experiments.goal.bounce_rate.description'
    },
    [GOAL_TYPES.CLICK_ON_ELEMENT]: {
        label: 'experiments.goal.click_on_element.name',
        description: 'experiments.goal.click_on_element.description'
    }
};
