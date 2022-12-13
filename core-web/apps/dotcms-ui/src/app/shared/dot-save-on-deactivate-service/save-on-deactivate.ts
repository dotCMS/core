import { DotAlertConfirm } from '@dotcms/dotcms-models';
import { Observable } from 'rxjs';

export interface OnSaveDeactivate {
    shouldSaveBefore(): boolean;
    onDeactivateSave(): Observable<boolean>;
    getSaveWarningMessages(): DotAlertConfirm;
}
