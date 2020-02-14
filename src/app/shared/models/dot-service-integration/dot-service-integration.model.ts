export interface DotServiceIntegration {
    description: string;
    iconUrl: string;
    configurationsCount: number;
    name: string;
    key: string;
    sites?: DotServiceIntegrationSites[];
}

export interface DotServiceIntegrationSites {
    configured: boolean;
    id: string;
    name: string;
}
