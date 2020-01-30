export interface DotServiceIntegration {
    description: string;
    iconUrl: string;
    configurationsCount: number;
    name: string;
    key: string;
    sites?: DotServiceIntegrationSites[];
}

export interface DotServiceIntegrationSites {
    id: string;
    name: string;
}
