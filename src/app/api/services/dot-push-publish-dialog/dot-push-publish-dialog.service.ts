import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { DotPushPublishDialogData } from '@models/dot-push-publish-dialog-data/dot-push-publish-dialog-data.model';

@Injectable()
export class DotPushPublishDialogService {
    private data: Subject<DotPushPublishDialogData> = new Subject<DotPushPublishDialogData>();

    get showDialog$(): Observable<DotPushPublishDialogData> {
        return this.data.asObservable();
    }

    open(data: DotPushPublishDialogData): void {
        this.data.next(data);
    }
}
