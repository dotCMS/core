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
