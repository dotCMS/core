import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';

@Injectable()
export class DotDownloadBundleDialogService {
    private data: Subject<string> = new Subject<string>();

    get showDialog$(): Observable<string> {
        return this.data.asObservable();
    }

    /**
     * Notify subscribers with new data
     *
     * @param string data
     * @memberof DotDownloadBundleDialogService
     */
    open(data: string): void {
        this.data.next(data);
    }
}
