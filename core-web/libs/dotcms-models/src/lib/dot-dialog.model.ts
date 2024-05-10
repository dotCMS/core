export interface DialogButton {
    action?: (dialog?: unknown) => void;
    disabled?: boolean;
    label: string;
}

export interface DotDialogActions {
    accept?: DialogButton;
    cancel?: DialogButton;
}
