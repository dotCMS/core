import { SelectItem } from 'primeng/api';

export enum dialogAction {
    IMPORT = 'Import',
    EXPORT = 'Export'
}

export interface DotApp {
    allowExtraParams: boolean;
    configurationsCount?: number;
    description?: string;
    iconUrl?: string;
    key: string;
    name: string;
    sites?: DotAppsSite[];
    sitesWithWarnings?: number;
}

export interface DotAppsSite {
    configured?: boolean;
    id: string;
    name: string;
    secrets?: DotAppsSecret[];
    secretsWithWarnings?: number;
}

export interface DotAppsSecret {
    dynamic: boolean;
    hidden: boolean;
    hint: string;
    label: string;
    name: string;
    options?: SelectItem[];
    required: boolean;
    type: string;
    value: string;
    hasEnvVar: boolean;
    hasEnvVarValue: boolean;
    envShow: boolean;
    warnings?: string[];
}

export interface DotAppsSaveData {
    [key: string]: {
        hidden: boolean;
        value: string;
    };
}

export interface DotAppsListResolverData {
    apps: DotApp[];
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
