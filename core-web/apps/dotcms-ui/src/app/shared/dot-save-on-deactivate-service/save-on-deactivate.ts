import { Observable } from 'rxjs';

import { DotAlertConfirm } from '@dotcms/dotcms-models';

export interface OnSaveDeactivate {
    shouldSaveBefore(): boolean;
    onDeactivateSave(): Observable<boolean>;
    getSaveWarningMessages(): DotAlertConfirm;
}
