export interface DotApps {
    configurationsCount?: number;
    description?: string;
    iconUrl?: string;
    key: string;
    name: string;
    sites?: DotAppsSites[];
}

export interface DotAppsSites {
    configured?: boolean;
    id: string;
    name: string;
    secrets?: DotAppsSecrets[];
}

export interface DotAppsSecrets {
    dynamic: boolean;
    hidden: boolean;
    hint: string;
    label: string;
    name: string;
    required: boolean;
    type: string;
    value: string;
}

export interface DotAppsSaveData {
    [key: string]: {
        hidden: string;
        value: string;
    };
}
