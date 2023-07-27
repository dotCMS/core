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
