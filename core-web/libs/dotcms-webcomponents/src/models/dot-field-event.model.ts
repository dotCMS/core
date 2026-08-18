import { DotFieldStatus } from './dot-field-status.model';
import { DotBinaryMessageError } from './dot-binary-message-error.model';

export interface DotFieldEvent {
    name: string;
}

export interface DotFieldStatusEvent extends DotFieldEvent {
    status: DotFieldStatus;
}

export interface DotInputCalendarStatusEvent extends DotFieldStatusEvent {
    isValidRange: boolean;
}

export interface DotFieldValueEvent extends DotFieldEvent {
    fieldType?: string;
    value: string | File;
}

export interface DotBinaryFileEvent {
    /** Null on every failure path — an invalid paste, an over-size file, a cleared field. */
    file: string | File | null;
    /** Null when the change was accepted. `dot-binary-file` clears its own copy the same way. */
    errorType: DotBinaryMessageError | null;
}
