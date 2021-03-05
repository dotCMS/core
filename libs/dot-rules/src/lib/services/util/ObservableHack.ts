import { Observable, asapScheduler } from 'rxjs';

// @dynamic
export class ObservableHack {
    /**
     * @See https://github.com/angular/angular/issues/5992
     * @param value
     */
    static of(value: string): Observable<string> {
        return Observable.create((subscriber) => {
            subscriber.next(value);
        }).subscribeOn(asapScheduler);
    }
}
