export interface DotCMSWorkflow {
    archived: boolean;
    creationDate: Date;
    defaultScheme: boolean;
    description: string;
    entryActionId: string | null;
    id: string;
    mandatory: boolean;
    modDate: Date;
    name: string;
    system: boolean;
    variableName?: string;
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
    contentlet?: Record<string, unknown>;
}

export interface DotCMSWorkflowStatus {
    scheme: DotCMSWorkflow;
    step: WorkflowStep;
    task: WorkflowTask;
    firstStep?: WorkflowStep;
}

export interface WorkflowStep {
    creationDate: number;
    enableEscalation: boolean;
    escalationAction: string | null;
    escalationTime: number;
    id: string;
    myOrder: number;
    name: string;
    resolved: boolean;
    schemeId: string;
}

export interface WorkflowTask {
    assignedTo: string;
    belongsTo: string | null;
    createdBy: string;
    creationDate: number;
    description: string;
    dueDate: string | null;
    id: string;
    inode: string;
    languageId: number;
    modDate: number;
    new: boolean;
    status: string;
    title: string;
    webasset: string;
}
