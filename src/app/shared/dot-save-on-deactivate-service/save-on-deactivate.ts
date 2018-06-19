import { DotDialog } from '../models/dot-confirmation/dot-confirmation.model';
import { Observable } from 'rxjs/Observable';

export interface OnSaveDeactivate {
    shouldSaveBefore(): boolean;
    onDeactivateSave(): Observable<boolean>;
    getSaveWarningMessages(): DotDialog;
}
