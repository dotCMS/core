export interface DotTag {
    id: string;
    label: string;
    siteId: string;
    siteName: string;
    persona: boolean;
}

export interface DotTagsPaginatedResponse {
    entity: DotTag[];
    pagination: {
        currentPage: number;
        perPage: number;
        totalEntries: number;
    };
}
