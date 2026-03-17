export type SupportedFrontEndFrameworks = 'nextjs' | 'astro' | 'angular' | 'angular-ssr';

export type FrameworkChoices = {
    name: string;
    value: SupportedFrontEndFrameworks;
};

export interface GetUserTokenRequest {
    user: string;
    password: string;
    expirationDays: string;
    label: string;
}

export interface GetUserTokenResponse {
    entity: {
        token: string;
    };
}

export interface UVEConfigRequest {
    configuration: {
        hidden: false;
        value: string;
    };
}

export interface UVEConfigResponse {
    entity: 'Ok';
}

export interface DemoSiteResponse {
    entity: {
        addThis: string;
        aliases: string;
        archived: boolean;
        default: boolean;
        description: string;
        embeddedDashboard: string | null;
        googleAnalytics: string;
        googleMap: string;
        identifier: string;
        inode: string;
        keywords: string;
        languageId: number;
        live: boolean;
        locked: boolean;
        modDate: number;
        modUser: string;
        proxyUrlForEditMode: string | null;
        runDashboard: boolean;
        siteName: string;
        siteThumbnail: string;
        systemHost: boolean;
        tagStorage: string;
        working: boolean;
    };
}

export type DotCmsCliOptions = {
    // common
    name?: string; // project name
    framework?: 'nextjs' | 'astro' | 'angular' | 'angular-ssr';
    directory?: string;

    // local / cloud
    local?: boolean;

    // cloud-only
    url?: string;
    username?: string;
    password?: string;
};

export interface FinalStepsOptions {
    projectPath: string;
    urlDotCMSInstance: string;
    devCommand: string; // e.g. 'npm run dev' or 'ng serve'
    defaultPort: number; // e.g. 3000, 4200
    envVariablesString: string;
    envDirectory?: string; // optional, defaults to projectPath
}
