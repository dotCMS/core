export interface DotApps {
    description: string;
    iconUrl: string;
    configurationsCount: number;
    name: string;
    key: string;
    sites?: DotAppsSites[];
}

export interface DotAppsSites {
    configured: boolean;
    id: string;
    name: string;
}
