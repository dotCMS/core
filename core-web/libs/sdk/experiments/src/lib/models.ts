export type Strategy = 'js';

export interface DataAttribute {
    'api-key': string;
    server: string;
    debug: boolean;
}

interface LookBackWindow {
    expireMillis: number;
    value: string;
}

interface Regexs {
    isExperimentPage: string;
    isTargetPage: string | null;
}

interface Variant {
    name: string;
    url: string;
}

interface Experiment {
    id: string;
    lookBackWindow: LookBackWindow;
    name: string;
    pageUrl: string;
    regexs: Regexs;
    runningId: string;
    variant: Variant;
}

interface Entity {
    excludedExperimentIds: string[];
    experiments: Experiment[];
    includedExperimentIds: string[];
}

export interface IsUserIncludedApiResponse {
    entity: Entity;
    errors: never[];
    i18nMessagesMap: Record<string, unknown>;
    messages: unknown[];
    pagination: unknown | null;
    permissions: unknown[];
}

export type SdkExperimentConfig = {
    mode: Strategy;
} & DataAttribute;
