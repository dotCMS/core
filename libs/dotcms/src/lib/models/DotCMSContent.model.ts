export interface DotCMSContent {
    stName: string;
}

export interface DotCMSContentQuery {
    contentType: string;
    queryParams?: object;
    options: {
        depth?: string;
        limit?: string;
        offset?: string;
        orderBy?: string;
    };
}
