import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';

@Injectable()
export class DotGenerateSecurePasswordService {
    private data: Subject<{ [key: string]: string }> = new Subject<{ [key: string]: string }>();

    get showDialog$(): Observable<{ [key: string]: string }> {
        return this.data.asObservable();
    }

    /**
     * Notify subscribers with new data
     *
     * @param {{ [key: string]: string }} data
     * @memberof DotGenerateSecurePasswordService
     */
    open(data: { [key: string]: string }): void {
        this.data.next(data);
    }
}
