import { EventEmitter } from '@angular/core';
import { FooterLabels } from './footer-labels.model';

export interface DotAlertConfirm {
    message: string;
    key?: string;
    icon?: string;
    header: string;
    footerLabel?: FooterLabels;
    accept?: (value?: unknown) => void;
    reject?: (value?: unknown) => void;
    acceptVisible?: boolean;
    rejectVisible?: boolean;
    acceptEvent?: EventEmitter<unknown>;
    rejectEvent?: EventEmitter<unknown>;
}
