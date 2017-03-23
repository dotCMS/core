import {Observable, Scheduler} from 'rxjs/Rx';


export class ObservableHack {

  /**
   * @See https://github.com/angular/angular/issues/5992
   * @param value
   * @returns {Observable<string>}
   */
  static of(value:string):Observable<string> {
    return Observable.create(subscriber => {
      subscriber.next(value);
    }).subscribeOn(Scheduler.asap);
  }
}