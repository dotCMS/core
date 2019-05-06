import { DotFieldStatus } from './dot-field-status.model';

export interface DotFieldEvent {
    name: string;
}

export interface DotFieldStatusEvent extends DotFieldEvent {
    status: DotFieldStatus;
}

export interface DotFieldValueEvent extends DotFieldEvent {
    fieldType?: string;
    value: string;
}
