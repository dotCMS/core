import { GOAL_OPERATORS, GOAL_PARAMETERS, GOAL_TYPES, Goals } from './dot-experiments.model';
import { DotDropdownSelectOption } from './shared-models';

export const MAX_VARIANTS_ALLOWED = 3;

export const DEFAULT_VARIANT_ID = 'DEFAULT';

export const DEFAULT_VARIANT_NAME = 'Original';

export const SESSION_STORAGE_VARIATION_KEY = 'variantName';

export const TIME_7_DAYS = 6048e5;

export const TIME_90_DAYS = 7776e6;

export const PROP_NOT_FOUND = 'NOT_FOUND';

export const MINIMUM_SESSIONS_TO_SHOW_CHART = 10;

export enum ExperimentsConfigProperties {
    EXPERIMENTS_MIN_DURATION = 'EXPERIMENTS_MIN_DURATION',
    EXPERIMENTS_MAX_DURATION = 'EXPERIMENTS_MAX_DURATION'
}

export enum TrafficProportionTypes {
    SPLIT_EVENLY = 'SPLIT_EVENLY',
    CUSTOM_PERCENTAGES = 'CUSTOM_PERCENTAGES'
}

export const MAX_INPUT_TITLE_LENGTH = 50;

export const MAX_INPUT_DESCRIPTIVE_LENGTH = 255;

// Keep the order of this enum is important to respect the order of the experiment listing.
export enum DotExperimentStatus {
    RUNNING = 'RUNNING',
    SCHEDULED = 'SCHEDULED',
    DRAFT = 'DRAFT',
    ENDED = 'ENDED',
    ARCHIVED = 'ARCHIVED'
}

export const ExperimentsStatusIcons: Record<DotExperimentStatus, string> = {
    [DotExperimentStatus.DRAFT]: 'pi pi-pencil',
    [DotExperimentStatus.SCHEDULED]: 'pi pi-calendar',
    [DotExperimentStatus.RUNNING]: 'pi pi-play',
    [DotExperimentStatus.ENDED]: 'pi pi-check',
    [DotExperimentStatus.ARCHIVED]: 'pi pi-inbox'
};

export const ExperimentsStatusList: Array<DotDropdownSelectOption<string>> = [
    {
        label: 'draft',
        value: DotExperimentStatus.DRAFT
    },
    {
        label: 'scheduled',
        value: DotExperimentStatus.SCHEDULED
    },
    {
        label: 'running',
        value: DotExperimentStatus.RUNNING
    },
    {
        label: 'ended',
        value: DotExperimentStatus.ENDED
    },
    {
        label: 'archived',
        value: DotExperimentStatus.ARCHIVED
    }
];

export const GoalsConditionsParametersListByType: Partial<
    Record<GOAL_TYPES, Array<DotDropdownSelectOption<GOAL_PARAMETERS>>>
> = {
    [GOAL_TYPES.URL_PARAMETER]: [
        {
            label: 'experiments.goal.conditions.params.query_param.label',
            value: GOAL_PARAMETERS.QUERY_PARAM,
            inactive: false
        }
    ],
    [GOAL_TYPES.REACH_PAGE]: [
        {
            label: 'experiments.goal.conditions.params.url.label',
            value: GOAL_PARAMETERS.URL,
            inactive: false
        }
    ]
};

type SelectOptionsOperators = Array<DotDropdownSelectOption<GOAL_OPERATORS>>;
export const GoalsConditionsOperatorsListByType: Partial<
    Record<GOAL_TYPES, SelectOptionsOperators>
> = {
    [GOAL_TYPES.URL_PARAMETER]: [
        {
            label: 'experiments.goal.conditions.operators.contains.label',
            value: GOAL_OPERATORS.CONTAINS,
            inactive: false
        },
        {
            label: 'experiments.goal.conditions.operators.equals.label',
            value: GOAL_OPERATORS.EQUALS,
            inactive: false
        },
        {
            label: 'experiments.goal.conditions.operators.exists.label',
            value: GOAL_OPERATORS.EXISTS,
            inactive: false
        }
    ],
    [GOAL_TYPES.REACH_PAGE]: [
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
    ]
};

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

export const GOALS_METADATA_MAP: Record<
    GOAL_TYPES,
    { label: string; description: string; icon: string }
> = {
    [GOAL_TYPES.REACH_PAGE]: {
        label: 'experiments.goal.reach_page.name',
        description: 'experiments.goal.reach_page.description',
        icon: 'pi-file-excel'
    },
    [GOAL_TYPES.BOUNCE_RATE]: {
        label: 'experiments.goal.bounce_rate.name',
        description: 'experiments.goal.bounce_rate.description',
        icon: 'pi-chart-pie'
    },
    [GOAL_TYPES.CLICK_ON_ELEMENT]: {
        label: 'experiments.goal.click_on_element.name',
        description: 'experiments.goal.click_on_element.description',
        icon: 'pi-check-square'
    },
    [GOAL_TYPES.URL_PARAMETER]: {
        label: 'experiments.goal.url_parameter.name',
        description: 'experiments.goal.url_parameter.description',
        icon: 'pi-paperclip'
    },
    [GOAL_TYPES.EXIT_RATE]: {
        label: 'experiments.goal.exit_rate.name',
        description: 'experiments.goal.exit_rate.description',
        icon: 'pi-sign-out'
    }
};

