export interface DotCMSWorkflow {
    archived: boolean;
    creationDate: Date;
    defaultScheme: boolean;
    description: string;
    entryActionId: string;
    id: string;
    mandatory: boolean;
    modDate: Date;
    name: string;
    system: boolean;
}

export interface DotWorkflowPayload {
    assign: string;
    comments: string;
    pathToMove: string;
    environment: string[];
    expireDate: string;
    filterKey: string;
    publishDate: string;
    pushActionSelected: string;
    timezoneId: string;
}

export interface DotProcessedWorkflowPayload {
    assign: string;
    comments: string;
    expireDate?: string;
    expireTime?: string;
    filterKey: string;
    iWantTo?: string;
    publishDate?: string;
    publishTime?: string;
    whereToSend?: string;
    environment?: string[];
    pushActionSelected?: string;
    timezoneId: string;
    pathToMove: string;
    contentlet: Record<string, unknown>;
}
