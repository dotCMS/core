import { DotCMSResponse } from './dot-request-response.model';
import { DotMenu } from './navigation';

export interface DotUIColors {
    primary: string;
    secondary: string;
    background: string;
}

export interface DotLogos {
    loginScreen: string;
    navBar: string;
}

export interface DotReleaseInfo {
    buildDate: string;
    version: string;
}

export interface DotSystemTimezone {
    id: string;
    label: string;
    offset: number;
}

export interface DotSystemLanguage {
    country: string;
    countryCode: string;
    id: number;
    isoCode: string;
    language: string;
    languageCode: string;
}

export interface DotSystemLicense {
    displayServerId: string;
    isCommunity: boolean;
    level: number;
    levelName: string;
}

export interface DotCluster {
    clusterId: string;
    companyKeyDigest: string;
}

/**
 * System configuration interface containing essential server configuration data.
 * This represents a subset of the full server configuration focused on UI and system info.
 */
export interface DotSystemConfig {
    logos: DotLogos;
    colors: DotUIColors;
    releaseInfo: DotReleaseInfo;
    systemTimezone: DotSystemTimezone;
    languages: DotSystemLanguage[];
    license: DotSystemLicense;
    cluster: DotCluster;
}

/**
 * Configuration entity structure from /api/v1/appconfiguration
 */
export interface SystemConfigEntity {
    config: {
        logos: DotLogos;
        colors: DotUIColors;
        releaseInfo: DotReleaseInfo;
        systemTimezone: DotSystemTimezone;
        languages: DotSystemLanguage[];
        license: DotSystemLicense;
        cluster: DotCluster;
        // Other config properties we don't need for the system config
        [key: string]: unknown;
    };
    menu?: DotMenu; // Menu data from the endpoint
}

/**
 * Server response interface for the configuration endpoint.
 * Contains the full response structure from /api/v1/appconfiguration
 */
export type SystemConfigResponse = DotCMSResponse<SystemConfigEntity>;
