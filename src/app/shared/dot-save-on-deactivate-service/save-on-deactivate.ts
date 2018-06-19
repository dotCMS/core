import { DotAlertConfirm } from '../models/dot-alert-confirm/dot-alert-confirm.model';
import { Observable } from 'rxjs/Observable';

export interface OnSaveDeactivate {
    shouldSaveBefore(): boolean;
    onDeactivateSave(): Observable<boolean>;
    getSaveWarningMessages(): DotAlertConfirm;
}
