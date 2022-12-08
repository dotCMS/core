export interface MonacoEditorOperation {
    range: number;
    text: string;
    forceMoveMarkers: boolean;
}

export interface MonacoEditor {
    getSelection: () => number;
    executeEdits: (action: string, data: MonacoEditorOperation[]) => void;
}
