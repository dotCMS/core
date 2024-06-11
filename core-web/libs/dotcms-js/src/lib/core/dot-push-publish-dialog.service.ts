import { Observable, Subject } from 'rxjs';

import { Injectable } from '@angular/core';

import { DotPushPublishDialogData } from '@dotcms/dotcms-models';

@Injectable({
    providedIn: 'root'
})
export class DotPushPublishDialogService {
    private data: Subject<DotPushPublishDialogData> = new Subject<DotPushPublishDialogData>();

    get showDialog$(): Observable<DotPushPublishDialogData> {
        return this.data.asObservable();
    }

    open(data: DotPushPublishDialogData): void {
        this.data.next(data);
    }
}
