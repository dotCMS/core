import {
    GOAL_OPERATORS,
    GOAL_PARAMETERS,
    GOAL_TYPES,
    Goals,
    GoalSelectOption
} from './dot-experiments.model';

export const MAX_VARIANTS_ALLOWED = 3;

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

export const ExperimentsStatusList = [
    {
        label: 'experimentspage.experiment.status.draft',
        value: DotExperimentStatusList.DRAFT
    },
    {
        label: 'experimentspage.experiment.status.running',
        value: DotExperimentStatusList.RUNNING
    },
    {
        label: 'experimentspage.experiment.status.ended',
        value: DotExperimentStatusList.ENDED
    },
    {
        label: 'experimentspage.experiment.status.archived',
        value: DotExperimentStatusList.ARCHIVED
    },
    {
        label: 'experimentspage.experiment.status.scheduled',
        value: DotExperimentStatusList.SCHEDULED
    }
];

export const ExperimentsGoalsList: Array<GoalSelectOption> = [
    {
        label: 'experiments.goal.reach_page.name',
        value: GOAL_TYPES.REACH_PAGE,
        description: 'experiments.goal.reach_page.description',
        inactive: false
    },
    {
        label: 'experiments.goal.bounce_rate.name',
        value: GOAL_TYPES.BOUNCE_RATE,
        description: 'experiments.goal.bounce_rate.description',
        inactive: false
    },
    {
        label: 'experiments.goal.click_on_element.name',
        value: GOAL_TYPES.CLICK_ON_ELEMENT,
        description: 'experiments.goal.click_on_element.description',
        inactive: true
    }
];

export enum SidebarStatus {
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
        label: 'experiments.goal.bounce_rate',
        description: 'experiments.goal.bounce_rate.description'
    },
    [GOAL_TYPES.CLICK_ON_ELEMENT]: {
        label: 'experiments.goal.click_on_element.name',
        description: 'experiments.goal.click_on_element.description'
    }
};