export const daysOfTheWeek = [
    'Sunday',
    'Monday',
    'Tuesday',
    'Wednesday',
    'Thursday',
    'Friday',
    'Saturday'
];

export const MonthsOfTheYear = [
    'months.january.short',
    'months.february.short',
    'months.march.short',
    'months.april.short',
    'months.may.short',
    'months.june.short',
    'months.july.short',
    'months.august.short',
    'months.september.short',
    'months.october.short',
    'months.november.short',
    'months.december.short'
];

export type SummaryLegend = { icon: string; legend: string };

export const enum BayesianStatusResponse {
    TIE = 'TIE',
    NONE = 'NONE'
}

export const BayesianNoWinnerStatus: Array<string> = [
    BayesianStatusResponse.NONE,
    BayesianStatusResponse.TIE
];

const enum BayesianLegendStatus {
    WINNER = 'WINNER',
    NO_WINNER_FOUND = 'NO_WINNER_FOUND',
    NO_WINNER_FOUND_YET = 'NO_WINNER_FOUND_YET',
    NO_ENOUGH_SESSIONS = 'NO_ENOUGH_SESSIONS',
    PRELIMINARY_WINNER = 'PRELIMINARY_WINNER'
}

export const ReportSummaryLegendByBayesianStatus: Record<BayesianLegendStatus, SummaryLegend> = {
    [BayesianLegendStatus.WINNER]: {
        icon: 'dot-trophy',
        legend: 'experiments.summary.suggested-winner.winner-is'
    },
    [BayesianLegendStatus.PRELIMINARY_WINNER]: {
        icon: 'dot-trophy',
        legend: 'experiments.summary.suggested-winner.preliminary-winner-is'
    },
    [BayesianLegendStatus.NO_WINNER_FOUND]: {
        icon: 'pi-ban',
        legend: 'experiments.summary.suggested-winner.no-winner-found'
    },
    [BayesianLegendStatus.NO_WINNER_FOUND_YET]: {
        icon: 'pi-ban',
        legend: 'experiments.summary.suggested-winner.no-winner-found-yet'
    },
    [BayesianLegendStatus.NO_ENOUGH_SESSIONS]: {
        icon: 'pi-ban',
        legend: 'experiments.summary.suggested-winner.no-enough-sessions'
    }
};

type DotExperimentListAction =
    | 'delete'
    | 'abort'
    | 'results'
    | 'configuration'
    | 'archive'
    | 'end'
    | 'addToBundle'
    | 'pushPublish'
    | 'cancelSchedule';
export const AllowedActionsByExperimentStatus: Record<
    DotExperimentListAction,
    Array<DotExperimentStatus>
> = {
    ['delete']: [DotExperimentStatus.DRAFT, DotExperimentStatus.SCHEDULED],
    ['abort']: [DotExperimentStatus.RUNNING],
    ['configuration']: [
        DotExperimentStatus.RUNNING,
        DotExperimentStatus.ENDED,
        DotExperimentStatus.ARCHIVED,
        DotExperimentStatus.SCHEDULED,
        DotExperimentStatus.DRAFT
    ],
    ['archive']: [DotExperimentStatus.ENDED],
    ['end']: [DotExperimentStatus.RUNNING],
    ['addToBundle']: [
        DotExperimentStatus.DRAFT,
        DotExperimentStatus.RUNNING,
        DotExperimentStatus.ENDED,
        DotExperimentStatus.ARCHIVED,
        DotExperimentStatus.SCHEDULED
    ],
    ['pushPublish']: [
        DotExperimentStatus.DRAFT,
        DotExperimentStatus.RUNNING,
        DotExperimentStatus.ENDED,
        DotExperimentStatus.ARCHIVED,
        DotExperimentStatus.SCHEDULED
    ],
    ['cancelSchedule']: [DotExperimentStatus.SCHEDULED],
    ['results']: [DotExperimentStatus.RUNNING, DotExperimentStatus.ENDED]
};

export const CONFIGURATION_CONFIRM_DIALOG_KEY = 'confirmDialog';

export enum HealthStatusTypes {
    OK = 'OK',
    NOT_CONFIGURED = 'NOT_CONFIGURED',
    CONFIGURATION_ERROR = 'CONFIGURATION_ERROR'
}

export const RUNNING_UNTIL_DATE_FORMAT = 'EEE, LLL dd';

export const EXP_CONFIG_ERROR_LABEL_CANT_EDIT = 'experiment.configure.edit.only.draft.status';

export const EXP_CONFIG_ERROR_LABEL_PAGE_BLOCKED = 'experiment.configure.edit.page.blocked';
