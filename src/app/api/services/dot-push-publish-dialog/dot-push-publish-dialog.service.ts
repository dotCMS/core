import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { DotPushPublishEvent } from '@models/push-publish-data/push-publish-data';

@Injectable()
export class DotPushPublishDialogService {
    private _showDialog: Subject<DotPushPublishEvent> = new Subject<DotPushPublishEvent>();

    get showDialog$(): Observable<DotPushPublishEvent> {
        return this._showDialog.asObservable();
    }

    open(data: DotPushPublishEvent): void {
        this._showDialog.next(data);
    }
}
