export interface DotPageTool {
    icon: string;
    title: string;
    description: string;
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
