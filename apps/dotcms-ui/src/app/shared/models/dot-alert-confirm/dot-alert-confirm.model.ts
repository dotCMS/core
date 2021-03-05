import { EventEmitter } from '@angular/core';
import { FooterLabels } from './footer-labels.model';

export interface DotAlertConfirm {
    message: string;
    key?: string;
    icon?: string;
    header: string;
    footerLabel?: FooterLabels;
    accept?: Function;
    reject?: Function;
    acceptVisible?: boolean;
    rejectVisible?: boolean;
    acceptEvent?: EventEmitter<any>;
    rejectEvent?: EventEmitter<any>;
}
