export interface DotWorkflow {
    id: string;
    name: string;
    creationDate: number;
    description: string;
    archived: boolean;
    mandatory: boolean;
    defaultScheme: boolean;
    modDate: Date;
    entryActionId: string;
    system: boolean;
}
