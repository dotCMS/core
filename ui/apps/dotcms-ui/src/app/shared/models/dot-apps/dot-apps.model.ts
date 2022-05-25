import { SelectItem } from 'primeng/api';

export enum dialogAction {
    IMPORT = 'Import',
    EXPORT = 'Export'
}

export interface DotApps {
    allowExtraParams: boolean;
    configurationsCount?: number;
    description?: string;
    iconUrl?: string;
    key: string;
    name: string;
    sites?: DotAppsSites[];
    sitesWithWarnings?: number;
}

export interface DotAppsSites {
    configured?: boolean;
    id: string;
    name: string;
    secrets?: DotAppsSecrets[];
    secretsWithWarnings?: number;
}

export interface DotAppsSecrets {
    dynamic: boolean;
    hidden: boolean;
    hint: string;
    label: string;
    name: string;
    options?: SelectItem[];
    required: boolean;
    type: string;
    value: string;
    warnings?: string[];
}

export interface DotAppsSaveData {
    [key: string]: {
        hidden: boolean;
        value: string;
    };
}

export interface DotAppsListResolverData {
    apps: DotApps[];
    isEnterpriseLicense: boolean;
}

export interface DotAppsExportConfiguration {
    appKeysBySite?: { [key: string]: string[] };
    exportAll: boolean;
    password: string;
}

export interface DotAppsImportConfiguration {
    file: File;
    json: { password: string };
}
