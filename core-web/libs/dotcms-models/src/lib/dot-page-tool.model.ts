export interface DotPageTool {
    icon: string;
    title: string;
    description: string;
    tags: string[];
    runnableLink: string;
}

export interface DotPageTools {
    pageTools: DotPageTool[];
}

export interface DotPageToolUrlParams {
    currentUrl: string;
    requestHostName: string;
    siteId: string;
    languageId: number;
}
