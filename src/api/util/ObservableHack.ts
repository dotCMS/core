import {Observable} from "rxjs/Rx";


export class ObservableHack {

  /**
   * @See https://github.com/angular/angular/issues/5992
   * @param value
   * @returns {Observable<string>}
   */
  static of(value:string) {
    return Observable.timer(10).map(x => value)
  }
}